import java.util.concurrent.ThreadLocalRandom;

public enum MessageType {

  TEXT,
  JOIN,
  LEAVE;

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
