import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * WebSocket-based implementation of {@link SendChannel}.
 *
 * <p>This class manages a single WebSocket connection, supports blocking
 * connect, reconnection, and delegates acknowledgment messages to an
 * optional {@link AckListener}.
 */
public final class WsSendChannel implements SendChannel {

  private final URI uri;
  private final Metrics metrics;
  private final AtomicBoolean open = new AtomicBoolean(false);
  private volatile WebSocketClient client;
  private final AckListener listener;

  /**
   * Creates a WebSocket send channel without an acknowledgment listener.
   *
   * @param uri target WebSocket URI
   * @param metrics metrics collector
   */
  public WsSendChannel(URI uri, Metrics metrics) {
    this(uri, metrics, null);
  }

  /**
   * Creates a WebSocket send channel with an acknowledgment listener.
   *
   * @param uri target WebSocket URI
   * @param metrics metrics collector
   * @param listener acknowledgment listener
   */
  public WsSendChannel(URI uri, Metrics metrics, AckListener listener) {
    this.uri = Objects.requireNonNull(uri, "uri");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    this.listener = listener;
  }

  /**
   * Sends a text message through the active WebSocket connection.
   *
   * @param text message payload
   * @throws Exception if the connection is not open
   */
  @Override
  public void send(String text) throws Exception {
    WebSocketClient c = client;
    if (c == null || !open.get()) {
      throw new IllegalStateException("WebSocket not open");
    }
    c.send(text);
  }

  /**
   * Returns whether the WebSocket connection is currently open.
   *
   * @return true if open, false otherwise
   */
  @Override
  public boolean isOpen() {
    WebSocketClient c = client;
    return c != null && open.get() && c.isOpen();
  }

  /**
   * Attempts to reconnect the WebSocket connection.
   *
   * @throws Exception if reconnection fails
   */
  @Override
  public void reconnect() throws Exception {
    metrics.reconnections().increment();
    closeSilently();
    connectBlocking(5, TimeUnit.SECONDS);
  }

  /**
   * Connects to the server and waits up to the specified timeout for success.
   *
   * @param timeout timeout value
   * @param timeUnit timeout unit
   * @throws Exception if connection fails or times out
   */
  public void connectBlocking(long timeout, TimeUnit timeUnit) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);

    client =
        new WebSocketClient(uri) {

          @Override
          public void onOpen(ServerHandshake serverHandshake) {
            open.set(true);
            metrics.connectionsCreated().increment();
            latch.countDown();
          }

          @Override
          public void onMessage(String s) {
            if (listener != null) {
              listener.onMessage(s);
            }
          }

          @Override
          public void onClose(int i, String s, boolean b) {
            open.set(false);
          }

          @Override
          public void onError(Exception e) {
            open.set(false);
          }
        };

    client.connect();
    boolean success = latch.await(timeout, timeUnit);
    if (!success || !isOpen()) {
      closeSilently();
      throw new Exception("WebSocket connect timeout: " + uri);
    }
  }

  /**
   * Closes the WebSocket connection without throwing exceptions.
   */
  public void closeSilently() {
    WebSocketClient c = client;
    if (c != null) {
      try {
        c.close();
      } catch (Exception ignored) {
        // ignored
      } finally {
        open.set(false);
      }
    }
  }
}
