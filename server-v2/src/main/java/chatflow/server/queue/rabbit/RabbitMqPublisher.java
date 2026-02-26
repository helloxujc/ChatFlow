package chatflow.server.queue.rabbit;

import chatflow.server.queue.MessagePublisher;
import chatflow.server.queue.QueueMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * RabbitMQ publisher using a topic exchange and per-room routing keys.
 */
public final class RabbitMqPublisher implements MessagePublisher {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final ChannelPool channelPool;
  private final String exchangeName;

  /**
   * Creates a publisher.
   *
   * @param channelPool channel pool
   * @param exchangeName topic exchange name
   */
  public RabbitMqPublisher(ChannelPool channelPool, String exchangeName) {
    this.channelPool = Objects.requireNonNull(channelPool, "channelPool");
    this.exchangeName = Objects.requireNonNull(exchangeName, "exchangeName");
  }

  @Override
  public void publish(QueueMessage msg) throws Exception {
    Objects.requireNonNull(msg, "msg");
    String routingKey = "room." + msg.getRoomId();
    byte[] body = MAPPER.writeValueAsString(msg).getBytes(StandardCharsets.UTF_8);

    Channel ch = null;
    try {
      ch = channelPool.borrow();
      ch.basicPublish(
          exchangeName,
          routingKey,
          MessageProperties.PERSISTENT_TEXT_PLAIN,
          body);
    } finally {
      channelPool.release(ch);
    }
  }

  @Override
  public void close() throws Exception {
    channelPool.close();
  }
}
