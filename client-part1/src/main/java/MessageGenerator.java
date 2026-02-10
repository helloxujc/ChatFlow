import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public final class MessageGenerator implements Runnable {

  private final BlockingQueue<OutboundMessage> queue;
  private final int totalMessages;
  private final int poisonPillCount;
  private final OutboundMessage poisonPill;
  private final String[] messagePool;


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
   * Runs this operation.
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
    } catch (InterruptedException e){
      Thread.currentThread().interrupt();
    }
  }

  private OutboundMessage generateOne(long seqId) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    int userId = random.nextInt(1, ClientConfig.USER_ID_MAX + 1);
    String username = "user" + userId;

    int roomId = random.nextInt(1, ClientConfig.ROOM_ID_MAX + 1);

    String text = messagePool[random.nextInt( messagePool.length)];

    MessageType type = MessageType.randomType();

    return new OutboundMessage(seqId,userId,username,text,roomId,type,Instant.now());
  }

  public static String[] defaultMessagePool() {
    String[] pool = new String[ClientConfig.MSG_POOL_SIZE];
    for (int i = 0; i < pool.length; i++) {
      pool[i] = "Message-" + (i + 1);
    }
    return pool;
  }
}
