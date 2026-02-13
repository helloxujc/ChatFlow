import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Computes latency statistics and summary breakdowns from collected records.
 */
public final class StatisticsCalculator {

  private StatisticsCalculator() {}

  /**
   * Prints latency statistics to stdout.
   *
   * @param records latency records
   */
  public static void printStats(List<LatencyRecord> records) {
    if (records.isEmpty()) {
      System.out.println("No latency records.");
      return;
    }

    List<Long> latencies =
        records.stream()
            .map(LatencyRecord::getLatencyMs)
            .sorted()
            .collect(Collectors.toList());

    long min = latencies.get(0);
    long max = latencies.get(latencies.size() - 1);
    double mean = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);

    long median = percentile(latencies, 50);
    long p95 = percentile(latencies, 95);
    long p99 = percentile(latencies, 99);

    System.out.println("=== LATENCY STATS ===");
    System.out.println("Mean ms: " + mean);
    System.out.println("Median ms: " + median);
    System.out.println("P95 ms: " + p95);
    System.out.println("P99 ms: " + p99);
    System.out.println("Min ms: " + min);
    System.out.println("Max ms: " + max);
  }

  /**
   * Prints throughput per room (messages/second) using ack timestamps.
   *
   * @param records latency records
   */
  public static void printThroughputPerRoom(List<LatencyRecord> records) {
    if (records.isEmpty()) {
      System.out.println("No records for throughput per room.");
      return;
    }

    double seconds = Math.max(0.001, computeDurationSeconds(records));
    Map<Integer, Long> counts =
        records.stream()
            .collect(Collectors.groupingBy(LatencyRecord::getRoomId, TreeMap::new, Collectors.counting()));

    System.out.println("=== THROUGHPUT PER ROOM (msg/s) ===");
    for (Map.Entry<Integer, Long> e : counts.entrySet()) {
      double t = e.getValue() / seconds;
      System.out.println("room " + e.getKey() + ": " + t);
    }
  }

  /**
   * Prints message type distribution (count and percent) based on collected records.
   *
   * @param records latency records
   */
  public static void printMessageTypeDistribution(List<LatencyRecord> records) {
    if (records.isEmpty()) {
      System.out.println("No records for message type distribution.");
      return;
    }

    Map<MessageType, Long> counts = new EnumMap<>(MessageType.class);
    for (LatencyRecord r : records) {
      counts.put(r.getMessageType(), counts.getOrDefault(r.getMessageType(), 0L) + 1L);
    }

    long total = records.size();
    System.out.println("=== MESSAGE TYPE DISTRIBUTION ===");
    for (MessageType t : MessageType.values()) {
      long c = counts.getOrDefault(t, 0L);
      double pct = (total == 0) ? 0.0 : (100.0 * c / total);
      System.out.println(t.name() + ": " + c + " (" + pct + "%)");
    }
  }

  private static long percentile(List<Long> sorted, int p) {
    int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
    index = Math.max(0, Math.min(index, sorted.size() - 1));
    return sorted.get(index);
  }

  /**
   * Computes the total test duration in seconds using the min/max ack timestamps.
   *
   * @param records latency records
   * @return duration in seconds
   */
  private static double computeDurationSeconds(List<LatencyRecord> records) {
    long min =
        records.stream()
            .map(LatencyRecord::getTimestamp)
            .mapToLong(Instant::toEpochMilli)
            .min()
            .orElse(0L);

    long max =
        records.stream()
            .map(LatencyRecord::getTimestamp)
            .mapToLong(Instant::toEpochMilli)
            .max()
            .orElse(min);

    return (max - min) / 1000.0;
  }
}

