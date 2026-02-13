import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Worker that consumes outbound messages from a queue and sends them via a {@link SendChannel}.
 *
 * <p>This worker retries failed sends with exponential backoff. A message is considered successful
 * only when an acknowledgment is received (counted elsewhere by the channel's listener).
 */
public final class SenderWorker implements Runnable {

  private final BlockingQueue<OutboundMessage> queue;
  private final OutboundMessage poisonPill;
  private final SendChannel channel;
  private final LongAdder failureCount;

  /**
   * Creates a sender worker.
   *
   * @param queue shared queue of outbound messages
   * @param poisonPill sentinel message used to stop the worker
   * @param channel channel used to send messages
   * @param failureCount counter incremented when a message exceeds max retries
   */
  public SenderWorker(
      BlockingQueue<OutboundMessage> queue,
      OutboundMessage poisonPill,
      SendChannel channel,
      LongAdder failureCount) {

    this.queue = Objects.requireNonNull(queue, "queue");
    this.poisonPill = Objects.requireNonNull(poisonPill, "poisonPill");
    this.channel = Objects.requireNonNull(channel, "channel");
    this.failureCount = Objects.requireNonNull(failureCount, "failureCount");
  }

  /**
   * Continuously takes messages from the queue and sends them until the poison pill is received.
   */
  @Override
  public void run() {
    try {
      while (true) {
        OutboundMessage msg = queue.take();
        if (msg == poisonPill) {
          break;
        }
        sendWithRetry(msg);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Sends a message with retry and backoff until it succeeds or exceeds the retry limit.
   *
   * @param msg message to send
   */
  private void sendWithRetry(OutboundMessage msg) {
    OutboundMessage current = msg;

    while (true) {
      int attempt = current.getAttempt();

      if (attempt >= ClientConfig.MAX_SEND_ATTEMPTS) {
        failureCount.increment();
        return;
      }

      try {
        ensureOpen();
        channel.send(toJson(current));
        return; // success will be counted in ack listener
      } catch (Exception e) {
        current = current.nextAttempt();
        backoffSleep(current.getAttempt());
      }
    }
  }

  /**
   * Sleeps using exponential backoff based on attempt count.
   *
   * @param attempt current attempt count
   */
  private void backoffSleep(int attempt) {
    long delay = ClientConfig.BACKOFF_BASE_MS * (1L << Math.max(0, attempt - 1));
    delay = Math.min(delay, ClientConfig.BACKOFF_MAX_MS);
    try {
      TimeUnit.MILLISECONDS.sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Ensures the channel is open, reconnecting if necessary.
   *
   * @throws Exception if reconnection fails
   */
  private void ensureOpen() throws Exception {
    if (!channel.isOpen()) {
      channel.reconnect();
    }
  }

  /**
   * Serializes an outbound message into a JSON string expected by the server.
   *
   * @param msg outbound message
   * @return JSON payload string
   */
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

  /**
   * Escapes characters that would break JSON string values.
   *
   * @param msg input string
   * @return escaped string
   */
  private String escape(String msg) {
    return msg.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
