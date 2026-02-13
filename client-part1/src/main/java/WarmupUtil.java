import java.time.Instant;

/**
 * Utility methods used during the warmup phase of the load test.
 *
 * <p>This class builds deterministic warmup messages and serializes them
 * into JSON format for sending to the server.
 */
public final class WarmupUtil {

  /**
   * Private constructor to prevent instantiation.
   */
  private WarmupUtil() {}

  /**
   * Builds a warmup message for a given thread and sequence number.
   *
   * @param threadIndex index of the warmup thread
   * @param seq sequence number of the message
   * @return constructed warmup outbound message
   */
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

  /**
   * Serializes an outbound message into JSON format.
   *
   * @param msg outbound message
   * @return JSON string representation
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

  /**
   * Escapes special characters for JSON string values.
   *
   * @param msg input string
   * @return escaped string
   */
  private static String escape(String msg) {
    return msg.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
