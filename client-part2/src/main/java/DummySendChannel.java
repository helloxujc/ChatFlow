import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mock implementation of {@link SendChannel} used for testing retry logic.
 *
 * <p>This channel simulates random send failures based on a configurable
 * failure rate percentage.
 */
public final class DummySendChannel implements SendChannel {

  private final AtomicBoolean open = new AtomicBoolean(true);
  private final int failureRatePrecent;

  /**
   * Creates a dummy send channel.
   *
   * @param failureRatePrecent percentage chance (0-100) of simulated failure
   */
  public DummySendChannel(int failureRatePrecent) {
    this.failureRatePrecent = failureRatePrecent;
  }

  /**
   * Simulates sending a message and randomly throws an exception
   * according to the configured failure rate.
   *
   * @param text message payload
   * @throws Exception if the channel is closed or a simulated failure occurs
   */
  @Override
  public void send(String text) throws Exception {
    if (!open.get()) {
      throw new IllegalStateException("Channel is closed");
    }
    int r = ThreadLocalRandom.current().nextInt(100);
    if (r < failureRatePrecent) {
      throw new Exception("Simulated send failure");
    }
  }

  /**
   * Returns whether the channel is currently open.
   *
   * @return true if open, false otherwise
   */
  @Override
  public boolean isOpen() {
    return open.get();
  }

  /**
   * Reopens the channel.
   *
   * @throws Exception if reconnection fails
   */
  @Override
  public void reconnect() throws Exception {
    open.set(true);
  }

  /**
   * Closes the channel.
   */
  public void close() {
    open.set(false);
  }
}