/**
 * Central configuration constants for the chat load client.
 *
 * <p>This class defines message counts, warmup settings, retry policies,
 * queue capacity, and limits used during load testing.
 */
public final class ClientConfig {

  /** Total number of messages to send in the main phase. */
  public static final int TOTAL_MSG = 500_000;

  /** Number of threads used during the warmup phase. */
  public static final int WARMUP_THREADS = 32;

  /** Number of messages sent per thread during warmup. */
  public static final int WARMUP_MSG_PER_THREAD = 1_000;

  /** Maximum number of send retry attempts for a failed message. */
  public static final int MAX_SEND_ATTEMPTS = 5;

  /** Base delay in milliseconds for exponential backoff. */
  public static final long BACKOFF_BASE_MS = 50L;

  /** Maximum backoff delay in milliseconds. */
  public static final long BACKOFF_MAX_MS = 2_000L;

  /** Maximum room ID value. */
  public static final int ROOM_ID_MAX = 20;

  /** Maximum user ID value. */
  public static final int USER_ID_MAX = 100_000;

  /** Number of predefined messages in the message pool. */
  public static final int MSG_POOL_SIZE = 50;

  /** Capacity of the outbound message queue. */
  public static final int QUEUE_CAPACITY = 20_000;

  /**
   * Default constructor.
   */
  public ClientConfig() {}
}
