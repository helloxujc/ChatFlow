package chatflow.server.queue.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Simple channel pool for RabbitMQ publishing.
 */
public final class ChannelPool implements AutoCloseable {

  private final BlockingQueue<Channel> pool;
  private final Connection connection;

  /**
   * Creates a pool with a single shared connection and pre-created channels.
   *
   * @param host rabbitmq host
   * @param port rabbitmq port
   * @param username rabbitmq username
   * @param password rabbitmq password
   * @param poolSize number of channels to pre-create
   * @throws Exception if initialization fails
   */
  public ChannelPool(
      String host, int port, String username, String password, int poolSize) throws Exception {
    Objects.requireNonNull(host, "host");
    if (poolSize < 1) {
      throw new IllegalArgumentException("poolSize must be >= 1");
    }

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(host);
    factory.setPort(port);
    factory.setUsername(username);
    factory.setPassword(password);

    this.connection = factory.newConnection("chatflow-producer");
    this.pool = new ArrayBlockingQueue<>(poolSize);

    for (int i = 0; i < poolSize; i++) {
      Channel ch = connection.createChannel();
      pool.add(ch);
    }
  }

  /**
   * Borrows a channel from the pool (blocking).
   *
   * @return channel
   * @throws InterruptedException if interrupted
   */
  public Channel borrow() throws InterruptedException {
    return pool.take();
  }

  /**
   * Returns a channel back to the pool.
   *
   * @param channel channel to return
   */
  public void release(Channel channel) {
    if (channel != null) {
      pool.offer(channel);
    }
  }

  @Override
  public void close() throws Exception {
    for (Channel ch : pool) {
      try {
        ch.close();
      } catch (Exception ignored) {
      }
    }
    connection.close();
  }
}
