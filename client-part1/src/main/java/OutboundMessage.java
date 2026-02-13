import java.time.Instant;
import java.util.Objects;

/**
 * Represents an outbound chat message to be sent by the load client.
 *
 * <p>This immutable model contains all fields required to build a chat payload,
 * along with a retry attempt counter for resend logic.
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
   * Creates a new outbound message with attempt count set to zero.
   *
   * @param seqId sequence ID
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
   * Creates a new outbound message with a specified retry attempt.
   *
   * @param seqId sequence ID
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
   * Returns a new instance with the attempt count incremented by one.
   *
   * @return new outbound message with incremented attempt
   */
  public OutboundMessage nextAttempt() {
    return new OutboundMessage(
        seqId, userId, username, message, roomId, messageType, timestamp, attempt + 1
    );
  }
}
