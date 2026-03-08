package chatflow.consumer.pool;

import com.rabbitmq.client.Delivery;

/**
 * Bundles a RabbitMQ delivery with its corresponding ack and nack callbacks for deferred acknowledgement.
 */
public class DeliveryTask {

  public final Delivery delivery;
  public final Runnable ack;
  public final Runnable nack;

  public DeliveryTask(Delivery delivery, Runnable ack, Runnable nack) {
    this.delivery = delivery;
    this.ack = ack;
    this.nack = nack;
  }
}
