import java.time.Instant;

/**
 * Represents a single latency measurement record for Part 3 analysis.
 *
 * <p>Each record captures the timestamp, message type, latency in milliseconds,
 * response status code, and room ID associated with a sent message.
 */
public final class LatencyRecord {

  private final Instant timestamp;
  private final MessageType messageType;
  private final long latencyMs;
  private final String statusCode;
  private final int roomId;

  /**
   * Creates a latency record.
   *
   * @param timestamp time when the acknowledgment was received
   * @param messageType type of the message
   * @param latencyMs measured latency in milliseconds
   * @param statusCode response status (e.g., OK or ERROR)
   * @param roomId room ID associated with the message
   */
  public LatencyRecord(Instant timestamp, MessageType messageType, long latencyMs,
                       String statusCode,
                       int roomId) {
    this.timestamp = timestamp;
    this.messageType = messageType;
    this.latencyMs = latencyMs;
    this.statusCode = statusCode;
    this.roomId = roomId;
  }

  /**
   * Returns the acknowledgment timestamp.
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Returns the message type.
   */
  public MessageType getMessageType() {
    return messageType;
  }

  /**
   * Returns the measured latency in milliseconds.
   */
  public long getLatencyMs() {
    return latencyMs;
  }

  /**
   * Returns the response status code.
   */
  public String getStatusCode() {
    return statusCode;
  }

  /**
   * Returns the room ID.
   */
  public int getRoomId() {
    return roomId;
  }
}
