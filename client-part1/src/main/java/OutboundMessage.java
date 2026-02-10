import java.time.Instant;
import java.util.Objects;

public final class OutboundMessage {

  private final long seqId;
  private final int userId;
  private final String username;
  private final String message;
  private final int roomId;
  private final MessageType messageType;
  private final Instant timestamp;
  private final int attempt;

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

  public long getSeqId() {
    return seqId;
  }

  public int getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }

  public String getMessage() {
    return message;
  }

  public int getRoomId() {
    return roomId;
  }

  public MessageType getMessageType() {
    return messageType;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public int getAttempt() {
    return attempt;
  }

  public OutboundMessage nextAttempt() {
    return new OutboundMessage(
        seqId, userId, username, message, roomId, messageType, timestamp, attempt + 1
    );
  }
}
