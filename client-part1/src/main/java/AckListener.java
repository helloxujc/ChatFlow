/**
 * Listener interface for handling acknowledgment messages from the server.
 *
 * <p>Implementations define how incoming WebSocket text messages
 * (typically acknowledgments) are processed.
 */
public interface AckListener {

  /**
   * Called when a text message is received from the server.
   *
   * @param text the received message payload
   */
  void onMessage(String text);

}
