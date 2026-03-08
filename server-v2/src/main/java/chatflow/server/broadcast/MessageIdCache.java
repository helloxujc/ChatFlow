package chatflow.server.broadcast;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache used to deduplicate broadcast messages by tracking recently seen message IDs.
 */
public class MessageIdCache {

  private final Cache<String, Boolean> cache;

  public MessageIdCache() {
    this.cache = Caffeine.newBuilder()
        .maximumSize(50_000).expireAfterWrite(60, TimeUnit.SECONDS).build();
  }

  /**
   * Returns {@code true} if the message ID has already been seen, or {@code false} and records it if this is the first occurrence.
   *
   * @param messageId the unique ID of the message to check
   * @return {@code true} if the message is a duplicate, {@code false} if it is new
   */
  public boolean seen(String messageId) {
    if (cache.getIfPresent(messageId) != null) {
      return true;
    }
    cache.put(messageId, true);
    return false;
  }
}
