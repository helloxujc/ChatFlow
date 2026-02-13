import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.LongAdder;

/**
 * Collects and computes runtime metrics for the load client.
 *
 * <p>This class tracks successful and failed messages, connection statistics,
 * and calculates duration and throughput.
 */
public final class Metrics {

  private final LongAdder success = new LongAdder();
  private final LongAdder failed = new LongAdder();
  private final LongAdder connectionsCreated = new LongAdder();
  private final LongAdder reconnections = new LongAdder();

  private volatile Instant start;
  private volatile Instant end;

  /**
   * Marks the start time of measurement.
   */
  public void start() {
    this.start = Instant.now();
    this.end = null;
  }

  /**
   * Marks the end time of measurement.
   */
  public void stop() {
    this.end = Instant.now();
  }

  /**
   * Returns the counter for successful messages.
   *
   * @return success counter
   */
  public LongAdder success() {
    return this.success;
  }

  /**
   * Returns the counter for failed messages.
   *
   * @return failed counter
   */
  public LongAdder failed() {
    return this.failed;
  }

  /**
   * Returns the counter for created connections.
   *
   * @return connections created counter
   */
  public LongAdder connectionsCreated() {
    return this.connectionsCreated;
  }

  /**
   * Returns the counter for reconnections.
   *
   * @return reconnections counter
   */
  public LongAdder reconnections() {
    return this.reconnections;
  }

  /**
   * Calculates the total duration between start and stop.
   *
   * @return duration of the measurement
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
