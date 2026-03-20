package chatflow.consumer.db;

import chatflow.consumer.config.ConsumerConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps a HikariCP connection pool configured from environment variables.
 * Call getConnection() to borrow a connection; the caller must close() it to return it to the pool.
 * Call close() on shutdown to release all pooled connections.
 */
public class DbConnectionPool {

  private final HikariDataSource dataSource;

  public DbConnectionPool(ConsumerConfig config) {
    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(config.dbUrl);
    hikariConfig.setUsername(config.dbUser);
    hikariConfig.setPassword(config.dbPass);
    hikariConfig.setMaximumPoolSize(config.dbPoolSize);

    // How long a thread will wait for a connection before throwing an exception (30s)
    hikariConfig.setConnectionTimeout(30_000);
    // How long a connection can sit idle in the pool before being evicted (10 min)
    hikariConfig.setIdleTimeout(600_000);
    // Maximum lifetime of a connection in the pool (30 min)
    hikariConfig.setMaxLifetime(1_800_000);

    this.dataSource = new HikariDataSource(hikariConfig);
  }

  /**
   * Borrows a connection from the pool.
   * The caller is responsible for closing it (try-with-resources recommended).
   */
  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  /**
   * Shuts down the connection pool. Call this once on application shutdown.
   */
  public void close() {
    dataSource.close();
  }
}
