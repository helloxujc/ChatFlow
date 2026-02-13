import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.LongAdder;

/**
 * Collects runtime metrics for the load test execution.
 *
 * <p>This class tracks success/failure counts, connection statistics,
 * total duration, and computes overall throughput.
 */
public final class Metrics {

  private final LongAdder success = new LongAdder();
  private final LongAdder failed = new LongAdder();
  private final LongAdder connectionsCreated = new LongAdder();
  private final LongAdder reconnections = new LongAdder();

  private volatile Instant start;
  private volatile Instant end;

  /**
   * Marks the start of the measurement period.
   */
  public void start() {
    this.start = Instant.now();
    this.end = null;
  }

  /**
   * Marks the end of the measurement period.
   */
  public void stop() {
    this.end = Instant.now();
  }

  /**
   * Returns the success counter.
   *
   * @return success counter
   */
  public LongAdder success() {
    return this.success;
  }

  /**
   * Returns the failure counter.
   *
   * @return failure counter
   */
  public LongAdder failed() {
    return this.failed;
  }

  /**
   * Returns the connections created counter.
   *
   * @return connections created counter
   */
  public LongAdder connectionsCreated() {
    return this.connectionsCreated;
  }

  /**
   * Returns the reconnections counter.
   *
   * @return reconnections counter
   */
  public LongAdder reconnections() {
    return this.reconnections;
  }

  /**
   * Computes the total duration of the measurement.
   *
   * @return duration between start and stop
   */
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

  /**
   * Calculates throughput in messages per second.
   *
   * @return throughput value
   */
  public double throughputMsgPerSec() {
    double seconds = Math.max(0.001, duration().toMillis() / 1000.0);
    return success.sum() / seconds;
  }
}
