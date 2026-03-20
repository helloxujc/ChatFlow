package chatflow.server;

import chatflow.server.broadcast.BroadcastHandler;
import chatflow.server.broadcast.MessageIdCache;
import chatflow.server.metrics.MetricsHandler;
import chatflow.server.metrics.MetricsRepository;
import chatflow.server.queue.MessagePublisher;
import chatflow.server.queue.rabbit.ChannelPool;
import chatflow.server.queue.rabbit.RabbitMqPublisher;
import chatflow.server.room.RoomManager;
import chatflow.server.ws.ChatWebSocketServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Entry point for starting the ChatFlow server.
 */
public class ServerMain {
  public static void main(String[] args) throws IOException {
    int port = 8080;
    ObjectMapper objectMapper = new ObjectMapper();

    RoomManager roomManager = new RoomManager();
    MessageIdCache messageIdCache = new MessageIdCache();
    BroadcastHandler broadcastHandler = new BroadcastHandler(roomManager, messageIdCache);

    // Dedicated health server — never blocked by broadcast load
    MetricsRepository metricsRepository = new MetricsRepository();
    MetricsHandler metricsHandler = new MetricsHandler(metricsRepository);

    HttpServer healthServer = HttpServer.create(new InetSocketAddress(port), 16);
    healthServer.setExecutor(Executors.newFixedThreadPool(4));
    healthServer.createContext("/health",
        exchange -> {
          String response = "OK";
          exchange.sendResponseHeaders(200, response.getBytes().length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
          }
        });
    healthServer.createContext("/metrics", metricsHandler);
    healthServer.start();
    System.out.println("Health server started on port " + port);

    // Broadcast server on port 8082 — isolated thread pool
    HttpServer broadcastServer = HttpServer.create(new InetSocketAddress(8082), 128);
    broadcastServer.setExecutor(Executors.newFixedThreadPool(
        Integer.parseInt(System.getenv().getOrDefault("BROADCAST_THREADS", "32"))));
    broadcastServer.createContext("/internal/broadcast", broadcastHandler);
    broadcastServer.createContext("/rooms", exchange -> {
      try {
        byte[] body = objectMapper.writeValueAsBytes(roomManager.getActiveRoomIds());
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(body);
        }
      } finally {
        exchange.close();
      }
    });
    broadcastServer.start();
    System.out.println("Broadcast server started on port 8082");

    String serverId = System.getenv().getOrDefault("CHATFLOW_SERVER_ID", "server-1");

    try {
      ChannelPool pool =
          new ChannelPool(
              System.getenv().getOrDefault("RABBIT_HOST", "localhost"),
              Integer.parseInt(System.getenv().getOrDefault("RABBIT_PORT", "5672")),
              System.getenv().getOrDefault("RABBIT_USER", "guest"),
              System.getenv().getOrDefault("RABBIT_PASS", "guest"),
              Integer.parseInt(System.getenv().getOrDefault("RABBIT_CHANNEL_POOL", "16")));

      MessagePublisher publisher =
          new RabbitMqPublisher(pool, System.getenv().getOrDefault("RABBIT_EXCHANGE", "chat.exchange"));

      ChatWebSocketServer wsServer = new ChatWebSocketServer(8081, publisher, serverId, roomManager);
      wsServer.setConnectionLostTimeout(0); // disable idle ping — prevents idle-timeout-task OOM
      wsServer.start();
      System.out.println("WebSocket bind address: " + wsServer.getAddress());
      System.out.println("WebSocket started on port 8081");
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("All servers started");
  }
}