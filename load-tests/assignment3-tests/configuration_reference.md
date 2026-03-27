# Configuration Reference — ChatFlow

## 1. Database Connection Settings

| Parameter | Environment variables| Default | Production Value |
|-----------|---------|---------|-----------------|
| JDBC URL | `DB_URL` | `jdbc:postgresql://localhost:5432/chatflow` | `jdbc:postgresql://172.31.27.231:5432/chatflow` |
| Connection pool size | `DB_POOL_SIZE` | `10` | `10` |
| Connection timeout | — | `30,000ms` | `30,000ms` |
| Idle timeout | — | `600,000ms` | `600,000ms` |
| Max connection lifetime | — | `1,800,000ms` | `1,800,000ms` |

---

## 2. Thread Pool Configurations

**Server (server-v2):**

| Parameter | Environment variables | Default | Production Value |
|-----------|---------|---------|-----------------|
| Broadcast thread pool | `BROADCAST_THREADS` | `32` | `32` |
| Health/Metrics HTTP thread pool | — | `4` (fixed) | `4` |
| Broadcast HTTP backlog | — | `128` | `128` |
| RabbitMQ channel pool size | `RABBIT_CHANNEL_POOL` | `16` | `16` |

**Consumer (consumer-v3):**

| Parameter | Environment variables | Default | Production Value |
|-----------|---------|---------|-----------------|
| Striped executor stripes | `STRIPE_COUNT` | `8` | `20` |
| RabbitMQ prefetch count | `PREFETCH_COUNT` | `10` | `10` |
| Total rooms | `TOTAL_ROOMS` | `20` | `20` |

**Client:**

| Parameter | Constant | Value |
|-----------|----------|-------|
| Main sender threads | `MAIN_SENDER_THREADS` | `32` |
| Warmup threads | `WARMUP_THREADS` | `32` |

---

## 3. Batch Processing Parameters

| Parameter | Environment variables | Default | Production Value |
|-----------|---------|---------|-----------------|
| DB batch size | `DB_BATCH_SIZE` | `1000` | `1000` |
| DB flush interval | `DB_FLUSH_MS` | `500ms` | `100ms` |
| DB write queue capacity | — | `100,000` (fixed) | `100,000` |
| RabbitMQ queue max length | — | `100,000` per queue | `100,000` |
| RabbitMQ message TTL | — | `300,000ms` | `300,000ms` |

---

## 4. Circuit Breaker Thresholds

**Broadcast Circuit Breaker:**

| Parameter | Environment variables | Default | Production Value |
|-----------|---------|---------|-----------------|
| Failure threshold | `CB_FAIL_THRESHOLD` | `5` | `5` |
| Open duration | `CB_OPEN_MS` | `20,000ms` | `20,000ms` |

**DB Write Circuit Breaker:**

| Parameter | Environment variables | Default | Production Value |
|-----------|---------|---------|-----------------|
| Failure threshold | `DB_CB_FAIL_THRESHOLD` | `3` | `3` |
| Open duration | `DB_CB_OPEN_MS` | `10,000ms` | `10,000ms` |

