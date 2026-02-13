import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Measures average WebSocket round-trip time (RTT) by sending probe messages and
 * correlating echoes using a sequence id embedded in the message text.
 */
public final class RttProbe {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private RttProbe() {}

  /**
   * Measures average RTT in milliseconds using {@code samples} probe messages.
   *
   * @param uri websocket endpoint
   * @param samples number of probe messages
   * @return average RTT in milliseconds
   * @throws Exception if connection fails or probe times out
   */
  public static double measureAverageRttMs(URI uri, int samples) throws Exception {
    Metrics metrics = new Metrics();
    CountDownLatch latch = new CountDownLatch(samples);
    Map<Long, Long> sendTimesNs = new ConcurrentHashMap<>();
    AtomicLong sumNs = new AtomicLong(0L);
    AtomicInteger parseFailures = new AtomicInteger(0);
    AtomicInteger matched = new AtomicInteger(0);

    AckListener listener =
        text -> {
          try {
            JsonNode node = MAPPER.readTree(text);
            JsonNode data = node.get("data");
            if (data == null) {
              parseFailures.incrementAndGet();
              return;
            }

            JsonNode msgNode = data.get("message");
            if (msgNode == null || !msgNode.isTextual()) {
              parseFailures.incrementAndGet();
              return;
            }

            long seqId = parseSeqId(msgNode.asText());
            if (seqId < 0) {
              parseFailures.incrementAndGet();
              return;
            }

            Long startNs = sendTimesNs.remove(seqId);
            if (startNs != null) {
              long rttNs = System.nanoTime() - startNs;
              sumNs.addAndGet(rttNs);
              matched.incrementAndGet();
              latch.countDown();
            }
          } catch (Exception e) {
            parseFailures.incrementAndGet();
          }
        };

    WsSendChannel channel = new WsSendChannel(uri, metrics, listener);
    channel.connectBlocking(5, TimeUnit.SECONDS);

    for (int i = 0; i < samples; i++) {
      long seqId = i;
      String payload = buildProbeJson(seqId);
      sendTimesNs.put(seqId, System.nanoTime());
      channel.send(payload);
    }

    boolean ok = latch.await(10, TimeUnit.SECONDS);
    channel.closeSilently();

    if (!ok) {
      throw new Exception(
          "RTT probe timed out; matched=" + matched.get()
              + ", remaining=" + latch.getCount()
              + ", parseFailures=" + parseFailures.get());
    }

    double avgNs = sumNs.get() / (double) samples;
    return avgNs / 1_000_000.0;
  }

  /**
   * Builds a probe JSON payload with a sequence id embedded in the message.
   *
   * @param seqId sequence id
   * @return json payload
   */
  private static String buildProbeJson(long seqId) {
    String msg = "seq:" + seqId + "|probe";
    return "{"
        + "\"userId\":\"1\","
        + "\"username\":\"probe\","
        + "\"message\":\"" + msg + "\","
        + "\"timestamp\":\"" + Instant.now().toString() + "\","
        + "\"messageType\":\"TEXT\""
        + "}";
  }

  /**
   * Parses the sequence id from a message formatted as "seq:<id>|...".
   *
   * @param message echoed message text
   * @return parsed sequence id, or -1 if not found/invalid
   */
  private static long parseSeqId(String message) {
    int idx = message.indexOf("seq:");
    if (idx < 0) {
      return -1;
    }
    int start = idx + 4;
    int bar = message.indexOf('|', start);
    if (bar < 0) {
      return -1;
    }
    String num = message.substring(start, bar).trim();
    try {
      return Long.parseLong(num);
    } catch (NumberFormatException e) {
      return -1;
    }
  }
}
