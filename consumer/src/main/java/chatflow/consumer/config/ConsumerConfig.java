package chatflow.consumer.config;

/**
 * Singleton that loads all consumer configuration values from environment variables with defaults.
 */
public class ConsumerConfig {

  public final String rabbitHost;
  public final int rabbitPort;
  public final String rabbitUser;
  public final String rabbitPass;
  public final String rabbitExchange;
  public final Integer totalRooms;
  public final String[] serverUrls;
  public final Integer stripeCount;
  public final Integer prefetchCount;
  public final Integer cbFailThreshold;
  public final long cbOpenMs;

  private static final ConsumerConfig INSTANCE = new ConsumerConfig();

  private ConsumerConfig() {
    this.rabbitHost = env("RABBIT_HOST","localhost");
    this.rabbitPort = Integer.parseInt(env("RABBIT_PORT","5672"));
    this.rabbitUser = env("RABBIT_USER","guest");
    this.rabbitPass = env("RABBIT_PASS","guest");
    this.rabbitExchange = env("RABBIT_EXCHANGE","chat.exchange");
    this.totalRooms = Integer.parseInt(env("TOTAL_ROOMS","20"));
    this.serverUrls = env("SERVER_URLS","http://localhost:8080").split(",");
    this.stripeCount = Integer.parseInt(env("STRIPE_COUNT","8"));
    this.prefetchCount = Integer.parseInt(env("PREFETCH_COUNT","10"));
    this.cbFailThreshold = Integer.parseInt(env("CB_FAIL_THRESHOLD","5"));
    this.cbOpenMs = Long.parseLong(env("CB_OPEN_MS","20000"));
  }

  private static String env(String key, String defaultValue) {
    String value = System.getenv(key);
    return (value != null && !value.isBlank()) ? value : defaultValue;
  }

  /**
   * Returns the singleton instance of the configuration.
   *
   * @return the global {@code ConsumerConfig} instance
   */
  public static ConsumerConfig get() {
    return INSTANCE;
  }

}
