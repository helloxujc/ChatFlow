import java.time.Instant;
import java.util.Objects;

/**
 * Immutable representation of a message to be sent by the load client.
 *
 * <p>Contains all fields required for JSON serialization, along with
 * a retry attempt counter used for resend logic.
 */
public final class OutboundMessage {

  private final long seqId;
  private final int userId;
  private final String username;
  private final String message;
  private final int roomId;
  private final MessageType messageType;
  private final Instant timestamp;
  private final int attempt;

  /**
   * Creates a new outbound message with an initial attempt count of zero.
   *
   * @param seqId sequence identifier
   * @param userId user ID
   * @param username username
   * @param message message content
   * @param roomId room ID
   * @param messageType message type
   * @param timestamp message timestamp
   */
  public OutboundMessage(
      long seqId,
      int userId,
      String username,
      String message,
      int roomId,
      MessageType messageType,
      Instant timestamp) {
    this(seqId, userId, username, message, roomId, messageType, timestamp, 0);
  }

  /**
   * Creates a new outbound message with a specific retry attempt.
   *
   * @param seqId sequence identifier
   * @param userId user ID
   * @param username username
   * @param message message content
   * @param roomId room ID
   * @param messageType message type
   * @param timestamp message timestamp
   * @param attempt retry attempt count
   */
  public OutboundMessage(
      long seqId,
      int userId,
      String username,
      String message,
      int roomId,
      MessageType messageType,
      Instant timestamp,
      int attempt) {

    this.seqId = seqId;
    this.userId = userId;
    this.username = Objects.requireNonNull(username, "username");
    this.message = Objects.requireNonNull(message, "message");
    this.roomId = roomId;
    this.messageType = Objects.requireNonNull(messageType, "messageType");
    this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    this.attempt = attempt;
  }

  /**
   * Returns the sequence ID.
   */
  public long getSeqId() {
    return seqId;
  }

  /**
   * Returns the user ID.
   */
  public int getUserId() {
    return userId;
  }

  /**
   * Returns the username.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Returns the message content.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns the room ID.
   */
  public int getRoomId() {
    return roomId;
  }

  /**
   * Returns the message type.
   */
  public MessageType getMessageType() {
    return messageType;
  }

  /**
   * Returns the timestamp.
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Returns the retry attempt count.
   */
  public int getAttempt() {
    return attempt;
  }

  /**
   * Returns a new message instance with the attempt count incremented.
   *
   * @return new outbound message with incremented attempt
   */
  public OutboundMessage nextAttempt() {
    return new OutboundMessage(
        seqId, userId, username, message, roomId, messageType, timestamp, attempt + 1
    );
  }
}
