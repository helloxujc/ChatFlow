package chatflow.consumer.room;

import chatflow.consumer.model.BroadcastRequest;
import chatflow.consumer.notifier.BroadcastClient;
import chatflow.consumer.pool.DeliveryTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.BlockingQueue;

public class RoomDispatcher {

  private final StripedExecutor stripedExecutor;
  private final BroadcastClient broadcastClient;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public RoomDispatcher(StripedExecutor stripedExecutor, BroadcastClient broadcastClient) {
    this.stripedExecutor = stripedExecutor;
    this.broadcastClient = broadcastClient;
  }

  public void dispatch(DeliveryTask task, BlockingQueue<Runnable> ackQueue) {
    String routingKey = task.delivery.getEnvelope().getRoutingKey();
    int roomId = parseRoomId(routingKey);
    stripedExecutor.submit(roomId, () -> {
      try {
        BroadcastRequest req = MAPPER.readValue(task.delivery.getBody(),
            BroadcastRequest.class);
        broadcastClient.broadcast(req);
        ackQueue.offer(task.ack);
      } catch (Exception e) {
        ackQueue.offer(task.nack);
      }
    });
  }

  private int parseRoomId(String routingKey) {
    String[] parts = routingKey.split("\\.");
    return Integer.parseInt(parts[1]);
  }
}
