package chatflow.consumer.db;

import chatflow.consumer.model.BroadcastRequest;
import chatflow.consumer.notifier.CircuitBreaker;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Background thread that drains the in-memory DB queue in batches and persists them to PostgreSQL.
 *
 * Flush is triggered when either:
 *   - the batch reaches dbBatchSize, or
 *   - dbFlushMs milliseconds pass with no new messages (poll timeout)
 *
 * Failed batches are retried up to 3 times with exponential backoff (100ms, 300ms, 1000ms).
 * Batches that still fail after all retries are written to failed-db-writes.log and the
 * circuit breaker is notified.
 */
public class DbWriter implements Runnable {

  private final LinkedBlockingQueue<BroadcastRequest> queue;
  private final MessageRepository repo;
  private final CircuitBreaker circuitBreaker;
  private final int batchSize;
  private final long flushMs;

  // Dead letter: in-memory list + log file for durability across restarts
  private final LinkedBlockingQueue<BroadcastRequest> deadLetterQueue = new LinkedBlockingQueue<>();
  private static final String DLQ_LOG = "failed-db-writes.log";

  // Backoff delays in ms for retry attempts 1, 2, 3
  private static final long[] BACKOFF_MS = {100, 300, 1000};

  private volatile boolean running = true;

  public DbWriter(LinkedBlockingQueue<BroadcastRequest> queue,
                  MessageRepository repo,
                  CircuitBreaker circuitBreaker,
                  int batchSize,
                  long flushMs) {
    this.queue = queue;
    this.repo = repo;
    this.circuitBreaker = circuitBreaker;
    this.batchSize = batchSize;
    this.flushMs = flushMs;
  }

  @Override
  public void run() {
    List<BroadcastRequest> batch = new ArrayList<>(batchSize);

    while (running) {
      try {
        // Block until the first message arrives or flushMs elapses
        BroadcastRequest first = queue.poll(flushMs, TimeUnit.MILLISECONDS);

        if (first == null) {
          // Timeout with no messages — nothing to flush
          continue;
        }

        batch.add(first);
        // Collect as many more as are already waiting, up to batchSize
        queue.drainTo(batch, batchSize - 1);

        flush(batch);
        batch.clear();

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    // Flush remaining messages on shutdown
    queue.drainTo(batch);
    if (!batch.isEmpty()) {
      flush(batch);
    }
  }

  /**
   * Attempts to persist the batch with exponential backoff retries.
   * Falls back to dead letter queue after all retries are exhausted.
   */
  private void flush(List<BroadcastRequest> batch) {
    if (!circuitBreaker.allowRequest()) {
      sendToDeadLetter(batch, "circuit breaker open");
      return;
    }

    for (int attempt = 0; attempt <= BACKOFF_MS.length; attempt++) {
      try {
        repo.persistBatch(batch);
        circuitBreaker.onSuccess();
        return; // success — done
      } catch (SQLException e) {
        boolean isLastAttempt = (attempt == BACKOFF_MS.length);
        if (isLastAttempt) {
          circuitBreaker.onFailure();
          sendToDeadLetter(batch, e.getMessage());
          return;
        }
        // Wait before retrying
        try {
          Thread.sleep(BACKOFF_MS[attempt]);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          sendToDeadLetter(batch, "interrupted during backoff");
          return;
        }
      }
    }
  }

  /**
   * Adds failed messages to the in-memory dead letter queue and appends
   * them to failed-db-writes.log so they survive a process restart.
   */
  private void sendToDeadLetter(List<BroadcastRequest> batch, String reason) {
    deadLetterQueue.addAll(batch);

    try (PrintWriter writer = new PrintWriter(new FileWriter(DLQ_LOG, true))) {
      for (BroadcastRequest msg : batch) {
        writer.printf("messageId=%s roomId=%s userId=%s reason=%s%n",
            msg.getMessageId(), msg.getRoomId(), msg.getUserId(), reason);
      }
    } catch (IOException e) {
      System.err.println("[DbWriter] Failed to write dead letter log: " + e.getMessage());
    }

    System.err.printf("[DbWriter] %d messages sent to dead letter queue. Reason: %s%n",
        batch.size(), reason);
  }

  /**
   * Signals the writer to stop after flushing remaining messages.
   * Call this from the shutdown hook, then join the thread.
   */
  public void stop() {
    running = false;
  }

  public int getDeadLetterCount() {
    return deadLetterQueue.size();
  }
}
