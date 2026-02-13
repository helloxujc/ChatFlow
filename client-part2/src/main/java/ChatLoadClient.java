import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Entry point for running the multithreaded WebSocket load test client.
 *
 * <p>This client performs:
 * <ul>
 *   <li>An RTT probe and Little's Law throughput estimate</li>
 *   <li>A warmup phase with short-lived connections</li>
 *   <li>A main phase that sends the full workload using a message queue and sender workers</li>
 *   <li>Part 3 collection and reporting (CSV output, stats, and throughput chart)</li>
 * </ul>
 */
public final class ChatLoadClient {

  /**
   * Private constructor to prevent instantiation.
   */
  private ChatLoadClient() {}

  /**
   * Runs the probe, warmup, and main load test phases against the configured server.
   *
   * @param args command-line arguments (not used)
   * @throws Exception if any networking or coordination step fails
   */
  public static void main(String[] args) throws Exception {
    String serverBase = "ws://ec2-54-213-224-201.us-west-2.compute.amazonaws.com:8081";
    String chatPrefix = serverBase + "/chat/";

    URI probeUri = new URI(chatPrefix + "1");

    int senderThreadsForMain = 4;
    int pipelineDepthPerConnection = 1;

    double avgRttMs = RttProbe.measureAverageRttMs(probeUri, 200);
    double wSeconds = avgRttMs / 1000.0;
    double lInFlight = (double) senderThreadsForMain * pipelineDepthPerConnection;
    double predictedThroughput = lInFlight / Math.max(0.000001, wSeconds);

    System.out.println("=== LITTLE'S LAW (PROBE) ===");
    System.out.println("Avg RTT ms: " + avgRttMs);
    System.out.println("Assumed in-flight L: " + lInFlight);
    System.out.println("Predicted max throughput msg/s: " + predictedThroughput);

    WarmupRunner.WarmupResult warmup = WarmupRunner.run(chatPrefix);
    printWarmup(warmup);

    runMainPhaseOnce(new URI(serverBase), avgRttMs);
  }

  /**
   * Prints warmup phase metrics, including throughput and connection counts.
   *
   * @param r warmup results
   */
  private static void printWarmup(WarmupRunner.WarmupResult r) {
    double seconds = Math.max(0.001, r.getDuration().toMillis() / 1000.0);
    double throughput = r.getSuccess() / seconds;

    System.out.println("=== WARMUP ===");
    System.out.println("Warmup success: " + r.getSuccess());
    System.out.println("Warmup failed: " + r.getFailed());
    System.out.println("Warmup connections: " + r.getConnections());
    System.out.println("Warmup wall time ms: " + r.getDuration().toMillis());
    System.out.println("Warmup throughput msg/s: " + throughput);
  }

  /**
   * Executes the main phase by sending the full workload and generating Part 3 artifacts.
   *
   * @param serverBaseUri base server URI
   * @param avgRttMs average RTT measured during the probe phase
   * @throws Exception if connection setup, sending, or output generation fails
   */
  private static void runMainPhaseOnce(URI serverBaseUri, double avgRttMs) throws Exception {
    int senderThreads = 4;
    int messagesToSend = ClientConfig.TOTAL_MSG;

    Metrics metrics = new Metrics();
    Part3Collector collector = new Part3Collector();
    LongAdder success = metrics.success();
    LongAdder failed = metrics.failed();

    BlockingQueue<OutboundMessage> queue = new ArrayBlockingQueue<>(ClientConfig.QUEUE_CAPACITY);

    String[] pool = MessageGenerator.defaultMessagePool();

    OutboundMessage poisonPill =
        new OutboundMessage(-1L, 1, "poison", "poison", 1, MessageType.TEXT, Instant.EPOCH);

    ExecutorService senders = Executors.newFixedThreadPool(senderThreads);

    String chatPrefix = serverBaseUri.toString() + "/chat/";
    RoomChannelPool channelPool = new RoomChannelPool(chatPrefix, metrics, collector::onAck);

    for (int roomId = 1; roomId <= 20; roomId++) {
      WsSendChannel ch = channelPool.channel(roomId);
    }

    for (int i = 0; i < senderThreads; i++) {
      senders.submit(new SenderWorker(queue, poisonPill, channelPool, success, failed, collector));
    }

    Instant start = Instant.now();

    Thread generator =
        new Thread(
            new MessageGenerator(queue, messagesToSend, senderThreads, poisonPill, pool),
            "message-generator");

    metrics.start();
    generator.start();
    generator.join();

    senders.shutdown();
    senders.awaitTermination(10, TimeUnit.MINUTES);

    awaitLateAcks(collector, 120_000);

    for (int roomId = 1; roomId <= 20; roomId++) {
      try {
        channelPool.channel(roomId).closeSilently();
      } catch (Exception ignored) {
        // Best-effort close.
      }
    }

    metrics.stop();

    Duration dur = Duration.between(start, Instant.now());
    double seconds = Math.max(0.001, dur.toMillis() / 1000.0);
    double throughput = success.sum() / seconds;

    System.out.println("=== MAIN ===");
    System.out.println("Successful messages sent: " + metrics.success().sum());
    System.out.println("Failed messages: " + metrics.failed().sum());
    System.out.println("Total runtime ms: " + metrics.duration().toMillis());
    System.out.println("Overall throughput msg/s: " + metrics.throughputMsgPerSec());
    System.out.println("Total connections: " + metrics.connectionsCreated().sum());
    System.out.println("Reconnections: " + metrics.reconnections().sum());

    double wSeconds = avgRttMs / 1000.0;
    double actualThroughput = metrics.throughputMsgPerSec();
    double impliedL = actualThroughput * wSeconds;
    double predictedFromImplied = impliedL / Math.max(0.000001, wSeconds);

    System.out.println("=== LITTLE'S LAW (IMPLIED) ===");
    System.out.println("Avg RTT ms: " + avgRttMs);
    System.out.println("Implied in-flight L: " + impliedL);
    System.out.println("Predicted throughput msg/s (L/W): " + predictedFromImplied);
    System.out.println("Actual throughput msg/s: " + actualThroughput);

    System.out.println("Collected latency records: " + collector.recordCount());

    CsvWriter.writeLatencyCsv(Path.of("..", "results", "latency.csv"), collector.snapshot());
    System.out.println("Wrote results/latency.csv");

    List<LatencyRecord> snap = collector.snapshot();

    StatisticsCalculator.printStats(snap);
    StatisticsCalculator.printThroughputPerRoom(snap);
    StatisticsCalculator.printMessageTypeDistribution(snap);

    var series = ThroughputTracker.computeMessagesPerSecond(collector.snapshot());
    ChartGenerator.writeThroughputChart(Path.of("..", "results", "throughput.png"), series);
    System.out.println("Wrote results/throughput.png");
  }

  /**
   * Waits for in-flight messages to drain before closing channels.
   *
   * @param collector part 3 collector tracking in-flight messages
   * @param timeoutMs max wait time in milliseconds
   * @throws InterruptedException if interrupted
   */
  private static void awaitLateAcks(Part3Collector collector, long timeoutMs)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      if (collector.inflightCount() == 0) {
        return;
      }
      Thread.sleep(50);
    }
  }
}
