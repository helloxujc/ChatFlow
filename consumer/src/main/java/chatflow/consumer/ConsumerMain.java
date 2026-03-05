package chatflow.consumer;

import chatflow.consumer.config.ConsumerConfig;
import chatflow.consumer.notifier.BroadcastClient;
import chatflow.consumer.pool.ConsumerPool;
import chatflow.consumer.room.RoomDispatcher;
import chatflow.consumer.room.StripedExecutor;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Entry point for the message consumer service.
 * This service pulls messages from the queue and broadcasts them to WebSocket clients.
 */
public class ConsumerMain {

    /**
     * Main method that starts the consumer service.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) throws IOException, TimeoutException {

        ConsumerConfig config = ConsumerConfig.get();
        BroadcastClient client = new BroadcastClient(config);
        StripedExecutor stripedExecutor = new StripedExecutor(config.stripeCount);
        RoomDispatcher roomDispatcher = new RoomDispatcher(stripedExecutor, client);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.rabbitHost);
        factory.setPort(config.rabbitPort);
        factory.setUsername(config.rabbitUser);
        factory.setPassword(config.rabbitPass);
        Connection connection = factory.newConnection();

        ConsumerPool consumerPool = new ConsumerPool(connection, roomDispatcher, config);
        consumerPool.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumerPool.shutdown();
            stripedExecutor.shutdown();
        }));

        System.out.println("Consumer service started");
    }
}