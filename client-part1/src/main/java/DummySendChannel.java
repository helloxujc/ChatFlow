import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DummySendChannel implements SendChannel {

  private final AtomicBoolean open = new AtomicBoolean(true);
  private final int failureRatePrecent;

  public DummySendChannel(int failureRatePrecent) {
    this.failureRatePrecent = failureRatePrecent;
  }

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

  @Override
  public boolean isOpen() {
    return open.get();
  }

  @Override
  public void reconnect() throws Exception {
    open.set(true);
  }

  public void close() {
    open.set(false);
  }
}
