import java.net.URI;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Maps roomId to a connected WsSendChannel, creating connections lazily.
 */
public final class RoomChannelPool {

  private final String baseWsUrl;
  private final Metrics metrics;
  private final AckListener listener;
  private final ConcurrentMap<Integer, WsSendChannel> map = new ConcurrentHashMap<>();

  public RoomChannelPool(String baseWsUrl, Metrics metrics, AckListener listener) {
    this.baseWsUrl = Objects.requireNonNull(baseWsUrl, "baseWsUrl");
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    this.listener = listener;
  }

  /**
   * Returns a connected channel for the given roomId, creating one if needed.
   */
  public WsSendChannel channel(int roomId) throws Exception {
    WsSendChannel ch = map.get(roomId);
    if (ch != null && ch.isOpen()) {
      return ch;
    }
    synchronized (this) {
      ch = map.get(roomId);
      if (ch != null && ch.isOpen()) {
        return ch;
      }
      URI uri = URI.create(baseWsUrl + roomId);
      WsSendChannel newCh = new WsSendChannel(uri, metrics, listener);
      newCh.connectBlocking(5, TimeUnit.SECONDS);
      map.put(roomId, newCh);
      return newCh;
    }
  }

  /**
   * Closes all channels in the pool.
   */
  public void closeAll() {
    for (WsSendChannel ch : map.values()) {
      ch.closeSilently();
    }
    map.clear();
  }
}
