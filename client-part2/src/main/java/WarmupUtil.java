import java.time.Instant;

/**
 * Helper utilities for generating warmup messages.
 */
public final class WarmupUtil {

  private WarmupUtil() {}

  /**
   * Builds a warmup message targeting the given room id.
   *
   * @param threadIndex warmup thread index
   * @param seq message sequence number
   * @param roomId target room id
   * @return outbound message
   */
  public static OutboundMessage buildWarmupMessage(int threadIndex, int seq, int roomId) {
    int userId = threadIndex + 1;
    String username = "warmup-user-" + userId;

    return new OutboundMessage(
        seq,
        userId,
        username,
        "warmup-message-" + seq,
        roomId,
        MessageType.TEXT,
        Instant.now());
  }

  /**
   * Serializes an outbound message to JSON.
   *
   * @param msg outbound message
   * @return JSON string
   */
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
