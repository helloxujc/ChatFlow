import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Runs the warmup phase by creating multiple short-lived connections and sending
 * a fixed number of messages per thread. Success is counted only when ack is received.
 */
public final class WarmupRunner {

  private WarmupRunner() {}

  /**
   * Runs warmup load against the given WebSocket base URL.
   *
   * @param baseWsUrl base url like "ws://host:8081/chat/"
   * @return warmup result
   * @throws InterruptedException if interrupted while waiting for warmup completion
   */
  public static WarmupResult run(String baseWsUrl) throws InterruptedException {
    int threads = ClientConfig.WARMUP_THREADS;
    int perThread = ClientConfig.WARMUP_MSG_PER_THREAD;

    Metrics metrics = new Metrics();
    LongAdder success = metrics.success();
    LongAdder failed = metrics.failed();

    CountDownLatch done = new CountDownLatch(threads);
    Instant start = Instant.now();

    for (int i = 0; i < threads; i++) {
      int threadIndex = i;
      Thread t =
          new Thread(
              () -> {
                WsSendChannel channel = null;
                try {
                  int roomId = (threadIndex % 20) + 1;
                  URI uri = URI.create(baseWsUrl + roomId);

                  channel = new WsSendChannel(uri, metrics, success);
                  channel.connectBlocking(5, TimeUnit.SECONDS);

                  for (int j = 0; j < perThread; j++) {
                    OutboundMessage msg = WarmupUtil.buildWarmupMessage(threadIndex, j);
                    channel.send(WarmupUtil.toJson(msg));
                  }

                  long deadline = System.currentTimeMillis() + 10_000;
                  while (System.currentTimeMillis() < deadline && success.sum() < (long) threads * perThread) {
                    Thread.sleep(50);
                  }
                } catch (Exception e) {
                  failed.increment();
                } finally {
                  if (channel != null) {
                    channel.closeSilently();
                  }
                  done.countDown();
                }
              },
              "warmup-" + i);
      t.start();
    }

    done.await();
    Instant end = Instant.now();

    return new WarmupResult(
        Duration.between(start, end),
        success.sum(),
        failed.sum(),
        metrics.connectionsCreated().sum());
  }

  /**
   * Holds aggregated warmup metrics.
   */
  public static final class WarmupResult {
    private final Duration duration;
    private final long success;
    private final long failed;
    private final long connections;

    /**
     * Creates a warmup result.
     *
     * @param duration total warmup duration
     * @param success number of acked messages
     * @param failed number of failed operations
     * @param connections number of connections created
     */
    public WarmupResult(Duration duration, long success, long failed, long connections) {
      this.duration = duration;
      this.success = success;
      this.failed = failed;
      this.connections = connections;
    }

    /**
     * Returns total warmup duration.
     *
     * @return duration
     */
    public Duration getDuration() {
      return duration;
    }

    /**
     * Returns number of successful (acked) messages.
     *
     * @return success count
     */
    public long getSuccess() {
      return success;
    }

    /**
     * Returns number of failed operations.
     *
     * @return failed count
     */
    public long getFailed() {
      return failed;
    }

    /**
     * Returns number of connections created.
     *
     * @return connections count
     */
    public long getConnections() {
      return connections;
    }
  }
}
