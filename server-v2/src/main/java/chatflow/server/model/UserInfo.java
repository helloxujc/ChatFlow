package chatflow.server.model;

import java.time.Instant;

public class UserInfo {
  private final String userId;
  private final String userName;
  private final String roomId;
  private final Instant connectedAt;

  public UserInfo(String userId, String userName, String roomId, Instant connectedAt) {
    this.userId = userId;
    this.userName = userName;
    this.roomId = roomId;
    this.connectedAt = Instant.now();
  }

  public String getUserId() {
    return userId;
  }

  public String getUserName() {
    return userName;
  }

  public String getRoomId() {
    return roomId;
  }

  public Instant getConnectedAt() {
    return connectedAt;
  }
}