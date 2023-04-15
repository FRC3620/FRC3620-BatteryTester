package org.usfirst.frc3620.batterytester;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static io.undertow.Handlers.*;

public class Application {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Undertow server;

    private BatteryTester batteryTester;

    public static void main(final String[] args) {
        Application application = new Application();
        application.buildAndStartServer(8080, "localhost");
    }

    public void buildAndStartServer(int port, String host) {
        BatteryTestStatusWebSocket batteryStatusSender = new BatteryTestStatusWebSocket();

        ResourceManager contentHandler = new ClassPathResourceManager(getClass().getClassLoader(), getClass().getPackage());
        // the following line is so we have hot reload when testing
        contentHandler = new PathResourceManager(Paths.get("src/main/resources/org/usfirst/frc3620/batterytester"));

        HttpHandler handler = null;
        if (false) {
            handler = new RoutingHandler()
                .get("/battery", websocket(batteryStatusSender))
                .get("/", resource(contentHandler).addWelcomeFiles("index.html"));
        } else {
            handler = path()
                .addPrefixPath("/battery", websocket(batteryStatusSender))
                .addPrefixPath("/", resource(contentHandler).addWelcomeFiles("index.html"));
        }

        server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(handler)
                .setWorkerThreads(1)
                .build();
        server.start();

        BatteryInfo bi = new BatteryInfo();
        bi.nominalCapacity = 18.2;
        IBattery battery = new FakeBattery(bi);

        batteryTester = new BatteryTester(battery);
        // batteryTester.addStatusConsumer(batteryStatusSender);
        Thread batteryThread = new Thread(batteryTester);
        batteryThread.start();
        batteryTester.startTest(200.0);

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
            logger.info("Got connection {}", channel);
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
            logger.info ("Starting a writer for {}", channel);
            while (true) {
                try {
                    WSMessage m = q.take();
                    if (channel.isOpen()) {
                        String s = m.json();
                        logger.debug("Writing to {}: {}", channel, s);
                        WebSockets.sendText(s, channel, null);
                    } else {
                        logger.info("Dropping connection {}", channel);
                        batteryTester.removeStatusConsumer(q);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static class WebsocketMessage {
        String messageType;
        Object payload;
        public WebsocketMessage (Object payload) {
            this.messageType = payload.getClass().getSimpleName();
            this.payload = payload;
        }

        public String getMessageType() {
            return messageType;
        }

        public Object getPayload() {
            return payload;
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
}
