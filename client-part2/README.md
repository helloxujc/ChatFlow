# ChatFlow Client - Part 2

Enhanced load-testing client with per-message latency tracking, statistical analysis, and throughput visualization.

## Enhancements over Part 1

- **RoomChannelPool**: maintains one WebSocket connection per room (20 rooms), routing messages by `roomId`.
- **Part3Collector**: correlates each outgoing message with its ACK to compute end-to-end latency.
- **Late-ACK draining**: waits up to 2 minutes for in-flight messages before shutdown.

## Output

### CSV — `results/latency.csv`

timestamp,messageType,latency,statusCode,roomId

One row per acknowledged message (~500K rows).

### Statistics (printed to console)

- Mean, Median, P95, P99, Min, Max response time (ms)
- Throughput per room (msg/s)
- Message type distribution (count and %)

### Chart — `results/throughput.png`

Line chart of throughput over time in 10-second buckets.

## Build & Run

```bash
./gradlew run
```

Before running, update the server URL in ChatLoadClient.java:
String serverBase = "ws://<your-ec2-host>:8081";