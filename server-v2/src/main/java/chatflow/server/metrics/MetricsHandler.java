package chatflow.server.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP handler for GET /metrics.
 * Runs all core queries and analytics queries and returns the results as JSON.
 * Intended to be called once after a load test completes.
 */
public class MetricsHandler implements HttpHandler {

  private final MetricsRepository repo;
  private final ObjectMapper mapper = new ObjectMapper();

  // Sample identifiers used for core query examples in the report
  private static final String SAMPLE_ROOM = "1";
  private static final String SAMPLE_USER = "user1";
  private static final int TOP_N = 10;

  public MetricsHandler(MetricsRepository repo) {
    this.repo = repo;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(405, -1);
      return;
    }

    try {
      Map<String, Object> response = buildMetrics();
      byte[] body = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(response);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(body);
      }
    } catch (Exception e) {
      String error = "{\"error\": \"" + e.getMessage() + "\"}";
      byte[] body = error.getBytes();
      exchange.sendResponseHeaders(500, body.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(body);
      }
    } finally {
      exchange.close();
    }
  }

  private Map<String, Object> buildMetrics() throws Exception {
    Map<String, Object> root = new LinkedHashMap<>();

    // Core queries
    Map<String, Object> core = new LinkedHashMap<>();
    core.put("room" + SAMPLE_ROOM + "_messages_last_hour", repo.getRoomMessageCount(SAMPLE_ROOM));
    core.put("user" + SAMPLE_USER + "_total_messages",     repo.getUserMessageCount(SAMPLE_USER));
    core.put("active_users_last_hour",                     repo.countActiveUsers());
    core.put("user" + SAMPLE_USER + "_rooms",              repo.getRoomsByUser(SAMPLE_USER));
    root.put("coreQueries", core);

    // Analytics queries
    Map<String, Object> analytics = new LinkedHashMap<>();
    analytics.put("messagesPerMinute", repo.getMessagesPerMinute());
    analytics.put("topUsers",          repo.getTopUsers(TOP_N));
    analytics.put("topRooms",          repo.getTopRooms(TOP_N));
    root.put("analytics", analytics);

    return root;
  }
}
