package chatflow.server;

import chatflow.server.queue.MessagePublisher;
import chatflow.server.queue.rabbit.ChannelPool;
import chatflow.server.queue.rabbit.RabbitMqPublisher;
import chatflow.server.room.RoomManager;
import chatflow.server.ws.ChatWebSocketServer;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Entry point for starting the ChatFlow server.
 */
public class ServerMain {
  public static void main(String[] args) throws IOException {
    int port = 8080;

    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/health",
        exchange -> {
          String response = "OK";
          exchange.sendResponseHeaders(200, response.getBytes().length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
          }
        });

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

      RoomManager roomManager = new RoomManager();
      ChatWebSocketServer wsServer = new ChatWebSocketServer(8081, publisher, serverId, roomManager);
      wsServer.start();
      System.out.println("WebSocket bind address: " + wsServer.getAddress());
      System.out.println("WebSocket started on port 8081");
    } catch (Exception e) {
      e.printStackTrace();
    }

    int actualPort = server.getAddress().getPort();
    server.start();
    System.out.println("Server started on port " + actualPort);
  }
}