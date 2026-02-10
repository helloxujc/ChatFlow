import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public final class WsSendChannel implements SendChannel {

  private final URI uri;
  private final Metrics metrics;
  private final AtomicBoolean open = new AtomicBoolean(false);
  private volatile WebSocketClient client;
  private final AckListener listener;

  public WsSendChannel(URI uri, Metrics metrics) {
    this(uri, metrics, null);
  }

  public WsSendChannel(URI uri, Metrics metrics, AckListener listener) {
    this.uri = Objects.requireNonNull(uri, "uri");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
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
