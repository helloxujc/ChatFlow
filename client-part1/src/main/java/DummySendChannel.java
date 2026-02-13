import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A mock implementation of {@link SendChannel} used for testing.
 *
 * <p>This channel simulates message sending with a configurable failure rate.
 * It can randomly throw exceptions to mimic network or send failures.
 */
public final class DummySendChannel implements SendChannel {

  private final AtomicBoolean open = new AtomicBoolean(true);
  private final int failureRatePrecent;

  /**
   * Creates a dummy channel with a given simulated failure rate.
   *
   * @param failureRatePrecent percentage (0-100) chance of send failure
   */
  public DummySendChannel(int failureRatePrecent) {
    this.failureRatePrecent = failureRatePrecent;
  }

  /**
   * Sends a message and randomly fails based on the configured failure rate.
   *
   * @param text the message payload
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
