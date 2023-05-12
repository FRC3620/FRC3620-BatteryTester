package org.usfirst.frc3620.batterytester;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static io.undertow.Handlers.*;

@CommandLine.Command(name = "FRC3620-BatteryTester", versionProvider = Application.PropertiesVersionProvider.class, mixinStandardHelpOptions = true)
public class Application implements Runnable {
    @CommandLine.Option(names = {"-V", "--version"}, versionHelp = true, description = "Print version info from examples.jar/version.txt and exit")
    boolean versionRequested;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Undertow server;

    private BatteryTester batteryTester;

    private Webcam webcam = null;

    public static void main(String... args) {
        int exitCode = new CommandLine(new Application()).execute(args);
        System.exit(exitCode);
    }

    public void run() {
        org.tinylog.Logger.info("Greetings from tinylog!");
        buildAndStartServer(8080, "0.0.0.0");
    }

    public void buildAndStartServer(int port, String host) {
        WSMessage.prime();
        BatteryTestStatusWebSocket batteryStatusSender = new BatteryTestStatusWebSocket();

        ResourceManager contentHandler;
        if (isRaspbian()) {
            contentHandler = new ClassPathResourceManager(getClass().getClassLoader(), getClass().getPackage());
        } else {
            // the following line is so we have hot reload when testing
            contentHandler = new PathResourceManager(Paths.get("src/main/resources/org/usfirst/frc3620/batterytester"));
        }

        HttpHandler handler = path()
                .addPrefixPath("/battery", websocket(batteryStatusSender))
                .addPrefixPath("/test", this::testHandler)
                .addPrefixPath("/", resource(contentHandler).addWelcomeFiles("index.html"));

        server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(handler)
                .setWorkerThreads(1)
                .build();
        server.start();

        IBattery battery;
        if (isRaspbian()) {
            battery = new RealBattery();
        } else {
            BatteryInfo bi = new BatteryInfo();
            bi.nominalCapacity = 18.2;
            battery = new FakeBattery(bi);
        }

        batteryTester = new BatteryTester(battery);
        Thread batteryThread = new Thread(batteryTester);
        batteryThread.start();
        batteryTester.setLoadAmperage(200.0);

        webcam = Webcam.getWebcams().get(0);
        webcam.open();

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.info("oops", e);
            }
        }
    }

    class BatteryTestStatusWebSocket implements WebSocketConnectionCallback {
        BatteryTestStatusWebSocket() {
            logger.info ("creating {}", this);
        }

        @Override
        public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
            logger.info("Got connection {}", channel.getSourceAddress());
            new BatteryTestStatusWriter(channel);
        }
    }

    class BatteryTestStatusWriter {
        Logger logger = LoggerFactory.getLogger(getClass());
        WebSocketChannel channel;
        BlockingQueue<WSMessage> q;
        BatteryTestStatusWriter (WebSocketChannel channel) {
            this.channel = channel;
            q = new LinkedBlockingQueue<>();

            Thread t = new Thread(this::thread);
            t.setDaemon(true);
            t.start();
            batteryTester.addStatusConsumer(q, true);
        }

        void thread() {
            logger.info ("Starting a writer for {}", channel.getSourceAddress());
            while (true) {
                try {
                    WSMessage m = q.take();
                    if (channel.isOpen()) {
                        String s = m.json();
                        logger.debug("Writing to {}: {}", channel, s);
                        WebSockets.sendText(s, channel, null);
                    } else {
                        logger.info("Dropping connection {}", channel.getSourceAddress());
                        batteryTester.removeStatusConsumer(q);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    void testHandler(HttpServerExchange httpServerExchange) {
        logger.info("testHandler hit: {} {} {}", httpServerExchange, httpServerExchange.getQueryParameters(), httpServerExchange.getPathParameters());
        String verb = httpServerExchange.getRelativePath().substring(1);
        logger.info("got {}, verb {}", httpServerExchange, verb);
        boolean ok = true;

        BatteryTester.Status status = batteryTester.getStatus();
        boolean rv = false;
        if (verb.equalsIgnoreCase("start")) {
            rv = batteryTester.startTest();
        } else if (verb.equalsIgnoreCase("pause")) {
            rv = batteryTester.pauseTest();
        } else if (verb.equalsIgnoreCase("stop")) {
            rv = batteryTester.stopTest();
        } else if (verb.equalsIgnoreCase("id")) {
            readCameraCode();
            rv = true;
        } else {
            httpServerExchange.setStatusCode(StatusCodes.NOT_FOUND);
            httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            httpServerExchange.getResponseSender().send("unknown verb " + verb);
            return;
        }
        if (rv) {
            httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            httpServerExchange.getResponseSender().send("ok, status = " + batteryTester.getStatus().toString());
            return;
        } else {
            httpServerExchange.setStatusCode(StatusCodes.BAD_REQUEST);
            httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            httpServerExchange.getResponseSender().send("cannot " + verb + ", current state is " + batteryTester.getStatus().toString());
            return;
        }
    }

    public void readCameraCode() {
        BufferedImage image;

        if (true) {
            image = webcam.getImage();
            try {
                ImageIO.write(image, "PNG", new File("hello-world.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                image = ImageIO.read(new File("frame.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        BinaryBitmap binaryBitmap
                = new BinaryBitmap(new HybridBinarizer(
                        new BufferedImageLuminanceSource(image)));

        try {
            Result result
                    = new MultiFormatReader().decode(binaryBitmap);
            logger.info("result: {}", result);
        } catch (NotFoundException e) {
            logger.warn("unable to decode", e);
            // throw new RuntimeException(e);
        }
    }

    boolean isRaspbian() {
        String OS = System.getProperty("os.name").toLowerCase();
        return OS.contains("nux"); // really bad hack
    }

  /**
   * {@link IVersionProvider} implementation that returns version information from a {@code /version.txt} file in the classpath.
   */
  static class PropertiesVersionProvider implements CommandLine.IVersionProvider {
    public String[] getVersion() throws Exception {
      URL url = getClass().getResource("/git.properties");
      if (url == null) {
        return new String[] {"No git.properties file found in the classpath. Is examples.jar in the classpath?"};
      }
      Properties properties = new Properties();
      properties.load(url.openStream());
      properties.list(System.out);
      return new String[] {
        properties.getProperty("Application-name") + " version \"" + properties.getProperty("Version") + "\"",
        "Built: " + properties.getProperty("Buildtime"),
      };
    }
  }

}
