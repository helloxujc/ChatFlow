import java.io.ObjectStreamException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public final class SenderWorker implements Runnable {

  private final BlockingQueue<OutboundMessage> queue;
  private final OutboundMessage poisonPill;
  private final SendChannel channel;
  private final LongAdder successCount;
  private final LongAdder failureCount;

  public SenderWorker(BlockingQueue<OutboundMessage> queue, OutboundMessage poisonPill,
      SendChannel channel, LongAdder successCount, LongAdder failureCount) {
    this.queue = Objects.requireNonNull(queue, "queue");
    this.poisonPill = Objects.requireNonNull(poisonPill, "poisonPill");
    this.channel = Objects.requireNonNull(channel, "channel");
    this.successCount = Objects.requireNonNull(successCount, "successCount");
    this.failureCount = Objects.requireNonNull(failureCount, "failureCount");
  }


  /**
   * Runs this operation.
   */
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
    }catch (InterruptedException e){
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
        ensureOpen();
        channel.send(toJson(current));
        successCount.increment();
        return;
      } catch (Exception e){
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

  private void ensureOpen() throws Exception {
    if (!channel.isOpen()) {
      channel.reconnect();
    }
  }

  private String toJson(OutboundMessage msg) {
    return "{"
        + "\"userId\":\"" + msg.getUserId() + "\","
        + "\"username\":\"" + escape(msg.getUsername()) + "\","
        + "\"message\":\"" + escape(msg.getMessage()) + "\","
        + "\"timestamp\":\"" + msg.getTimestamp().toString() + "\","
        + "\"messageType\":\"" + msg.getMessageType().name() + "\""
        + "}";
  }

  private String escape(String msg) {
    return msg.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
