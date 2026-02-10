package chatflow.server.ws;


import chatflow.server.model.ChatMessage;
import chatflow.server.model.ChatResponse;
import chatflow.server.validation.MessageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class ChatWebSocketServer extends WebSocketServer {

  private final Map<WebSocket, Integer> roomByConn = new ConcurrentHashMap<>();
  private static final ObjectMapper MAPPER = new ObjectMapper();


  public ChatWebSocketServer(int port) {
    super(new InetSocketAddress(port));
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

  @Override
  public void onClose(WebSocket webSocket, int i, String s, boolean b) {

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

      chatResponse.setStatus("OK");
      chatResponse.setData(msg);
      safeSend(webSocket, MAPPER.writeValueAsString(chatResponse));
    } catch (Exception e) {
      System.out.println("Deserialization failed: " + e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();

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

  @Override
  public void onError(WebSocket webSocket, Exception e) {

  }

  @Override
  public void onStart() {

  }

  private void safeSend(WebSocket conn, String payload) {
    if (conn == null || !conn.isOpen()) {
      return;
    }
    try {
      conn.send(payload);
    } catch (org.java_websocket.exceptions.WebsocketNotConnectedException ignored) {
      // ignored
    } catch (Exception ignored) {
      // ignored
    }
  }
}
