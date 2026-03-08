# ChatFlow Client - Part 2

Enhanced load-testing client with per-user WebSocket connections, latency tracking, statistical analysis, and throughput visualization.

## Design

Each sender thread simulates one user with its own WebSocket connection:
- Connects to a random room (1–20) via ALB
- Sends: `JOIN` → `TEXT × N` → `LEAVE`
- Waits for all server ACKs before closing

## Usage

```bash
# Run with N sender threads (default: 4)
./gradlew run --args="128"
```

## Phases

| Phase | Description |
|-------|-------------|
| RTT Probe | Measures average round-trip time, estimates max throughput via Little's Law |
| Warmup | 32 threads × 1,000 messages to warm up server JIT and connections |
| Main | Full workload (500K messages) across N sender threads |

## Configuration (`ClientConfig.java`)

| Constant | Default | Description |
|----------|---------|-------------|
| `TOTAL_MSG` | 500,000 | Total messages to send |
| `WARMUP_THREADS` | 32 | Warmup thread count |
| `WARMUP_MSG_PER_THREAD` | 1,000 | Messages per warmup thread |
| `MAX_SEND_ATTEMPTS` | 5 | Retry attempts per message |
| `ROOM_ID_MAX` | 20 | Number of rooms |
| `MSG_POOL_SIZE` | 50 | Reusable message pool size |

## Output

### Console
```
=== LITTLE'S LAW (PROBE) ===
=== WARMUP ===
=== MAIN ===
=== LITTLE'S LAW (IMPLIED) ===
=== LATENCY STATS ===       Mean / Median / P95 / P99 / Min / Max (ms)
=== THROUGHPUT PER ROOM ===  msg/s per room
=== MESSAGE TYPE DISTRIBUTION ===
```

### Files
- `results/latency.csv` — one row per ACKed message: `timestamp, messageType, latency, statusCode, roomId`
- `results/throughput.png` — line chart of throughput over time (10-second buckets)

## Server Target

```
ws://chatflow-alb-1246938090.us-west-2.elb.amazonaws.com/chat/{roomId}
```

To point at a different server, update `serverBase` in `ChatLoadClient.java`.
