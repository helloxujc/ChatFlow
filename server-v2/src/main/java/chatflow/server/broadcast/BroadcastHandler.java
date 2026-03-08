package chatflow.server.broadcast;

import chatflow.server.room.RoomManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;

/**
 * HTTP handler for the internal broadcast endpoint that delivers messages to all WebSocket sessions in a room.
 */
public class BroadcastHandler implements HttpHandler {

  private RoomManager roomManager;
  private MessageIdCache messageIdCache;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public BroadcastHandler(RoomManager roomManager, MessageIdCache messageIdCache) {
    this.roomManager = roomManager;
    this.messageIdCache = messageIdCache;
  }

  /**
   * Handles an incoming HTTP POST broadcast request by deduplicating it and delivering the message to all sessions in the target room.
   *
   * @param httpExchange the HTTP exchange containing the request and response streams
   * @throws IOException if reading the request or writing the response fails
   */
  public void handle(HttpExchange httpExchange) throws IOException {
    try {
      if (!"POST".equalsIgnoreCase(httpExchange.getRequestMethod())) {
        httpExchange.sendResponseHeaders(405, -1);
        return;
      }

      BroadcastRequest broadcastRequest;
      try {
        broadcastRequest = parseRequest(httpExchange);
      } catch (IOException e) {
        httpExchange.sendResponseHeaders(400, -1);
        return;
      }
      if (messageIdCache.seen(broadcastRequest.getMessageId())) {
        httpExchange.sendResponseHeaders(200, -1);
        return;
      }

      String payload = objectMapper.writeValueAsString(broadcastRequest);
      roomManager.broadcast(broadcastRequest.getRoomId(), payload);
      httpExchange.sendResponseHeaders(200, -1);
    } finally {
      httpExchange.close();
    }
  }

  private BroadcastRequest parseRequest(HttpExchange httpExchange) throws IOException {
    InputStream requestBody = httpExchange.getRequestBody();

    BroadcastRequest broadcastRequest = objectMapper.readValue(requestBody, BroadcastRequest.class);
    return broadcastRequest;
  }

}
