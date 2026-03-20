package chatflow.consumer.room;

import chatflow.consumer.model.BroadcastRequest;
import chatflow.consumer.notifier.BroadcastClient;
import chatflow.consumer.pool.DeliveryTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Routes incoming delivery tasks to the correct striped executor lane based on room ID,
 * broadcasts the decoded message to all servers, and offers it to the DB write queue.
 * If the DB queue is full (backpressure), the message is nacked so RabbitMQ requeues it.
 */
public class RoomDispatcher {

  private final StripedExecutor stripedExecutor;
  private final BroadcastClient broadcastClient;
  private final LinkedBlockingQueue<BroadcastRequest> dbQueue;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public RoomDispatcher(StripedExecutor stripedExecutor,
                        BroadcastClient broadcastClient,
                        LinkedBlockingQueue<BroadcastRequest> dbQueue) {
    this.stripedExecutor = stripedExecutor;
    this.broadcastClient = broadcastClient;
    this.dbQueue = dbQueue;
  }

  /**
   * Submits the delivery task to the stripe for the message's room ID.
   * After a successful broadcast, the message is offered to the DB queue.
   * If the DB queue is full, the message is nacked and requeued in RabbitMQ.
   *
   * @param task     the delivery task containing the raw message and ack/nack callbacks
   * @param ackQueue the queue to which the ack or nack runnable will be offered after processing
   */
  public void dispatch(DeliveryTask task, BlockingQueue<Runnable> ackQueue) {
    String routingKey = task.delivery.getEnvelope().getRoutingKey();
    int roomId = parseRoomId(routingKey);
    stripedExecutor.submit(roomId, () -> {
      try {
        BroadcastRequest req = MAPPER.readValue(task.delivery.getBody(),
            BroadcastRequest.class);
        broadcastClient.broadcast(req);
        boolean accepted = dbQueue.offer(req);
        if (accepted) {
          ackQueue.offer(task.ack);
        } else {
          // DB queue full — nack so RabbitMQ redelivers once pressure eases
          ackQueue.offer(task.nack);
        }
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
