# ChatFlow Client - Part 1

Multithreaded WebSocket load-testing client that sends 500,000 messages and reports throughput metrics.

## Threading Model

1. **RTT Probe** — sends 200 probe messages to measure average round-trip time.
2. **Warmup Phase** — 32 threads, each sends 1,000 messages then terminates (32,000 total).
3. **Main Phase** — 1 message-generator thread produces 500K messages into a `BlockingQueue`; 4 sender threads consume and send via
persistent WebSocket connections.

## Message Generation

- `userId`: random 1–100,000
- `username`: derived from userId (e.g. `user12345`)
- `message`: random selection from a pool of 50 pre-defined strings
- `roomId`: random 1–20
- `messageType`: 90% TEXT, 5% JOIN, 5% LEAVE

## Error Handling

- Failed sends are retried up to 5 times with exponential backoff (base 50ms, max 2,000ms).
- Connection drops trigger automatic reconnection.

## Output Metrics

- Successful / failed message counts
- Total wall-clock runtime (ms)
- Overall throughput (msg/s)
- Connection statistics (total connections, reconnections)
- Little's Law prediction vs actual throughput

## Build & Run

```bash
./gradlew run
```

Before running, update the server URL in ChatLoadClient.java:
String baseWsUrl = "ws://<your-ec2-host>:8081/chat/";