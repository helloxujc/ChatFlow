package chatflow.server.room;

import chatflow.server.model.UserInfo;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import org.java_websocket.WebSocket;

/**
 * Manages WebSocket sessions grouped by room and provides thread-safe user registration, session removal, and message broadcasting.
 */
public class RoomManager {

  private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocket>> roomSessions
      = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, UserInfo> activeUsers = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<WebSocket, UserInfo> sessionUserMap = new ConcurrentHashMap<>();

  public final AtomicLong messagesProcessed = new AtomicLong(0);

  /**
   * Adds a WebSocket session to the specified room's session set.
   *
   * @param roomId the room to add the session to
   * @param ws     the WebSocket connection to register
   */
  public void addSession(String roomId, WebSocket ws) {
    roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(ws);
  }

  /**
   * Associates the given WebSocket connection with a user's metadata in both the active-user and session-user maps.
   *
   * @param ws       the WebSocket connection
   * @param userInfo the user metadata to associate
   */
  public void registerUser(WebSocket ws, UserInfo userInfo) {
    activeUsers.put(userInfo.getUserId(), userInfo);
    sessionUserMap.put(ws, userInfo);
  }

  /**
   * Removes a WebSocket session from the room and cleans up the associated user records.
   *
   * @param roomId the room the session belongs to
   * @param ws     the WebSocket connection to remove
   */
  public void removeSession(String roomId, WebSocket ws) {
    CopyOnWriteArraySet<WebSocket> sessions = roomSessions.get(roomId);
    if (sessions != null) {
      sessions.remove(ws);
    }
    UserInfo userInfo = sessionUserMap.remove(ws);
    if (userInfo != null) {
      activeUsers.remove(userInfo.getUserId());
    }
  }

  /**
   * Sends the payload string to every open WebSocket session in the specified room.
   *
   * @param roomId  the room whose sessions should receive the message
   * @param payload the JSON string to send to each session
   */
  public void broadcast(String roomId, String payload) {
    CopyOnWriteArraySet<WebSocket> sessions = roomSessions.get(roomId);
    if (sessions == null || sessions.isEmpty()) {
      return;
    }
    for (WebSocket ws : sessions) {
      if (ws != null && ws.isOpen()) {
        try {
          ws.send(payload);
        } catch (Exception ignored) {
        }
      }
    }
    messagesProcessed.incrementAndGet();
  }

  /**
   * Returns the number of active WebSocket sessions currently in the specified room.
   *
   * @param roomId the room to query
   * @return the session count, or {@code 0} if the room has no sessions
   */
  public int getSessionCount(String roomId) {
    CopyOnWriteArraySet<WebSocket> sessions = roomSessions.get(roomId);
    return sessions == null ? 0 : sessions.size();
  }

  public Set<String> getActiveRoomIds() {
    Set<String> active = new HashSet<>();
    for (Map.Entry<String, CopyOnWriteArraySet<WebSocket>> entry : roomSessions.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        active.add(entry.getKey());
      }
    }
    return active;
  }

}
