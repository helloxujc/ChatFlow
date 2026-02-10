import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class WarmupRunner {

  public WarmupRunner() {}

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
                  URI uri = new URI(baseWsUrl + "1");
                  channel = new WsSendChannel(uri, metrics);
                  channel.connectBlocking(5, TimeUnit.SECONDS);

                  for (int j = 0; j < perThread; j++) {
                    OutboundMessage msg = WarmupUtil.buildWarmupMessage(threadIndex, j);
                    channel.send(WarmupUtil.toJson(msg));
                    success.increment();
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

  public static final class WarmupResult {
    private final Duration duration;
    private final long success;
    private final long failed;
    private final long connections;

    public WarmupResult(Duration duration, long success, long failed, long connections) {
      this.duration = duration;
      this.success = success;
      this.failed = failed;
      this.connections = connections;
    }

    public Duration getDuration() {
      return duration;
    }

    public long getSuccess() {
      return success;
    }

    public long getFailed() {
      return failed;
    }

    public long getConnections() {
      return connections;
    }
  }
}
