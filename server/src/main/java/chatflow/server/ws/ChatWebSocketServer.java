package chatflow.server.ws;

import chatflow.server.model.ChatMessage;
import chatflow.server.model.ChatResponse;
import chatflow.server.queue.MessagePublisher;
import chatflow.server.queue.QueueMessage;
import chatflow.server.validation.MessageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * WebSocket server for handling chat connections and publishing validated messages to a queue.
 */
public class ChatWebSocketServer extends WebSocketServer {

  private final Map<WebSocket, Integer> roomByConn = new ConcurrentHashMap<>();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final MessagePublisher publisher;
  private final String serverId;

  /**
   * Creates a chat WebSocket server.
   *
   * @param port the TCP port to bind
   * @param publisher queue publisher
   * @param serverId server identifier
   */
  public ChatWebSocketServer(int port, MessagePublisher publisher, String serverId) {
    super(new InetSocketAddress(port));
    this.publisher = Objects.requireNonNull(publisher, "publisher");
    this.serverId = Objects.requireNonNull(serverId, "serverId");
  }

  @Override
  public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
    String path = clientHandshake.getResourceDescriptor();
    int roomId = parseRoomID(path);

    if (roomId < 1 || roomId > 20) {
      sendAndClose(webSocket, "Invalid roomId");
      return;
    }

    roomByConn.put(webSocket, roomId);
    System.out.println("WebSocket opened: roomId=" + roomId + ", path" + path);
  }

  @Override
  public void onMessage(WebSocket webSocket, String s) {
    try {
      ChatMessage msg = MAPPER.readValue(s, ChatMessage.class);
      List<String> errors = MessageValidator.validate(msg);

      ChatResponse chatResponse = new ChatResponse();
      chatResponse.setServerTimestamp(Instant.now().toString());

      if (!errors.isEmpty()) {
        chatResponse.setStatus("ERROR");
        chatResponse.setErrors(errors);
        safeSend(webSocket, MAPPER.writeValueAsString(chatResponse));
        return;
      }

      Integer roomId = roomByConn.get(webSocket);
      if (roomId == null) {
        chatResponse.setStatus("ERROR");
        chatResponse.setErrors(List.of("Room not found for connection"));
        safeSend(webSocket, MAPPER.writeValueAsString(chatResponse));
        return;
      }

      String messageId = UUID.randomUUID().toString();
      String clientIp = webSocket.getRemoteSocketAddress() == null
          ? "unknown"
          : webSocket.getRemoteSocketAddress().getAddress().getHostAddress();

      QueueMessage qm =
          QueueMessage.from(messageId, String.valueOf(roomId), msg, serverId, clientIp);

      publisher.publish(qm);

      chatResponse.setStatus("OK");
      chatResponse.setData(Map.of("messageId", messageId));
      safeSend(webSocket, MAPPER.writeValueAsString(chatResponse));
    } catch (Exception e) {
      ChatResponse chatResponse = new ChatResponse();
      chatResponse.setStatus("ERROR");
      chatResponse.setServerTimestamp(Instant.now().toString());
      chatResponse.setErrors(List.of("Invalid JSON"));
      try {
        safeSend(webSocket, MAPPER.writeValueAsString(chatResponse));
      } catch (Exception ignored) {
      }
    }
  }

  private static int parseRoomID(String path) {
    if (path == null || path.isEmpty()) {
      return -1;
    }
    String[] parts = path.split("/");
    if (parts.length < 2) {
      return -1;
    }
    if (!"chat".equals(parts[1])) {
      return -1;
    }
    try {
      return Integer.parseInt(parts[2]);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private void sendAndClose(WebSocket webSocket, String cause) {
    try {
      ChatResponse chatResponse = new ChatResponse();
      chatResponse.setStatus("ERROR");
      chatResponse.setServerTimestamp(Instant.now().toString());
      chatResponse.setErrors(List.of(cause));
      webSocket.send(MAPPER.writeValueAsString(chatResponse));
    } catch (Exception ignored) {
    } finally {
      webSocket.close(1008, "Policy violation");
    }
  }

  private void safeSend(WebSocket conn, String payload) {
    if (conn == null || !conn.isOpen()) {
      return;
    }
    try {
      conn.send(payload);
    } catch (org.java_websocket.exceptions.WebsocketNotConnectedException ignored) {
    } catch (Exception ignored) {
    }
  }
}