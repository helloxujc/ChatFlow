# ChatFlow System Architecture

## Overview

ChatFlow is a distributed real-time chat system built on WebSocket, RabbitMQ, and AWS infrastructure. It supports up to 20 chat rooms and is designed for high-throughput message delivery with horizontal scalability.

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                     Load Test Client                         │
│         Java · 64 sender threads · WebSocket                 │
└─────────────────────────┬────────────────────────────────────┘
                          ▼ ws:// port 80
┌──────────────────────────────────────────────────────────────┐
│          AWS Application Load Balancer (ALB)                 │
└──────┬───────────┬───────────┬───────────┬───────────────────┘ 
       ▼           ▼           ▼           ▼ HTTP:8081
  ┌───────── ┐ ┌───────── ┐ ┌───────── ┐ ┌───────── ┐
  │Server 1  │ │Server 2  │ │Server 3  │ │Server 4  │
  │:8080 HTTP│ │:8080 HTTP│ │:8080 HTTP│ │:8080 HTTP│  health + /rooms
  │:8081 WS  │ │:8081 WS  │ │:8081 WS  │ │:8081 WS  │  client connections
  │:8082 HTTP│ │:8082 HTTP│ │:8082 HTTP│ │:8082 HTTP│  internal broadcast
  └────┬──── ┘ └────┬──── ┘ └────┬──── ┘ └────┬──── ┘
       └─────────── ┴─────────── ┴─────────── ┘
                          │ AMQP
                          ▼
┌──────────────────────────────────────────────────────────────┐
│                    RabbitMQ Broker                           │
└─────────────────────────┬────────────────────────────────────┘
                          ▼  AMQP
┌──────────────────────────────────────────────────────────────┐
│                   Consumer Service (EC2)                     │
│  20 RabbitConsumer threads · StripedExecutor (20 stripes)    │
└─────────────────────────┬────────────────────────────────────┘
                          ▼ HTTP POST /internal/broadcast (parallel)
             (target servers with active connections for room N → WebSocket clients)
```

## Component Descriptions

### Load Test Client
- Multithreaded Java client simulating concurrent users
- Each thread owns one WebSocket connection (1 thread = 1 user)
- Message flow per user: JOIN → TEXT × N → LEAVE
- Collects latency records (send time → ACK time) for analysis

### Application Load Balancer (ALB)
- Distributes WebSocket connections across 4 server instances
- Sticky sessions ensure each user's WebSocket stays on one server
- Health checks monitor server availability every 30 seconds
- Automatically removes unhealthy instances from rotation

### Chat Servers (×4)
- **Port 8080**: HTTP server for health checks (`/health`) and room membership (`/rooms`)
- **Port 8082**: HTTP server for internal broadcast from consumer
- **Port 8081**: Java-WebSocket server handling client connections
- On message receipt: validates → publishes to RabbitMQ → responds OK
- On broadcast receipt: deduplicates (Caffeine cache) → fans out to room WebSocket sessions
- RabbitMQ publishing uses a `ChannelPool` (16 channels) for thread-safe concurrent access

### RabbitMQ Message Broker
- Topic exchange routes messages to per-room queues by routing key `room.{id}`
- 20 durable queues survive broker restarts
- TTL and max-length limits prevent unbounded queue growth under burst load

### Consumer Service
- Runs on a dedicated EC2 instance managed by systemd (auto-restart on failure)
- One `RabbitConsumer` thread per room for isolated queue consumption
- `StripedExecutor` guarantees in-order delivery within each room
- `BroadcastClient` polls each server's `/rooms` endpoint every 500ms and fans out only to servers with active connections for the target room using `CompletableFuture`
- Per-server circuit breaker skips unreachable servers and retries after a cooldown

## Design Patterns

| Pattern | Where Used | Purpose |
|---------|-----------|---------|
| Channel Pool | `ChannelPool` (server) | Thread-safe RabbitMQ channel reuse |
| Circuit Breaker | `CircuitBreaker` (consumer) | Skip unhealthy servers, prevent cascade failures |
| Striped Executor | `StripedExecutor` (consumer) | Ordered per-room message processing |
| Pub-Sub | RabbitMQ topic exchange | Decouple producers from consumers |
| Deduplication Cache | `MessageIdCache` (server) | Idempotent broadcast under at-least-once delivery |

## Message Flow

```
1. Client sends WebSocket message to ALB
2. ALB routes to sticky server (e.g., Server 2)
3. Server validates message, assigns UUID messageId
4. Server borrows RabbitMQ channel from ChannelPool
5. Server publishes QueueMessage to chat.exchange with routing key room.{N}
6. Server returns OK response to client
7. RabbitMQ routes to queue room.{N}
8. Consumer thread for room N receives message
9. StripedExecutor dispatches to stripe thread for room N
10. BroadcastClient POSTs to servers with active connections for room N in parallel
11. Each server checks MessageIdCache (deduplication), broadcasts to WebSocket sessions
12. All clients in room N receive the message
13. Consumer ACKs the message
```

## Infrastructure

| Component | Instance | Public IP | Port |
|-----------|----------|-----------|------|
| Server 1 | chatflow-server | 54.213.224.201 | 8080/8081/8082 |
| Server 2 | chatflow-server-v2-2 | 54.186.75.122 | 8080/8081/8082 |
| Server 3 | chatflow-server-v2-3 | 35.163.56.167 | 8080/8081/8082 |
| Server 4 | chatflow-server-v2-4 | 35.95.35.2 | 8080/8081/8082 |
| RabbitMQ + Consumer | chatflow-rabbitmq | 54.218.236.208 | 5672/15672 |
| ALB | chatflow-alb | (DNS) | 80 |
