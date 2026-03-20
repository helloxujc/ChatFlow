package chatflow.server.metrics;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs all core queries and analytics queries against PostgreSQL for the /metrics endpoint.
 * Uses a small read-only HikariCP connection pool.
 */
public class MetricsRepository {

  private final HikariDataSource dataSource;

  public MetricsRepository() {
    HikariConfig cfg = new HikariConfig();
    cfg.setJdbcUrl(env("DB_URL", "jdbc:postgresql://localhost:5432/chatflow"));
    cfg.setUsername(env("DB_USER", "chatflow"));
    cfg.setPassword(env("DB_PASS", "chatflow"));
    cfg.setMaximumPoolSize(5);
    cfg.setReadOnly(true);
    this.dataSource = new HikariDataSource(cfg);
  }

  // Core Query 1: message count for a room in the last hour
  public long getRoomMessageCount(String roomId) throws SQLException {
    String sql = "SELECT COUNT(*) FROM messages WHERE room_id = ? AND client_timestamp >= ?";
    Timestamp since = Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS));
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, roomId);
      ps.setTimestamp(2, since);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getLong(1) : 0;
      }
    }
  }

  // Core Query 2: total messages sent by a user
  public long getUserMessageCount(String userId) throws SQLException {
    String sql = "SELECT COUNT(*) FROM messages WHERE user_id = ?";
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getLong(1) : 0;
      }
    }
  }

  // Core Query 3: unique active users in the last hour
  public long countActiveUsers() throws SQLException {
    String sql = "SELECT COUNT(DISTINCT user_id) FROM messages WHERE client_timestamp >= ?";
    Timestamp since = Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS));
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setTimestamp(1, since);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getLong(1) : 0;
      }
    }
  }

  // Core Query 4: rooms a user has participated in (uses user_room_activity table)
  public List<Map<String, Object>> getRoomsByUser(String userId) throws SQLException {
    String sql = "SELECT room_id, last_activity FROM user_room_activity" +
                 " WHERE user_id = ? ORDER BY last_activity DESC";
    List<Map<String, Object>> result = new ArrayList<>();
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("roomId", rs.getString("room_id"));
          row.put("lastActivity", rs.getTimestamp("last_activity").toInstant().toString());
          result.add(row);
        }
      }
    }
    return result;
  }

  // Analytics 1: total messages per minute over the last hour
  public Map<String, Long> getMessagesPerMinute() throws SQLException {
    String sql = "SELECT to_char(date_trunc('minute', client_timestamp), 'HH24:MI') AS minute," +
                 " COUNT(*) AS cnt" +
                 " FROM messages WHERE client_timestamp >= ?" +
                 " GROUP BY 1 ORDER BY 1";
    Timestamp since = Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS));
    Map<String, Long> result = new LinkedHashMap<>();
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setTimestamp(1, since);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.put(rs.getString("minute"), rs.getLong("cnt"));
        }
      }
    }
    return result;
  }

  // Analytics 2: top N most active users by message count
  public List<Map<String, Object>> getTopUsers(int n) throws SQLException {
    String sql = "SELECT user_id, username, COUNT(*) AS cnt FROM messages" +
                 " GROUP BY user_id, username ORDER BY cnt DESC LIMIT ?";
    List<Map<String, Object>> result = new ArrayList<>();
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, n);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("userId", rs.getString("user_id"));
          row.put("username", rs.getString("username"));
          row.put("messageCount", rs.getLong("cnt"));
          result.add(row);
        }
      }
    }
    return result;
  }

  // Analytics 3: top N most active rooms by message count
  public List<Map<String, Object>> getTopRooms(int n) throws SQLException {
    String sql = "SELECT room_id, COUNT(*) AS cnt FROM messages" +
                 " GROUP BY room_id ORDER BY cnt DESC LIMIT ?";
    List<Map<String, Object>> result = new ArrayList<>();
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, n);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("roomId", rs.getString("room_id"));
          row.put("messageCount", rs.getLong("cnt"));
          result.add(row);
        }
      }
    }
    return result;
  }

  public void close() {
    dataSource.close();
  }

  private static String env(String key, String def) {
    String v = System.getenv(key);
    return (v != null && !v.isBlank()) ? v : def;
  }
}
