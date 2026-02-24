package chatflow.server.queue;

import chatflow.server.model.ChatMessage;

/**
 * Message envelope published to the message queue.
 */
public final class QueueMessage {
  private String messageId;
  private String roomId;
  private String userId;
  private String username;
  private String message;
  private String timestamp;
  private String messageType;
  private String serverId;
  private String clientIp;

  /**
   * Creates an empty instance for JSON serialization/deserialization.
   */
  public QueueMessage() {}

  /**
   * Builds a queue message from an incoming chat message.
   *
   * @param messageId unique message id
   * @param roomId room id as string
   * @param msg validated chat message
   * @param serverId server identifier
   * @param clientIp client ip
   * @return queue message
   */
  public static QueueMessage from(
      String messageId, String roomId, ChatMessage msg, String serverId, String clientIp) {
    QueueMessage qm = new QueueMessage();
    qm.messageId = messageId;
    qm.roomId = roomId;
    qm.userId = msg.getUserId();
    qm.username = msg.getUsername();
    qm.message = msg.getMessage();
    qm.timestamp = msg.getTimestamp();
    qm.messageType = msg.getMessageType();
    qm.serverId = serverId;
    qm.clientIp = clientIp;
    return qm;
  }

  public String getMessageId() {
    return messageId;
  }

  public String getRoomId() {
    return roomId;
  }

  public String getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }

  public String getMessage() {
    return message;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public String getMessageType() {
    return messageType;
  }

  public String getServerId() {
    return serverId;
  }

  public String getClientIp() {
    return clientIp;
  }
}
