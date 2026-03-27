package chatflow.consumer.pool;

import chatflow.consumer.room.RoomDispatcher;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Consumes messages from a single room queue and dispatches each delivery to the {@link chatflow.consumer.room.RoomDispatcher} for processing.
 */
public class RabbitConsumer implements Runnable {

  private final Channel channel;
  private final String queueName;
  private final int roomId;
  private final RoomDispatcher dispatcher;
  private final String exchange;
  private final int prefetchCount;
  private final BlockingQueue<Runnable> ackQueue = new LinkedBlockingQueue<>();
  public volatile boolean running = true;

  public RabbitConsumer(Channel channel, String queueName, int roomId, RoomDispatcher dispatcher,
                        String exchange, int prefetchCount) {
    this.channel = channel;
    this.queueName = queueName;
    this.roomId = roomId;
    this.dispatcher = dispatcher;
    this.exchange = exchange;
    this.prefetchCount = prefetchCount;
  }

  @Override
  public void run() {
    try {
      channel.basicQos(prefetchCount);
      Map<String, Object> queueArgs = new HashMap<>();
      queueArgs.put("x-message-ttl", 300_000);  // drop messages older than 5 min
      queueArgs.put("x-max-length", 100_000);   // max 100k messages per queue
      channel.queueDeclare(queueName, true, false, false, queueArgs);
      channel.queueBind(queueName, exchange, "room." + roomId);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      channel.basicConsume(queueName, false, (tag, delivery) -> {
        long deliveryTag = delivery.getEnvelope().getDeliveryTag();
        DeliveryTask task = new DeliveryTask(
            delivery,
            () -> {
              try {
                channel.basicAck(deliveryTag, false);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            },
            () -> {
              try {
                channel.basicNack(deliveryTag, false, true);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
        dispatcher.dispatch(task, ackQueue);
      },
          tag -> {}
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    while (running) {
      List<Runnable> ackTasks = new ArrayList<>();
      ackQueue.drainTo(ackTasks);
      ackTasks.forEach(Runnable::run);
      if (ackTasks.isEmpty()) {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }


}
