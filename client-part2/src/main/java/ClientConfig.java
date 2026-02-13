/**
 * Configuration constants used by the chat load client.
 *
 * <p>This class defines workload size, warmup parameters, retry policy,
 * ID limits, message pool size, and queue capacity.
 */
public final class ClientConfig {

  /** Total number of messages to send during the main phase. */
  public static final int TOTAL_MSG = 500_000;

  /** Number of threads used in the warmup phase. */
  public static final int WARMUP_THREADS = 32;

  /** Number of messages each warmup thread sends. */
  public static final int WARMUP_MSG_PER_THREAD = 1_000;

  /** Maximum number of retry attempts for a failed send. */
  public static final int MAX_SEND_ATTEMPTS = 5;

  /** Base backoff delay in milliseconds for retry logic. */
  public static final long BACKOFF_BASE_MS = 50L;

  /** Maximum backoff delay in milliseconds. */
  public static final long BACKOFF_MAX_MS = 2_000L;

  /** Maximum room ID value supported by the client. */
  public static final int ROOM_ID_MAX = 20;

  /** Maximum user ID value used for random generation. */
  public static final int USER_ID_MAX = 100_000;

  /** Size of the predefined message pool. */
  public static final int MSG_POOL_SIZE = 50;

  /** Capacity of the outbound message blocking queue. */
  public static final int QUEUE_CAPACITY = 20_000;

  /**
   * Default constructor.
   */
  public ClientConfig() {}
}
