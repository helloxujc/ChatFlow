import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates outbound chat messages and places them into a shared queue.
 *
 * <p>This generator produces a fixed number of messages with randomized user IDs,
 * room IDs, message content, and message types. After producing all messages,
 * it inserts poison pills to signal sender threads to terminate.
 */
public final class MessageGenerator implements Runnable {

  private final BlockingQueue<OutboundMessage> queue;
  private final int totalMessages;
  private final int poisonPillCount;
  private final OutboundMessage poisonPill;
  private final String[] messagePool;

  /**
   * Creates a message generator.
   *
   * @param queue shared blocking queue for outbound messages
   * @param totalMessages total number of messages to generate
   * @param poisonPillCount number of poison pills to insert after generation
   * @param poisonPill special message signaling termination
   * @param messagePool predefined pool of message texts
   */
  public MessageGenerator(
      BlockingQueue<OutboundMessage> queue,
      int totalMessages,
      int poisonPillCount,
      OutboundMessage poisonPill,
      String[] messagePool) {

    this.queue = Objects.requireNonNull(queue, "queue");
    this.totalMessages = totalMessages;
    this.poisonPillCount = poisonPillCount;
    this.poisonPill = Objects.requireNonNull(poisonPill, "poisonPill");
    this.messagePool = Objects.requireNonNull(messagePool, "messagePool");
  }

  /**
   * Generates messages and inserts them into the queue, followed by poison pills.
   */
  @Override
  public void run() {
    try {
      for (int i = 0; i < totalMessages; i++) {
        OutboundMessage msg = generateOne(i);
        queue.put(msg);
      }
      for (int i = 0; i < poisonPillCount; i++) {
        queue.put(poisonPill);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Generates a single outbound message with randomized fields.
   *
   * @param seqId sequence ID of the message
   * @return generated outbound message
   */
  private OutboundMessage generateOne(long seqId) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    int userId = random.nextInt(1, ClientConfig.USER_ID_MAX + 1);
    String username = "user" + userId;

    int roomId = random.nextInt(1, ClientConfig.ROOM_ID_MAX + 1);

    String text = messagePool[random.nextInt(messagePool.length)];

    MessageType type = MessageType.randomType();

    return new OutboundMessage(seqId, userId, username, text, roomId, type, Instant.now());
  }

  /**
   * Creates a default pool of predefined message strings.
   *
   * @return array of default message texts
   */
  public static String[] defaultMessagePool() {
    String[] pool = new String[ClientConfig.MSG_POOL_SIZE];
    for (int i = 0; i < pool.length; i++) {
      pool[i] = "Message-" + (i + 1);
    }
    return pool;
  }
}
