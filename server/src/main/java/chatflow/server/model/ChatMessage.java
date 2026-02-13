package chatflow.server.model;

/**
 * Represents a chat message sent by a client through the WebSocket server.
 *
 * <p>This model contains user information, message content, message type,
 * timestamp, and a sequence ID used for tracking or ordering.
 */
public class ChatMessage {
  private String userId;
  private String username;
  private String message;
  private String timestamp;
  private String messageType;
  private Long seqId;

  /**
   * Default constructor for serialization and deserialization.
   */
  public ChatMessage() {
  }

  /**
   * Returns the user ID of the sender.
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Sets the user ID of the sender.
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * Returns the username of the sender.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username of the sender.
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Returns the message content.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the message content.
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Returns the timestamp of the message.
   */
  public String getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the timestamp of the message.
   */
  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Returns the message type (TEXT, JOIN, or LEAVE).
   */
  public String getMessageType() {
    return messageType;
  }

  /**
   * Sets the message type.
   */
  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  /**
   * Returns the sequence ID of the message.
   */
  public Long getSeqId() {
    return seqId;
  }

  /**
   * Sets the sequence ID of the message.
   */
  public void setSeqId(Long seqId) {
    this.seqId = seqId;
  }
}
