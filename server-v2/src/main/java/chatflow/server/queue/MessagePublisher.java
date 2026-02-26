package chatflow.server.queue;

/**
 * Publishes messages to a backing message queue.
 */
public interface MessagePublisher {

  /**
   * Publishes the given message.
   *
   * @param msg queue message
   * @throws Exception if publishing fails
   */
  void publish(QueueMessage msg) throws Exception;

  /**
   * Closes any underlying resources.
   *
   * @throws Exception if close fails
   */
  void close() throws Exception;
}