/**
 * Abstraction of a message sending channel.
 *
 * <p>Implementations provide the ability to send text messages,
 * check connection state, and attempt reconnection when needed.
 */
public interface SendChannel {

  /**
   * Sends a text message through the channel.
   *
   * @param text message payload
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
   * Attempts to reconnect the channel.
   *
   * @throws Exception if reconnection fails
   */
  void reconnect() throws Exception;
}

