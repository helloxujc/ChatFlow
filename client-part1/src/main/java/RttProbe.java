import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class RttProbe {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private RttProbe() {}

  public static double measureAverageRttMs(URI uri, int samples) throws Exception {
    Metrics metrics = new Metrics();
    CountDownLatch latch = new CountDownLatch(samples);
    Map<Long, Long> sendTimesNs = new ConcurrentHashMap<>();
    long[] sumNs = new long[] {0L};

    AckListener listener =
        text -> {
          try {
            JsonNode node = MAPPER.readTree(text);
            JsonNode data = node.get("data");
            if (data == null) {
              return;
            }
            JsonNode seqNode = data.get("seqId");
            if (seqNode == null || !seqNode.isNumber()) {
              return;
            }
            long seqId = seqNode.asLong();
            Long startNs = sendTimesNs.remove(seqId);
            if (startNs != null) {
              long rttNs = System.nanoTime() - startNs;
              synchronized (sumNs) {
                sumNs[0] += rttNs;
              }
              latch.countDown();
            }
          } catch (Exception ignored) {

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
      throw new Exception("RTT probe timed out; received=" + (samples - latch.getCount()));
    }

    double avgNs;
    synchronized (sumNs) {
      avgNs = sumNs[0] / (double) samples;
    }
    return avgNs / 1_000_000.0;
  }

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
