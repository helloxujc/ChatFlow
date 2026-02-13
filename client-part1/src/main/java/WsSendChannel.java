import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * WebSocket send channel with reconnect support.
 * Success can be counted on ack via {@code successCounter}, and custom ack handling
 * can be attached via {@code listener}.
 */
public final class WsSendChannel implements SendChannel {

  private final URI uri;
  private final Metrics metrics;
  private final AtomicBoolean open = new AtomicBoolean(false);
  private volatile WebSocketClient client;

  private final LongAdder successCounter;
  private final AckListener listener;

  /**
   * Creates a channel without ack counting.
   *
   * @param uri websocket uri
   * @param metrics metrics sink
   */
  public WsSendChannel(URI uri, Metrics metrics) {
    this(uri, metrics, null, null);
  }

  /**
   * Creates a channel that counts acks as success.
   *
   * @param uri websocket uri
   * @param metrics metrics sink
   * @param successCounter increments on each received message (ack)
   */
  public WsSendChannel(URI uri, Metrics metrics, LongAdder successCounter) {
    this(uri, metrics, successCounter, null);
  }

  /**
   * Creates a channel with an optional success counter and an optional ack listener.
   *
   * @param uri websocket uri
   * @param metrics metrics sink
   * @param successCounter increments on each received message (ack), may be null
   * @param listener custom ack listener, may be null
   */
  public WsSendChannel(URI uri, Metrics metrics, LongAdder successCounter, AckListener listener) {
    this.uri = Objects.requireNonNull(uri, "uri");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    this.successCounter = successCounter;
    this.listener = listener;
  }

  @Override
  public void send(String text) throws Exception {
    WebSocketClient c = client;
    if (c == null || !open.get()) {
      throw new IllegalStateException("WebSocket not open");
    }
    c.send(text);
  }

  @Override
  public boolean isOpen() {
    WebSocketClient c = client;
    return c != null && open.get() && c.isOpen();
  }

  @Override
  public void reconnect() throws Exception {
    metrics.reconnections().increment();
    closeSilently();
    connectBlocking(5, TimeUnit.SECONDS);
  }

  /**
   * Connects and blocks until the websocket is open or timeout occurs.
   *
   * @param timeout timeout duration
   * @param timeUnit timeout unit
   * @throws Exception if connection fails
   */
  public void connectBlocking(long timeout, TimeUnit timeUnit) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);

    client =
        new WebSocketClient(uri) {

          @Override
          public void onOpen(ServerHandshake handshake) {
            open.set(true);
            metrics.connectionsCreated().increment();
            latch.countDown();
          }

          @Override
          public void onMessage(String message) {
            if (successCounter != null) {
              successCounter.increment();
            }
            if (listener != null) {
              listener.onMessage(message);
            }
          }

          @Override
          public void onClose(int code, String reason, boolean remote) {
            open.set(false);
          }

          @Override
          public void onError(Exception ex) {
            open.set(false);
          }
        };

    client.connect();

    boolean ok = latch.await(timeout, timeUnit);
    if (!ok || !isOpen()) {
      closeSilently();
      throw new Exception("WebSocket connect timeout: " + uri);
    }
  }

  /**
   * Closes the websocket quietly.
   */
  public void closeSilently() {
    WebSocketClient c = client;
    if (c != null) {
      try {
        c.close();
      } catch (Exception ignored) {
      } finally {
        open.set(false);
      }
    }
  }
}
