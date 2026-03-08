# ALB Configuration

## Load Balancer
- Name: chatflow-alb
- Type: Application Load Balancer
- DNS: chatflow-alb-1246938090.us-west-2.elb.amazonaws.com
- Region: us-west-2

## Listener
- Port: 80 (HTTP)
- Protocol: HTTP
- Supports WebSocket upgrade 

## Target Group: chatflow-servers
- Target type: Instance
- Protocol: HTTP
- Traffic port: 8081 (WebSocket)
- Health check port: 8080 (HTTP /health)
- Health check path: /health
- Health check interval: 30s
- Healthy threshold: 2 consecutive successes
- Unhealthy threshold: 3 consecutive failures

## Stickiness
- Type: Load balancer generated cookie (AWSALB)
- Duration: 1 day
- Required for WebSocket: clients must reconnect to same server

## Idle Timeout
- ALB idle timeout: 300 seconds
- WebSocket server connection lost timeout: 300 seconds

## Security Groups
- ALB SG (chatflow-alb-sg): inbound TCP 80 from 0.0.0.0/0
- EC2 SG: inbound TCP 8080 from ALB SG (health check + /rooms)
- EC2 SG: inbound TCP 8081 from ALB SG (WebSocket traffic)
- EC2 SG: inbound TCP 8082 from Consumer SG (internal broadcast)

## Backend Instances
| Instance | Public IP | Private IP | Port |
|----------|-----------|------------|------|
| chatflow-server | 54.213.224.201 | 172.31.27.59 | 8081 |
| chatflow-server-v2-2 | 54.186.75.122 | 172.31.30.111 | 8081 |
| chatflow-server-v2-3 | 35.163.56.167 | 172.31.30.43 | 8081 |
| chatflow-server-v2-4 | 35.95.35.2 | 172.31.27.211 | 8081 |
