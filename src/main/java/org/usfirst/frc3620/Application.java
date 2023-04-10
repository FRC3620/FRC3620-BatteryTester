package org.usfirst.frc3620;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.PathHandler;
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
import java.util.List;

import static io.undertow.Handlers.*;

public class Application {
  private final Logger LOGGER = LoggerFactory.getLogger(getClass());
  private Undertow server;

  public static void main(final String[] args) {
    Application application = new Application();
    application.buildAndStartServer(8080, "localhost");
  }

  HttpHandler ROUTES = new RoutingHandler()
          .get("/websocket", websocket(new WSTest()))
          .get("/", resource(new ClassPathResourceManager(getClass().getClassLoader(), getClass().getPackage()))
                  .addWelcomeFiles("index.html"));

  PathHandler PATHS = path()
            .addPrefixPath("/websocket", websocket(new WSTest()))
            .addPrefixPath("/", resource(new ClassPathResourceManager(getClass().getClassLoader(), getClass().getPackage()))
                    .addWelcomeFiles("index.html"));

  public void buildAndStartServer(int port, String host) {
    server = Undertow.builder()
      .addHttpListener(port, host)
      .setHandler(PATHS)
      .setWorkerThreads(1)
      .build();
    server.start();
    while (true) {
      try {
        Thread.sleep(1000);
        for (var wsChannel : wsChannels) {
          WebSockets.sendText(new Date() + " " + wsChannel.toString(), wsChannel, null);
        }
      } catch (InterruptedException e) {
        LOGGER.info("oops", e);
      }
    }
  }

  class WSTest implements WebSocketConnectionCallback {
    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
      LOGGER.info("Got connection {}", channel);
      channel.getReceiveSetter().set(new AbstractReceiveListener() {
        @Override
        protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
          String data = message.getData();
          LOGGER.info("Received data: {}", data);
          WebSockets.sendText(data, channel, null);
        }
      });
      channel.resumeReceives();
      wsChannels.add(channel);
    }

  }

  List<WebSocketChannel> wsChannels = new ArrayList<>();

  public void stopServer() {
    if (server != null) {
      server.stop();
    }
  }
}
