package chatflow.server;

import chatflow.server.ws.ChatWebSocketServer;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

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

    try {
      ChatWebSocketServer wsServer = new ChatWebSocketServer(8081);
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