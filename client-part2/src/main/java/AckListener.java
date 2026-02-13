/**
 * Listener interface for receiving acknowledgment messages from the server.
 *
 * <p>Implementations define how incoming text frames are processed,
 * typically to record successful message delivery.
 */
public interface AckListener {

  /**
   * Invoked when a text message is received.
   *
   * @param text the received message payload
   */
  void onMessage(String text);

}
