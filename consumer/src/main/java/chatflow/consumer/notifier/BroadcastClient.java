package chatflow.consumer.notifier;

import chatflow.consumer.config.ConsumerConfig;
import chatflow.consumer.model.BroadcastRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends broadcast requests to all configured chat servers in parallel, using per-server circuit breakers to skip unhealthy endpoints.
 */
public class BroadcastClient {

  private final String[] serverUrls;
  private final Map<String, CircuitBreaker> breakers = new HashMap<>();
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final ExecutorService broadcastPool;
  private final Map<String, Set<String>> roomCache = new ConcurrentHashMap<>();
  private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor();

  public BroadcastClient(ConsumerConfig config) {
    this.serverUrls = config.serverUrls;
    Integer cbFailThreshold = config.cbFailThreshold;
    Long cbOpenMs = config.cbOpenMs;
    for (String singleUrl : serverUrls) {
      CircuitBreaker singleBreaker = new CircuitBreaker(cbFailThreshold, cbOpenMs);
      this.breakers.put(singleUrl, singleBreaker);
    }
    this.broadcastPool = Executors.newFixedThreadPool(serverUrls.length * 32);
    poller.scheduleAtFixedRate(this::pollAllRooms, 0, 500, TimeUnit.MILLISECONDS);
  }

  private void pollAllRooms() {
    for (String url : serverUrls) {
      try {
        HttpURLConnection conn = (HttpURLConnection) new URL(url + "/rooms").openConnection();
        conn.setConnectTimeout(500);
        conn.setReadTimeout(500);
        if (conn.getResponseCode() == 200) {
          try (InputStream is = conn.getInputStream()) {
            Set<String> rooms = objectMapper.readValue(is, new TypeReference<Set<String>>() {});
            roomCache.put(url, rooms);
          }
        }
      } catch (Exception ignored) {
      }
    }
  }

  private List<String> getTargetServers(String roomId) {
    boolean allCached = roomCache.size() == serverUrls.length;
    if (!allCached) return Arrays.asList(serverUrls);
    List<String> targets = new ArrayList<>();
    for (String url : serverUrls) {
      Set<String> rooms = roomCache.get(url);
      if (rooms == null || rooms.contains(roomId)) {
        targets.add(url);
      }
    }
    return targets.isEmpty() ? Arrays.asList(serverUrls) : targets;
  }

  /**
   * Sends the broadcast request to every healthy server URL concurrently, waiting for all to complete.
   *
   * @param request the message payload to broadcast
   */
  public void broadcast(BroadcastRequest request) {
    String json;
    try {
      json = objectMapper.writeValueAsString(request);
    } catch (IOException e){
      throw new RuntimeException(e);
    }
    String roomId = request.getRoomId();
    List<String> targets = getTargetServers(roomId);
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (String singleUrl : targets) {
      CircuitBreaker singlebreaker = breakers.get(singleUrl);
      if (!singlebreaker.allowRequest()) {
        continue;
      }
      futures.add(CompletableFuture.runAsync(() -> {
        try {
          int code = postBroadcast(singleUrl, json);
          if (code == 200) {
            singlebreaker.onSuccess();
          } else {
            singlebreaker.onFailure();
          }
        } catch (Exception e) {
          singlebreaker.onFailure();
        }
      }, broadcastPool));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
  }

  private int postBroadcast(String serverUrl, String payload) throws Exception {
    URL url = new URL(serverUrl + "/internal/broadcast");

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setConnectTimeout(1000);
    conn.setReadTimeout(1000);

    conn.setRequestProperty("Content-Type", "application/json");
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
      os.write(payload.getBytes());
      os.flush();
    }

    return conn.getResponseCode();
  }

  /** Shuts down the internal broadcast thread pool and room poller. */
  public void shutdown() {
    poller.shutdown();
    broadcastPool.shutdown();
  }

}
