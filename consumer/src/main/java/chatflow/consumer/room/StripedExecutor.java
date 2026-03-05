package chatflow.consumer.room;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StripedExecutor {

  private final ExecutorService[] stripes;

  public StripedExecutor(int stripeCount) {
    stripes = new ExecutorService[stripeCount];
    for (int i = 0; i < stripeCount; i++) {
      stripes[i] = Executors.newSingleThreadExecutor();
    }
  }

  public void submit(int roomId, Runnable task) {
    int stripe = (roomId - 1) % stripes.length;
    stripes[stripe].submit(task);
  }

  public void shutdown() {
    Arrays.stream(stripes).forEach(ExecutorService::shutdown);
  }
}
