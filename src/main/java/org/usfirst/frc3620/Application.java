package org.usfirst.frc3620;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import static io.undertow.Handlers.*;

public class Application {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Undertow server;

    private IBattery battery;
    private BatteryTester batteryTester;
    private BatteryTestStatusWebSocket batteryStatusSender;

    public static void main(final String[] args) {
        Application application = new Application();
        application.buildAndStartServer(8080, "localhost");
    }

    public void buildAndStartServer(int port, String host) {
        batteryStatusSender = new BatteryTestStatusWebSocket();

        ResourceManager contentHandler = new ClassPathResourceManager(getClass().getClassLoader(), getClass().getPackage());
        // the following line is so we have hot reload when testing
        contentHandler = new PathResourceManager(Paths.get("src/main/resources/org/usfirst/frc3620"));

        HttpHandler handler = null;
        if (false) {
            handler = new RoutingHandler()
                .get("/websocket", websocket(new WSTest()))
                .get("/battery", websocket(batteryStatusSender))
                .get("/", resource(contentHandler).addWelcomeFiles("index.html"));
        } else {
            handler = path()
                .addPrefixPath("/websocket", websocket(new WSTest()))
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
        battery = new FakeBattery(bi);

        batteryTester = new BatteryTester(battery);
        // batteryTester.addStatusConsumer(batteryStatusSender);
        Thread batteryThread = new Thread(batteryTester);
        batteryThread.start();
        batteryTester.startTest(10);

        while (true) {
            try {
                Thread.sleep(1000);
                for (Iterator<WebSocketChannel> i = wsTestChannels.iterator(); i.hasNext(); ) {
                    var wsChannel = i.next();
                    if (wsChannel.isOpen()) {
                        WebSockets.sendText(new Date() + " " + wsChannel.toString(), wsChannel, null);
                    } else {
                        logger.info ("Removing {}", wsChannel);
                        i.remove();
                    }
                }
            } catch (InterruptedException e) {
                logger.info("oops", e);
            }
        }
    }

    List<WebSocketChannel> wsTestChannels = new ArrayList<>();

    class WSTest implements WebSocketConnectionCallback {
        @Override
        public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
            logger.info("Got connection {}", channel);
            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                @Override
                protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                String data = message.getData();
                logger.info("Received data: {}", data);
                WebSockets.sendText(data, channel, null);
                }
            });
            channel.resumeReceives();
            wsTestChannels.add(channel);
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
        WebSocketChannel channel;
        BlockingQueue<BatteryTestStatus> q;
        BatteryTestStatusWriter (WebSocketChannel channel) {
            this.channel = channel;
            q = new LinkedBlockingQueue<>();

            Thread t = new Thread(this::thread);
            t.setDaemon(true);
            t.start();
            batteryTester.addStatusConsumer(q, true);
        }

        void thread() {
            ObjectMapper objectMapper = new ObjectMapper();
            while (true) {
                try {
                    BatteryTestStatus s = q.take();
                    WebsocketMessage w = new WebsocketMessage("test_status", s);
                    String j = objectMapper.writeValueAsString(w);
                    if (channel.isOpen()) {
                        WebSockets.sendText(j, channel, null);
                    } else {
                        logger.info("Dropping connection {}", channel);
                        batteryTester.removeStatusConsumer(q);
                    }
                } catch (InterruptedException | JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static class WebsocketMessage {
        String messageType;
        Object payload;
        public WebsocketMessage (String messageType, Object payload) {
            this.messageType = messageType;
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
