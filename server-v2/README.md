# ChatFlow Server

WebSocket chat server with message validation and echo-back functionality.

## Endpoints

| Protocol  | Path             | Description                                      |
|-----------|------------------|--------------------------------------------------|
| HTTP GET  | `/health`        | Returns `OK` (port 8080)                         |
| WebSocket | `/chat/{roomId}` | Accepts chat messages, validates, echoes (port 8081) |

## Message Format

```json
{
"userId": "12345",
"username": "user12345",
"message": "Hello world",
"timestamp": "2026-02-13T10:00:00Z",
"messageType": "TEXT"
}
```

## Validation Rules
- userId: integer string, 1–100,000
- username: 3–20 alphanumeric characters
- message: 1–500 characters
- timestamp: valid ISO-8601 instant
- messageType: one of TEXT, JOIN, LEAVE
- roomId (path param): 1–20

## Build & Run
```bash
./gradlew run
```
The server starts an HTTP health-check endpoint on port 8080 and a WebSocket server on port 8081.

## EC2 Deployment
```bash
./gradlew build
scp build/distributions/server-1.0-SNAPSHOT.tar <ec2-host>:~
ssh <ec2-host> "tar xf server-1.0-SNAPSHOT.tar && server-1.0-SNAPSHOT/bin/server"
```

## Tech Stack
- Java-WebSocket 1.5.4
- Jackson 2.17.0
- JDK built-in com.sun.net.httpserver for /health