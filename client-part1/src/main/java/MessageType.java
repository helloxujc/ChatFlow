import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents the type of a chat message.
 *
 * <p>This enum defines supported message types and provides
 * a utility method to generate a random type with a weighted distribution.
 */
public enum MessageType {

  TEXT,
  JOIN,
  LEAVE;

  /**
   * Returns a random message type using the following distribution:
   * 90% TEXT, 5% JOIN, 5% LEAVE.
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
