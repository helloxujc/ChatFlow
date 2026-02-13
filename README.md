# ChatFlow                                                                    
A scalable WebSocket-based chat system built for CS6650. The project includes a WebSocket server deployed on AWS EC2 and two load-testing clients that simulate high-volume messaging.

## Project Structure

- ChatFlow/
- ├── server/          # WebSocket server (Java-WebSocket + Jackson)
- ├── client-part1/    # Basic load testing client (500K messages, throughput metrics)
- ├── client-part2/    # Enhanced client with per-message latency tracking & visualization
- └── results/         # Test output: latency.csv, throughput.png, EC2 screenshot

## Prerequisites
- Java 17+
- Gradle 7+

## Quick Start

1. Start the server (or deploy to EC2):
```bash
cd server && ./gradlew run
```

2. Run basic load test:
```bash
cd client-part1 && ./gradlew run
```
3. Run performance analysis:
```
cd client-part2 && ./gradlew run
```

## EC2 Deployment

The server is deployed on an EC2 free-tier instance in us-west-2:
- Health check: http://ec2-54-213-224-201.us-west-2.compute.amazonaws.com:8080/health
- WebSocket: ws://ec2-54-213-224-201.us-west-2.compute.amazonaws.com:8081/chat/{roomId}