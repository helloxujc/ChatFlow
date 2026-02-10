public final class ClientConfig {

  public static final int TOTAL_MSG = 500_000;

  public static final int WARMUP_THREADS = 32;

  public static final int WARMUP_MSG_PER_THREAD = 1_000;

  public static final int MAX_SEND_ATTEMPTS = 5;

  public static final long BACKOFF_BASE_MS = 50L;

  public static final long BACKOFF_MAX_MS = 2_000L;

  public static final int ROOM_ID_MAX = 20;

  public static final int USER_ID_MAX = 100_000;

  public static final int MSG_POOL_SIZE = 50;

  public static final int QUEUE_CAPACITY = 20_000;

  public ClientConfig() {}
}
