package chatflow.consumer.pool;

import chatflow.consumer.config.ConsumerConfig;
import chatflow.consumer.room.RoomDispatcher;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConsumerPool {

  private final Connection connection;
  private final RoomDispatcher dispatcher;
  private final ConsumerConfig config;
  private final List<RabbitConsumer> consumers = new ArrayList<>();
  private ExecutorService pool;

  public ConsumerPool(Connection connection, RoomDispatcher dispatcher, ConsumerConfig config) {
    this.connection = connection;
    this.dispatcher = dispatcher;
    this.config = config;
  }

  public void start() {
    pool = Executors.newFixedThreadPool(config.totalRooms);
    for ( int i = 1; i <= config.totalRooms; i++ ) {
      try {
        Channel channel = connection.createChannel();
        String queueName = "room." + i;
        RabbitConsumer consumer = new RabbitConsumer(channel, queueName, i, dispatcher,
            config.rabbitExchange, config.prefetchCount);
        consumers.add(consumer);
        pool.submit(consumer);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void shutdown() {
    consumers.forEach(consumer -> consumer.running = false);
    pool.shutdown();
    try {
      pool.awaitTermination(10, TimeUnit.SECONDS);
      connection.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
