package chatflow.consumer.db;

import chatflow.consumer.model.BroadcastRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Executes batch SQL writes to PostgreSQL.
 * Both tables are written in a single transaction — if either fails, both roll back.
 */
public class MessageRepository {

  private final DbConnectionPool pool;

  private static final String INSERT_MESSAGE =
      "INSERT INTO messages" +
      " (message_id, room_id, user_id, username, message, message_type, client_timestamp, server_id, client_ip)" +
      " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" +
      " ON CONFLICT (message_id) DO NOTHING";

  private static final String UPSERT_ACTIVITY =
      "INSERT INTO user_room_activity (user_id, room_id, last_activity)" +
      " VALUES (?, ?, ?)" +
      " ON CONFLICT (user_id, room_id)" +
      " DO UPDATE SET last_activity = GREATEST(EXCLUDED.last_activity, user_room_activity.last_activity)";

  public MessageRepository(DbConnectionPool pool) {
    this.pool = pool;
  }

  /**
   * Persists a batch of messages to both tables in a single transaction.
   * Rolls back both tables if any write fails.
   *
   * @throws SQLException if the batch fails after all retries
   */
  public void persistBatch(List<BroadcastRequest> batch) throws SQLException {
    try (Connection conn = pool.getConnection()) {
      conn.setAutoCommit(false);
      try {
        insertMessages(conn, batch);
        upsertActivity(conn, batch);
        conn.commit();
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    }
  }

  private void insertMessages(Connection conn, List<BroadcastRequest> batch) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(INSERT_MESSAGE)) {
      for (BroadcastRequest msg : batch) {
        ps.setObject(1, parseUuid(msg.getMessageId()));
        ps.setString(2, msg.getRoomId());
        ps.setString(3, msg.getUserId());
        ps.setString(4, msg.getUsername());
        ps.setString(5, msg.getMessage());
        ps.setString(6, msg.getMessageType());
        ps.setTimestamp(7, parseTimestamp(msg.getTimestamp()));
        ps.setString(8, msg.getServerId());
        ps.setString(9, msg.getClientIp());
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private void upsertActivity(Connection conn, List<BroadcastRequest> batch) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(UPSERT_ACTIVITY)) {
      for (BroadcastRequest msg : batch) {
        ps.setString(1, msg.getUserId());
        ps.setString(2, msg.getRoomId());
        ps.setTimestamp(3, parseTimestamp(msg.getTimestamp()));
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private UUID parseUuid(String id) {
    try {
      return UUID.fromString(id);
    } catch (Exception e) {
      return UUID.randomUUID();
    }
  }

  private Timestamp parseTimestamp(String iso) {
    try {
      return Timestamp.from(Instant.parse(iso));
    } catch (Exception e) {
      return Timestamp.from(Instant.now());
    }
  }
}
