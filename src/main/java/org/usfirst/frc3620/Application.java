package org.usfirst.frc3620;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static io.undertow.Handlers.*;

public class Application {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Undertow server;

    private IBattery battery;
    private BatteryTester batteryTester;
    private BatteryStatusSender batteryStatusSender;

    public static void main(final String[] args) {
        Application application = new Application();
        application.buildAndStartServer(8080, "localhost");
    }

    public void buildAndStartServer(int port, String host) {
        batteryStatusSender = new BatteryStatusSender();

        HttpHandler handler = null;
        if (false) {
            handler = new RoutingHandler()
                .get("/websocket", websocket(new WSTest()))
                .get("/battery", websocket(batteryStatusSender))
                .get("/", resource(new ClassPathResourceManager(getClass().getClassLoader(), getClass().getPackage()))
                    .addWelcomeFiles("index.html"));
        } else {
            handler = path()
                .addPrefixPath("/websocket", websocket(new WSTest()))
                .addPrefixPath("/battery", websocket(batteryStatusSender))
                .addPrefixPath("/", resource(new ClassPathResourceManager(getClass().getClassLoader(), getClass().getPackage()))
                    .addWelcomeFiles("index.html"));
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
        batteryTester.addStatusConsumer(batteryStatusSender);
        Thread batteryThread = new Thread(batteryTester);
        batteryThread.start();

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

    class BatteryStatusSender implements Consumer<BatteryStatus>, WebSocketConnectionCallback {
        ObjectMapper objectMapper = new ObjectMapper();

        BatteryStatusSender() {
            logger.info ("creating {}", this);
        }
        List<WebSocketChannel> wsChannels = new ArrayList<>();

        @Override
        public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
            logger.info("Got connection {}", channel);
            wsChannels.add(channel);
        }

        @Override
        public void accept(BatteryStatus batteryStatus) {
            logger.debug ("Accepted {}", batteryStatus);
            String payload = null;
            try {
                payload = objectMapper.writeValueAsString(batteryStatus);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            for (Iterator<WebSocketChannel> i = wsChannels.iterator(); i.hasNext(); ) {
                var channel = i.next();
                if (channel.isOpen()) {
                    WebSockets.sendText(payload, channel, null);
                } else {
                    logger.info ("Dropping connection {}", channel);
                    i.remove();
                }
            }
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
}
