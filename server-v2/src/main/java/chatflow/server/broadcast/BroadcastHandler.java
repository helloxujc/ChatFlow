package chatflow.server.broadcast;

import chatflow.server.room.RoomManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BroadcastHandler implements HttpHandler {

  private RoomManager roomManager;
  private MessageIdCache messageIdCache;
  private ExecutorService executorService;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public BroadcastHandler(RoomManager roomManager, MessageIdCache messageIdCache,
                          ExecutorService executorService) {
    this.roomManager = roomManager;
    this.messageIdCache = messageIdCache;
    this.executorService = executorService;
  }

  public void handle(HttpExchange httpExchange) throws IOException {
    if (!"POST".equalsIgnoreCase(httpExchange.getRequestMethod())) {
      httpExchange.sendResponseHeaders(405, 0);
      return;
    }

    BroadcastRequest broadcastRequest;
    try { broadcastRequest = parseRequest(httpExchange);
    } catch (IOException e) {
      httpExchange.sendResponseHeaders(400, 0);
      return;
    }
    if (messageIdCache.seen(broadcastRequest.getMessageId())) {
      httpExchange.sendResponseHeaders(200, 0);
      return;
    }

    String payload = objectMapper.writeValueAsString(broadcastRequest);

    Future<?> future = executorService.submit(() -> {
      roomManager.broadcast(broadcastRequest.getRoomId(), payload);
    });

    try {
      future.get(3, TimeUnit.SECONDS);
      httpExchange.sendResponseHeaders(200, 0);
    } catch (Exception e) {
      future.cancel(true);
      httpExchange.sendResponseHeaders(500, 0);
    }

  }

  private BroadcastRequest parseRequest(HttpExchange httpExchange) throws IOException {
    InputStream requestBody = httpExchange.getRequestBody();

    BroadcastRequest broadcastRequest = objectMapper.readValue(requestBody, BroadcastRequest.class);
    return broadcastRequest;
  }

}
