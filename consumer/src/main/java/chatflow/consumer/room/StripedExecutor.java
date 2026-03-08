package chatflow.consumer.room;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Distributes tasks across a fixed number of single-threaded executor lanes so that tasks with the same room ID always run on the same lane in order.
 */
public class StripedExecutor {

  private final ExecutorService[] stripes;

  public StripedExecutor(int stripeCount) {
    stripes = new ExecutorService[stripeCount];
    for (int i = 0; i < stripeCount; i++) {
      stripes[i] = Executors.newSingleThreadExecutor();
    }
  }

  /**
   * Submits a task to the executor stripe that corresponds to the given room ID.
   *
   * @param roomId the room ID used to select the stripe
   * @param task   the runnable to execute on the selected stripe
   */
  public void submit(int roomId, Runnable task) {
    int stripe = (roomId - 1) % stripes.length;
    stripes[stripe].submit(task);
  }

  /**
   * Initiates an orderly shutdown of all stripe executors.
   */
  public void shutdown() {
    Arrays.stream(stripes).forEach(ExecutorService::shutdown);
  }
}
