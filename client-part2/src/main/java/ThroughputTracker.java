import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Computes throughput over time using fixed-size time buckets.
 */
public final class ThroughputTracker {

  private ThroughputTracker() {}

  /**
   * Aggregates records into 10-second buckets and returns messages/sec per bucket.
   *
   * @param records latency records
   * @return ordered map of bucketStartEpochMs -> messagesPerSecond
   */
  public static Map<Long, Double> computeMessagesPerSecond(List<LatencyRecord> records) {
    if (records.isEmpty()) {
      return Map.of();
    }

    long bucketSizeMs = 10_000L;

    long minTs =
        records.stream()
            .map(LatencyRecord::getTimestamp)
            .mapToLong(Instant::toEpochMilli)
            .min()
            .orElse(0L);

    Map<Long, Integer> counts = new TreeMap<>();
    for (LatencyRecord r : records) {
      long ts = r.getTimestamp().toEpochMilli();
      long bucketStart = ((ts - minTs) / bucketSizeMs) * bucketSizeMs + minTs;
      counts.put(bucketStart, counts.getOrDefault(bucketStart, 0) + 1);
    }

    Map<Long, Double> mps = new TreeMap<>();
    for (Map.Entry<Long, Integer> e : counts.entrySet()) {
      mps.put(e.getKey(), e.getValue() / 10.0);
    }
    return mps;
  }
}
