import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.LongAdder;

public final class Metrics {

  private final LongAdder success = new LongAdder();
  private final LongAdder failed = new LongAdder();
  private final LongAdder connectionsCreated = new LongAdder();
  private final LongAdder reconnections = new LongAdder();

  private volatile Instant start;
  private volatile Instant end;

  public void start() {
    this.start = Instant.now();
    this.end = null;
  }

  public void stop() {
    this.end = Instant.now();
  }

  public LongAdder success() {
    return this.success;
  }

  public LongAdder failed() {
    return this.failed;
  }

  public LongAdder connectionsCreated() {
    return this.connectionsCreated;
  }

  public LongAdder reconnections() {
    return this.reconnections;
  }

  public Duration duration() {
    Instant s = start;
    Instant e = end;
    if (s == null) {
      return Duration.ZERO;
    }
    if (e == null) {
      e = Instant.now();
    }
    return Duration.between(s, e);
  }

  public double throughputMsgPerSec(){
    double seconds = Math.max(0.001, duration().toMillis() / 1000.0);
    return success.sum() / seconds;
  }
}
