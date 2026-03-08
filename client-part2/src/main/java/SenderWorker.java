import java.net.URI;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Simulates a single user: owns one WebSocket connection per room session,
 * sends JOIN -> TEXT x N -> LEAVE, then switches to the next room.
 */
public final class SenderWorker implements Runnable {

  private final int userId;
  private final int totalMessages;
  private final long startDelayMs;
  private final String chatPrefix;
  private final AtomicLong seqCounter;
  private final LongAdder successCount;
  private final LongAdder failureCount;
  private final Part3Collector collector;
  private final Metrics metrics;
  private final String[] messagePool;

  public SenderWorker(
      int userId,
      int totalMessages,
      long startDelayMs,
      String chatPrefix,
      AtomicLong seqCounter,
      LongAdder successCount,
      LongAdder failureCount,
      Part3Collector collector,
      Metrics metrics,
      String[] messagePool) {
    this.userId = userId;
    this.totalMessages = totalMessages;
    this.startDelayMs = startDelayMs;
    this.chatPrefix = chatPrefix;
    this.seqCounter = seqCounter;
    this.successCount = successCount;
    this.failureCount = failureCount;
    this.collector = collector;
    this.metrics = metrics;
    this.messagePool = messagePool;
  }

  @Override
  public void run() {
    if (startDelayMs > 0) {
      try { Thread.sleep(startDelayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
    }
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    int roomId = rng.nextInt(1, ClientConfig.ROOM_ID_MAX + 1);
    URI uri = URI.create(chatPrefix + roomId);
    WsSendChannel channel = new WsSendChannel(uri, metrics,
        collector != null ? collector::onAck : null);

    try {
      channel.connectBlocking(10, TimeUnit.SECONDS);

      // JOIN once at start
      sendOne(channel, MessageType.JOIN, roomId);

      // TEXT for all remaining messages except last
      for (int sent = 1; sent < totalMessages - 1; sent++) {
        sendOne(channel, MessageType.TEXT, roomId);
      }

      // LEAVE at end
      if (totalMessages > 1) {
        sendOne(channel, MessageType.LEAVE, roomId);
      }

      // wait for server to process and ACK all messages (up to 5 min)
      waitForAcks(300_000);

    } catch (Exception e) {
      failureCount.add(totalMessages);
    } finally {
      channel.closeSilently();
    }
  }

  private void sendOne(WsSendChannel channel, MessageType type, int roomId) {
    long seqId = seqCounter.getAndIncrement();
    String text = messagePool[ThreadLocalRandom.current().nextInt(messagePool.length)];
    OutboundMessage msg = new OutboundMessage(
        seqId, userId, "user" + userId, text, roomId, type, Instant.now());

    for (int attempt = 0; attempt < ClientConfig.MAX_SEND_ATTEMPTS; attempt++) {
      try {
        if (!channel.isOpen()) {
          channel.reconnect();
        }
        if (collector != null) {
          collector.onSend(msg);
        }
        channel.send(toJson(msg));
        successCount.increment();
        if (ClientConfig.SEND_DELAY_MS > 0) {
          TimeUnit.MILLISECONDS.sleep(ClientConfig.SEND_DELAY_MS);
        }
        return;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception e) {
        if (attempt < ClientConfig.MAX_SEND_ATTEMPTS - 1) {
          backoffSleep(attempt + 1);
        }
      }
    }
    failureCount.increment();
  }

  private void waitForAcks(long maxMs) {
    if (collector == null) return;
    long deadline = System.currentTimeMillis() + maxMs;
    while (System.currentTimeMillis() < deadline) {
      if (collector.inflightCount() == 0) return;
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
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

  private String escape(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
