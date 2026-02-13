/**
 * Abstraction for a channel capable of sending text messages.
 *
 * <p>Implementations may represent a real WebSocket connection
 * or a mock channel used for testing.
 */
public interface SendChannel {

  /**
   * Sends a text payload through the channel.
   *
   * @param text the message to send
   * @throws Exception if sending fails
   */
  void send(String text) throws Exception;

  /**
   * Returns whether the channel is currently open.
   *
   * @return true if open, false otherwise
   */
  boolean isOpen();

  /**
   * Attempts to reconnect the channel if it is closed.
   *
   * @throws Exception if reconnection fails
   */
  void reconnect() throws Exception;
}
