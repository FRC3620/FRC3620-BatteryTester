package martins.developer.world;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.core.handler.WebSocketConnectionCallback;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static io.undertow.Handlers.*;

public class WebSocketServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketServer.class);
    private Undertow server;
    String lastReceivedMessage;

    public static void main(final String[] args) {
        WebSocketServer webSocketServer = new WebSocketServer();
        webSocketServer.buildAndStartServer(8080, "localhost");
    }

    public void buildAndStartServer(int port, String host) {
        server = Undertow.builder()
                .addListener(port, host)
                .setHandler(getWebSocketHandler())
                .setWorkerThreads(1)
                .build();
        server.start();
        while (true) {
            try {
                Thread.sleep(1000);
                for (var wsChannel : wsChannels) {
                    WebSockets.sendText(new Date() + " "  + wsChannel.toString(), wsChannel, null);
                }
            } catch (InterruptedException e) {
                LOGGER.info("oops", e);
            }
        }
    }
    List<WebSocketChannel> wsChannels = new ArrayList<>();

    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }


    private PathHandler getWebSocketHandler() {
        return path().addPath("/websocket", websocket(new WebSocketConnectionCallback() {
            @Override
            public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                channel.getReceiveSetter().set(new AbstractReceiveListener() {
                    @Override
                    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                        String data = message.getData();
                        lastReceivedMessage = data;
                        LOGGER.info("Received data: " + data);
                        WebSockets.sendText(data, channel, null);
                    }
                });
                channel.resumeReceives();
                wsChannels.add(channel);
            }
        }))
                .addPath("/", resource(new ClassPathResourceManager(WebSocketServer.class.getClassLoader(), WebSocketServer.class.getPackage()))
                        .addWelcomeFiles("index.html"));
    }
}
