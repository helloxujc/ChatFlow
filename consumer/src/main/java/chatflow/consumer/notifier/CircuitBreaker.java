package chatflow.consumer.notifier;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks consecutive failures for a single downstream endpoint and opens the circuit to stop calls when the failure threshold is exceeded.
 */
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

  /**
   * Returns {@code true} if the circuit is closed or ready to try a probe request, {@code false} if the circuit is open and the cooldown has not elapsed.
   *
   * @return whether the caller is allowed to attempt a request
   */
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

  /**
   * Resets the failure counter and closes the circuit after a successful request.
   */
  public void onSuccess() {
    consecutiveFailures.set(0);
    state.set(State.CLOSED);
  }

  /**
   * Increments the failure counter and opens the circuit if the failure threshold has been reached.
   */
  public void onFailure() {
    consecutiveFailures.addAndGet(1);
    if (consecutiveFailures.get() >= failThreshold) {
      Long now = System.currentTimeMillis();
      openUntil.set(now + openDurationMs);
      state.set(State.OPEN);
    }
  }

}
