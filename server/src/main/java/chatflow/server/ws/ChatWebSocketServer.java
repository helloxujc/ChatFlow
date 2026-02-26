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

/**
 * WebSocket server for handling chat connections and messages.
 *
 * <p>This server accepts connections on {@code /chat/{roomId}}, validates room IDs and incoming
 * JSON messages, and echoes back a {@link ChatResponse} with status and server timestamp.
 */
public class ChatWebSocketServer extends WebSocketServer {

  private final Map<WebSocket, Integer> roomByConn = new ConcurrentHashMap<>();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Creates a chat WebSocket server bound to the given port.
   *
   * @param port the TCP port to bind
   */
  public ChatWebSocketServer(int port) {
    super(new InetSocketAddress(port));
  }

  /**
   * Handles a new WebSocket connection and extracts the room ID from the request path.
   *
   * @param webSocket the connected client socket
   * @param clientHandshake the handshake containing the resource descriptor
   */
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

  /**
   * Parses the room ID from a resource path of the form {@code /chat/{roomId}}.
   *
   * @param path the resource descriptor path
   * @return parsed room ID, or {@code -1} if invalid
   */
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

  /**
   * Sends an ERROR response to the client and closes the connection with a policy violation code.
   *
   * @param webSocket the client socket
   * @param cause the human-readable error reason
   */
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

  /**
   * Handles a WebSocket close event.
   *
   * @param webSocket the client socket
   * @param i the close code
   * @param s the close reason
   * @param b whether the close was initiated by the remote peer
   */
  @Override
  public void onClose(WebSocket webSocket, int i, String s, boolean b) {

  }

  /**
   * Handles an incoming message by deserializing JSON, validating fields, and echoing a response.
   *
   * @param webSocket the client socket
   * @param s the incoming text message payload
   */
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

  /**
   * Handles a server-side error associated with a connection.
   *
   * @param webSocket the client socket (may be null)
   * @param e the exception raised by the WebSocket implementation
   */
  @Override
  public void onError(WebSocket webSocket, Exception e) {

  }

  /**
   * Called when the server has started successfully.
   */
  @Override
  public void onStart() {

  }

  /**
   * Sends a payload only if the connection is open, swallowing connection-related exceptions.
   *
   * @param conn the client socket
   * @param payload the serialized response payload
   */
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
