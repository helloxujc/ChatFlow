import java.util.concurrent.ThreadLocalRandom;

/**
 * Enumeration of supported chat message types.
 *
 * <p>Provides a helper method to generate a random message type
 * using a weighted distribution.
 */
public enum MessageType {

  TEXT,
  JOIN,
  LEAVE;

  /**
   * Returns a randomly selected message type.
   *
   * <p>Distribution:
   * <ul>
   *   <li>90% TEXT</li>
   *   <li>5% JOIN</li>
   *   <li>5% LEAVE</li>
   * </ul>
   *
   * @return randomly selected message type
   */
  public static MessageType randomType() {
    int value = ThreadLocalRandom.current().nextInt(100);
    if (value < 90) {
      return TEXT;
    } else if (value < 95) {
      return JOIN;
    } else {
      return LEAVE;
    }
  }
}
