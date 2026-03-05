package chatflow.consumer.notifier;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CircuitBreaker {

  private enum State { CLOSED, OPEN, HALF_OPEN }
  private Integer failThreshold;
  private long openDurationMs;
  AtomicInteger consecutiveFailures = new AtomicInteger(0);
  AtomicLong openUntil = new AtomicLong(0);
  AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

  public CircuitBreaker(int failThreshold, long openDurationMs) {
    this.failThreshold = failThreshold;
    this.openDurationMs = openDurationMs;
  }

  public boolean allowRequest(){
    Long now = System.currentTimeMillis();

    if (state.get() == State.CLOSED) {
      return true;
    }
    if (state.get() == State.OPEN) {
      if (now > openUntil.get()) {
        state.set(State.HALF_OPEN);
        return true;
      }
      return false;
    }
    return true;
  }

  public void onSuccess() {
    consecutiveFailures.set(0);
    state.set(State.CLOSED);
  }

  public void onFailure() {
    consecutiveFailures.addAndGet(1);
    if (consecutiveFailures.get() >= failThreshold) {
      Long now = System.currentTimeMillis();
      openUntil.set(now + openDurationMs);
      state.set(State.OPEN);
    }
  }

}
