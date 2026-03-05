package chatflow.consumer.notifier;

import chatflow.consumer.config.ConsumerConfig;
import chatflow.consumer.model.BroadcastRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BroadcastClient {

  private final String[] serverUrls;
  private final Map<String, CircuitBreaker> breakers = new HashMap<>();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public BroadcastClient(ConsumerConfig config) {
    this.serverUrls = config.serverUrls;
    Integer cbFailThreshold = config.cbFailThreshold;
    Long cbOpenMs = config.cbOpenMs;
    for (String singleUrl : serverUrls) {
      CircuitBreaker singleBreaker = new CircuitBreaker(cbFailThreshold, cbOpenMs);
      this.breakers.put(singleUrl, singleBreaker);
    }
  }

  public void broadcast(BroadcastRequest request) {
    String json;
    try {
      json = objectMapper.writeValueAsString(request);
    } catch (IOException e){
      throw new RuntimeException(e);
    }
    for (String singleUrl : serverUrls) {
      CircuitBreaker singlebreaker = breakers.get(singleUrl);
      if (!singlebreaker.allowRequest()) {
        continue;
      }
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
    }
  }

  private int postBroadcast(String serverUrl, String payload) throws Exception {
    URL url = new URL(serverUrl + "/internal/broadcast");

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setConnectTimeout(5000);
    conn.setReadTimeout(5000);

    conn.setRequestProperty("Content-Type", "application/json");
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
      os.write(payload.getBytes());
      os.flush();
    }

    return conn.getResponseCode();
  }

}
