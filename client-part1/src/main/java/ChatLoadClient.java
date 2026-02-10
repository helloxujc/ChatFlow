import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public final class ChatLoadClient {

  private ChatLoadClient() {}

  public static void main(String[] args) throws Exception {
    String baseWsUrl = "ws://127.0.0.1:8081/chat/";
    URI probeUri = new URI(baseWsUrl + "1");

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

    WarmupRunner.WarmupResult warmup = WarmupRunner.run(baseWsUrl);
    printWarmup(warmup);

    runMainPhaseOnce(baseWsUrl, avgRttMs);
  }

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

  private static void runMainPhaseOnce(String baseWsUrl, double avgRttMs) throws Exception {
    int senderThreads = 4;
    int messagesToSend = ClientConfig.TOTAL_MSG;

    Metrics metrics = new Metrics();
    LongAdder success = metrics.success();
    LongAdder failed = metrics.failed();

    BlockingQueue<OutboundMessage> queue =
        new ArrayBlockingQueue<>(ClientConfig.QUEUE_CAPACITY);

    String[] pool = MessageGenerator.defaultMessagePool();

    OutboundMessage poisonPill =
        new OutboundMessage(
            -1L,
            1,
            "poison",
            "poison",
            1,
            MessageType.TEXT,
            Instant.EPOCH);

    ExecutorService senders = Executors.newFixedThreadPool(senderThreads);
    WsSendChannel[] channels = new WsSendChannel[senderThreads];

    URI uri = new URI(baseWsUrl + "1");

    for (int i = 0; i < senderThreads; i++) {
      WsSendChannel channel = new WsSendChannel(uri, metrics);
      channel.connectBlocking(5, TimeUnit.SECONDS);
      channels[i] = channel;
      senders.submit(new SenderWorker(queue, poisonPill, channel, success, failed));
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

    for (WsSendChannel c : channels) {
      if (c != null) {
        c.closeSilently();
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
  }
}
