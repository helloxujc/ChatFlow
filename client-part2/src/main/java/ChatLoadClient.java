import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
    String serverBase    = "ws://chatflow-alb-1246938090.us-west-2.elb.amazonaws.com";
    String metricsUrl    = "http://54.213.224.201:8080/metrics";
    String chatPrefix    = serverBase + "/chat/";

    URI probeUri = new URI(chatPrefix + "1");

    int senderThreadsForMain = args.length > 0 ? Integer.parseInt(args[0]) : ClientConfig.MAIN_SENDER_THREADS;
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

    runMainPhaseOnce(new URI(serverBase), metricsUrl, avgRttMs, senderThreadsForMain);
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
  private static void runMainPhaseOnce(URI serverBaseUri, String metricsUrl,
                                        double avgRttMs, int senderThreads) throws Exception {
    int messagesToSend = ClientConfig.TOTAL_MSG;
    int base = messagesToSend / senderThreads;
    int remainder = messagesToSend % senderThreads;

    Metrics metrics = new Metrics();
    Part3Collector collector = new Part3Collector();
    LongAdder success = metrics.success();
    LongAdder failed = metrics.failed();
    AtomicLong seqCounter = new AtomicLong(0);

    String[] pool = new String[ClientConfig.MSG_POOL_SIZE];
    for (int i = 0; i < pool.length; i++) {
      pool[i] = "Message-" + (i + 1);
    }

    String chatPrefix = serverBaseUri.toString() + "/chat/";
    ExecutorService senders = Executors.newFixedThreadPool(senderThreads);

    for (int i = 0; i < senderThreads; i++) {
      int msgsForThread = base + (i < remainder ? 1 : 0);
      long delayMs = (long) i * 2000L / senderThreads; // spread over 2 seconds
      senders.submit(new SenderWorker(
          i + 1, msgsForThread, delayMs, chatPrefix, seqCounter,
          success, failed, collector, metrics, pool));
    }

    Instant start = Instant.now();
    metrics.start();

    senders.shutdown();
    senders.awaitTermination(25, TimeUnit.MINUTES);

    metrics.stop();

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

    System.out.println("\n=== METRICS REPORT ===");
    String metricsJson = fetchMetrics(metricsUrl);
    System.out.println(metricsJson);
    Path metricsFile = Path.of("..", "results", "metrics_result.json");
    Files.writeString(metricsFile, metricsJson);
    System.out.println("Wrote results/metrics_result.json");
  }

  /**
   * Fetches the metrics JSON from the server's /metrics endpoint.
   * Returns an error string instead of throwing if the request fails.
   */
  private static String fetchMetrics(String metricsUrl) {
    try {
      HttpURLConnection conn = (HttpURLConnection) new URL(metricsUrl).openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(5_000);
      conn.setReadTimeout(10_000);

      int code = conn.getResponseCode();
      InputStream stream = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      return "{\"error\": \"Failed to fetch metrics: " + e.getMessage() + "\"}";
    }
  }

}

