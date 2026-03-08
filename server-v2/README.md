# ChatFlow Server (v2)

WebSocket chat server with RabbitMQ publishing, internal broadcast endpoint, and message deduplication.

## Endpoints

| Protocol | Port | Path | Description |
|----------|------|------|-------------|
| HTTP GET | 8080 | `/health` | Returns `OK` — used by ALB health check |
| HTTP GET | 8080 | `/rooms` | Returns active room IDs — polled by consumer for smart routing |
| HTTP POST | 8082 | `/internal/broadcast` | Receives broadcast requests from consumer, fans out to WebSocket sessions |
| WebSocket | 8081 | `/chat/{roomId}` | Accepts client connections and chat messages (roomId: 1–20) |

## Message Format (client → server)

```json
{
  "userId": "12345",
  "username": "user12345",
  "message": "Hello world",
  "timestamp": "2026-02-13T10:00:00Z",
  "messageType": "TEXT"
}
```

`messageType`: `TEXT`, `JOIN`, or `LEAVE`

## Validation Rules

- `userId`: integer string, 1–100,000
- `username`: 3–20 alphanumeric characters
- `message`: 1–500 characters
- `timestamp`: valid ISO-8601 instant
- `messageType`: one of `TEXT`, `JOIN`, `LEAVE`
- `roomId` (path): 1–20

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `RABBIT_HOST` | localhost | RabbitMQ host |
| `RABBIT_PORT` | 5672 | RabbitMQ port |
| `RABBIT_USER` | guest | RabbitMQ username |
| `RABBIT_PASS` | guest | RabbitMQ password |
| `RABBIT_EXCHANGE` | chat.exchange | Topic exchange name |
| `RABBIT_CHANNEL_POOL` | 16 | Number of pre-created RabbitMQ channels |
| `CHATFLOW_SERVER_ID` | server-1 | Server identifier logged on connections |
| `BROADCAST_THREADS` | 32 | Threads for handling incoming broadcast requests |

## Build & Deploy

```bash
# Build fat jar
./gradlew jar

# Deploy to EC2
scp -i ~/Downloads/chatflow-key.pem build/libs/server-1.0-SNAPSHOT.tar ubuntu@<ip>:~
ssh -i ~/Downloads/chatflow-key.pem ubuntu@<ip> \
  "nohup bash -c 'ulimit -n 65536; exec env RABBIT_HOST=<host> RABBIT_USER=chatflow RABBIT_PASS=chatflow123 BROADCAST_THREADS=32 \
   java -Xmx700m -classpath /home/ubuntu/server-1.0-SNAPSHOT/lib/\* chatflow.server.ServerMain' \
   > ~/server.log 2>&1 &"
```

Or use the deployment script:
```bash
../deployment/server-start.sh
```

## Tech Stack

- Java-WebSocket 1.5.4
- Jackson 2.17.0
- Caffeine 3.1.8 (message deduplication cache)
- RabbitMQ Java Client 5.20.0
- JDK `com.sun.net.httpserver` (health check, /rooms, and broadcast endpoint)
