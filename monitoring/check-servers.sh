#!/bin/bash
# Check status of all ChatFlow servers and consumer

KEY=~/Downloads/chatflow-key.pem
SERVERS=(54.213.224.201 54.186.75.122 35.163.56.167 35.95.35.2)
CONSUMER_IP=54.218.236.208

echo "=== SERVER STATUS ==="
for ip in "${SERVERS[@]}"; do
  echo -n "$ip | process: "
  ssh -i $KEY -o StrictHostKeyChecking=no -o ConnectTimeout=5 ubuntu@$ip \
    "pgrep -f chatflow.server.ServerMain > /dev/null && echo -n 'UP' || echo -n 'DOWN'
     echo -n ' | ws_conns: '
     ss -tn | grep ':8081' | grep ESTAB | wc -l" 2>/dev/null
done

echo ""
echo "=== CONSUMER STATUS ==="
ssh -i $KEY -o StrictHostKeyChecking=no ubuntu@$CONSUMER_IP \
  "sudo systemctl status chatflow-consumer --no-pager | grep -E 'Active|Main PID'" 2>/dev/null

echo ""
echo "=== ALB HEALTH ==="
curl -s -o /dev/null -w "ALB /health: %{http_code}\n" \
  --max-time 5 "http://chatflow-alb-1246938090.us-west-2.elb.amazonaws.com/health"
