import java.time.Instant;

public final class WarmupUtil {

  private WarmupUtil() {}

  public static OutboundMessage buildWarmupMessage(int threadIndex, int seq) {
    int userId = threadIndex + 1;
    String username = "warmup-user-" + userId;

    return new OutboundMessage(
        seq,
        userId,
        username,
        "warmup-message-" + seq,
        1,
        MessageType.TEXT,
        Instant.now());
  }

  public static String toJson(OutboundMessage msg) {
    return "{"
        + "\"userId\":\"" + msg.getUserId() + "\","
        + "\"username\":\"" + escape(msg.getUsername()) + "\","
        + "\"message\":\"" + escape(msg.getMessage()) + "\","
        + "\"timestamp\":\"" + msg.getTimestamp().toString() + "\","
        + "\"messageType\":\"" + msg.getMessageType().name() + "\""
        + "}";
  }

  private static String escape(String msg) {
    return msg.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
