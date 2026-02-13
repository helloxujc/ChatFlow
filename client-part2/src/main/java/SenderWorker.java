import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Consumes outbound messages from a shared queue and sends them over WebSocket.
 *
 * <p>This worker selects the appropriate pooled WebSocket channel based on the
 * message room id, ensuring messages are routed to /chat/{roomId} endpoints.
 */
public final class SenderWorker implements Runnable {

  private final BlockingQueue<OutboundMessage> queue;
  private final OutboundMessage poisonPill;
  private final RoomChannelPool pool;
  private final LongAdder successCount;
  private final LongAdder failureCount;
  private final Part3Collector collector;

  /**
   * Creates a sender worker without a Part 3 collector.
   *
   * @param queue shared outbound message queue
   * @param poisonPill sentinel message indicating termination
   * @param pool room-based channel pool
   * @param successCount counter for successful sends
   * @param failureCount counter for failed sends
   */
  public SenderWorker(
      BlockingQueue<OutboundMessage> queue,
      OutboundMessage poisonPill,
      RoomChannelPool pool,
      LongAdder successCount,
      LongAdder failureCount) {
    this(queue, poisonPill, pool, successCount, failureCount, null);
  }

  /**
   * Creates a sender worker.
   *
   * @param queue shared outbound message queue
   * @param poisonPill sentinel message indicating termination
   * @param pool room-based channel pool
   * @param successCount counter for successful sends
   * @param failureCount counter for failed sends
   * @param collector optional Part 3 collector
   */
  public SenderWorker(
      BlockingQueue<OutboundMessage> queue,
      OutboundMessage poisonPill,
      RoomChannelPool pool,
      LongAdder successCount,
      LongAdder failureCount,
      Part3Collector collector) {
    this.queue = Objects.requireNonNull(queue, "queue");
    this.poisonPill = Objects.requireNonNull(poisonPill, "poisonPill");
    this.pool = Objects.requireNonNull(pool, "pool");
    this.successCount = Objects.requireNonNull(successCount, "successCount");
    this.failureCount = Objects.requireNonNull(failureCount, "failureCount");
    this.collector = collector;
  }

  /** Runs the send loop until a poison pill is received or the thread is interrupted. */
  @Override
  public void run() {
    try {
      while (true) {
        OutboundMessage msg = queue.take();
        if (msg == poisonPill || msg.getSeqId() == poisonPill.getSeqId()) {
          break;
        }
        sendWithRetry(msg);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void sendWithRetry(OutboundMessage msg) {
    OutboundMessage current = msg;

    while (true) {
      int attempt = current.getAttempt();
      if (attempt >= ClientConfig.MAX_SEND_ATTEMPTS) {
        failureCount.increment();
        return;
      }

      try {
        SendChannel channel = pool.channel(current.getRoomId());
        ensureOpen(channel);
        if (collector != null) {
          collector.onSend(current);
        }
        channel.send(toJson(current));
        successCount.increment();
        TimeUnit.MICROSECONDS.sleep(500);
        return;
      } catch (Exception e) {
        current = current.nextAttempt();
        backoffSleep(current.getAttempt());
      }
    }
  }

  private void backoffSleep(int attempt) {
    long delay = ClientConfig.BACKOFF_BASE_MS * (1L << Math.max(0, attempt - 1));
    delay = Math.min(delay, ClientConfig.BACKOFF_MAX_MS);
    try {
      TimeUnit.MILLISECONDS.sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void ensureOpen(SendChannel channel) throws Exception {
    if (!channel.isOpen()) {
      channel.reconnect();
    }
  }

  private String toJson(OutboundMessage msg) {
    String msgWithSeq = "seq:" + msg.getSeqId() + "|" + msg.getMessage();
    return "{"
        + "\"userId\":\"" + msg.getUserId() + "\","
        + "\"username\":\"" + escape(msg.getUsername()) + "\","
        + "\"message\":\"" + escape(msgWithSeq) + "\","
        + "\"timestamp\":\"" + msg.getTimestamp().toString() + "\","
        + "\"messageType\":\"" + msg.getMessageType().name() + "\""
        + "}";
  }

  private String escape(String msg) {
    return msg.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}

