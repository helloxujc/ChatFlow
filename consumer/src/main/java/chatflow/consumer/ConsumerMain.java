package chatflow.consumer;

/**
 * Entry point for the message consumer service.
 * This service pulls messages from the queue and
 * broadcasts them to WebSocket clients.
 */
public class ConsumerMain {

    /**
     * Main method that starts the consumer service.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Consumer service started");
    }
}