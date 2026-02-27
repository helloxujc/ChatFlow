package chatflow.server.room;

import chatflow.server.model.UserInfo;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import org.java_websocket.WebSocket;

public class RoomManager {

  private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocket>> roomSessions
      = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, UserInfo> activeUsers = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<WebSocket, UserInfo> sessionUserMap = new ConcurrentHashMap<>();

  public final AtomicLong messagesProcessed = new AtomicLong(0);

  public void addSession(String roomId, WebSocket ws, UserInfo userInfo) {
    roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(ws);
    activeUsers.put(userInfo.getUserId(), userInfo);
    sessionUserMap.put(ws, userInfo);
  }

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

  public int getSessionCount(String roomId) {
    CopyOnWriteArraySet<WebSocket> sessions = roomSessions.get(roomId);
    return sessions == null ? 0 : sessions.size();
  }

}
