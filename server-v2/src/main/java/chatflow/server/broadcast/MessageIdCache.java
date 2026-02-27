package chatflow.server.broadcast;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

public class MessageIdCache {

  private final Cache<String, Boolean> cache;

  public MessageIdCache() {
    this.cache = Caffeine.newBuilder()
        .maximumSize(50_000).expireAfterWrite(60, TimeUnit.SECONDS).build();
  }

  public boolean seen(String messageId) {
    if (cache.getIfPresent(messageId) != null) {
      return true;
    }
    cache.put(messageId, true);
    return false;
  }
}
