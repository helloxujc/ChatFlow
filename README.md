# ChatFlow

A scalable distributed real-time chat system built for CS6650. Supports 20 chat rooms with WebSocket connections, RabbitMQ message queuing, and a multi-server AWS deployment behind an Application Load Balancer.

## Project Structure

```
ChatFlow/
├── server-v2/        # Chat server (WebSocket + RabbitMQ publisher + broadcast receiver)
├── consumer/         # Message consumer (RabbitMQ → smart broadcast to servers with active room connections)
├── client-part1/     # Basic load testing client (500K messages, throughput metrics)
├── client-part2/     # Enhanced client (per-user connections, latency tracking, visualization)
├── deployment/       # ALB config, server/consumer startup scripts, systemd service
├── monitoring/       # Connection distribution, RabbitMQ stats, and health check scripts
└── results/          # Test output: latency.csv, throughput.png
```

## Architecture

```
Client (64–512 threads)
    ↓ ws:// port 80
ALB (sticky session, 300s idle timeout)
    ↓ HTTP:8081
[Server1, Server2, Server3, Server4]  (port 8080 HTTP + 8081 WebSocket + 8082 broadcast)
    ↓ AMQP — topic exchange: chat.exchange — routing key: room.{1..20}
RabbitMQ (20 durable queues, TTL 60s, max 10k messages)
    ↓
Consumer (20 threads + StripedExecutor + parallel broadcast)
    ↓ HTTP POST /internal/broadcast
[Server1, Server2, Server3, Server4]  → WebSocket clients
```

See [deployment/architecture.md](deployment/architecture.md) for full architecture documentation.

## Prerequisites

- Java 21+
- Gradle 8+
- AWS EC2 instances (5 total: 4 servers + 1 RabbitMQ/consumer)

## Quick Start

### 1. Deploy servers (4 EC2 instances)

```bash
cd deployment
./server-start.sh
```

### 2. Deploy consumer

```bash
cd deployment
./consumer-start.sh 20   # 20 = STRIPE_COUNT
```

### 3. Run load test

```bash
cd client-part2
./gradlew run --args="128"   # 128 = sender thread count
```

### 4. Monitor during test

```bash
# Terminal 2 — connection distribution across servers
./monitoring/collect-metrics.sh

# Terminal 3 — RabbitMQ queue depth and rates
./monitoring/rabbitmq-stats.sh
```

## Infrastructure

| Component | Public IP | Ports |
|-----------|-----------|-------|
| Server 1 | 54.213.224.201 | 8080 (HTTP), 8081 (WS), 8082 (broadcast) |
| Server 2 | 54.186.75.122 | 8080 (HTTP), 8081 (WS), 8082 (broadcast) |
| Server 3 | 35.163.56.167 | 8080 (HTTP), 8081 (WS), 8082 (broadcast) |
| Server 4 | 35.95.35.2 | 8080 (HTTP), 8081 (WS), 8082 (broadcast) |
| RabbitMQ + Consumer | 54.218.236.208 | 5672 (AMQP), 15672 (Management UI) |
| ALB | chatflow-alb-1246938090.us-west-2.elb.amazonaws.com | 80 |

