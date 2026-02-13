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
import java.util.concurrent.atomic.LongAdder;

/**
 * Measures average WebSocket round-trip time (RTT) by sending probe messages and
 * correlating echoes via {@code data.seqId}.
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

            JsonNode seqNode = data.get("seqId");
            if (seqNode == null || !seqNode.isNumber()) {
              parseFailures.incrementAndGet();
              return;
            }

            long seqId = seqNode.asLong();
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

    LongAdder dummySuccess = new LongAdder();
    WsSendChannel channel = new WsSendChannel(uri, metrics, dummySuccess, listener);
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
   * Builds a probe JSON payload including seqId so the server echoes it back.
   *
   * @param seqId sequence id
   * @return json payload
   */
  private static String buildProbeJson(long seqId) {
    return "{"
        + "\"userId\":\"1\","
        + "\"username\":\"probe\","
        + "\"message\":\"probe\","
        + "\"timestamp\":\"" + Instant.now().toString() + "\","
        + "\"messageType\":\"TEXT\","
        + "\"seqId\":" + seqId
        + "}";
  }
}
