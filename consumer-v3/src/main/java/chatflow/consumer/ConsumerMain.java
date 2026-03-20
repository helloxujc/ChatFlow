package chatflow.consumer;

import chatflow.consumer.config.ConsumerConfig;
import chatflow.consumer.db.DbConnectionPool;
import chatflow.consumer.db.DbWriter;
import chatflow.consumer.db.MessageRepository;
import chatflow.consumer.model.BroadcastRequest;
import chatflow.consumer.notifier.BroadcastClient;
import chatflow.consumer.notifier.CircuitBreaker;
import chatflow.consumer.pool.ConsumerPool;
import chatflow.consumer.room.RoomDispatcher;
import chatflow.consumer.room.StripedExecutor;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

/**
 * Entry point for the consumer-v3 service.
 * Pulls messages from RabbitMQ, broadcasts to WebSocket servers, and persists to PostgreSQL.
 */
public class ConsumerMain {

    public static void main(String[] args) throws IOException, TimeoutException {

        ConsumerConfig config = ConsumerConfig.get();

        // --- DB setup ---
        LinkedBlockingQueue<BroadcastRequest> dbQueue = new LinkedBlockingQueue<>(100_000);
        DbConnectionPool dbPool = new DbConnectionPool(config);
        MessageRepository repo = new MessageRepository(dbPool);
        CircuitBreaker dbCircuitBreaker = new CircuitBreaker(config.dbCbFailThreshold, config.dbCbOpenMs);
        DbWriter dbWriter = new DbWriter(dbQueue, repo, dbCircuitBreaker, config.dbBatchSize, config.dbFlushMs);
        Thread dbWriterThread = new Thread(dbWriter, "db-writer");
        dbWriterThread.start();

        // --- Broadcast setup ---
        int stripeCount = args.length > 0 ? Integer.parseInt(args[0]) : config.stripeCount;
        BroadcastClient broadcastClient = new BroadcastClient(config);
        StripedExecutor stripedExecutor = new StripedExecutor(stripeCount);
        RoomDispatcher roomDispatcher = new RoomDispatcher(stripedExecutor, broadcastClient, dbQueue);

        // --- RabbitMQ setup ---
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.rabbitHost);
        factory.setPort(config.rabbitPort);
        factory.setUsername(config.rabbitUser);
        factory.setPassword(config.rabbitPass);
        Connection connection = factory.newConnection();

        ConsumerPool consumerPool = new ConsumerPool(connection, roomDispatcher, config);
        consumerPool.start();

        // --- Shutdown hook ---
        // Order matters: stop consuming first, then flush DB, then close connections
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[ConsumerMain] Shutting down...");

            // 1. Stop consuming from RabbitMQ
            consumerPool.shutdown();
            stripedExecutor.shutdown();
            broadcastClient.shutdown();

            // 2. Signal DbWriter to stop, wait for final flush
            dbWriter.stop();
            try {
                dbWriterThread.join(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 3. Close DB connection pool
            dbPool.close();

            System.out.println("[ConsumerMain] Shutdown complete. Dead letter count: "
                + dbWriter.getDeadLetterCount());
        }));

        System.out.println("[ConsumerMain] Consumer-v3 started");
    }
}