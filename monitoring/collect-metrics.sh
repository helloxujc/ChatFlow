#!/bin/bash
# Monitor active WebSocket connections per server during load test
# Run this in a separate terminal while ./gradlew run --args="N" is running

KEY=~/Downloads/chatflow-key.pem
SERVERS=(54.213.224.201 54.186.75.122 35.163.56.167 35.95.35.2)
INTERVAL=${1:-5}  # sampling interval in seconds, default 5s

echo "Monitoring connection distribution (Ctrl+C to stop)..."
echo "Interval: ${INTERVAL}s"
echo ""

while true; do
  echo -n "$(date +%H:%M:%S) | "
  total=0
  for ip in "${SERVERS[@]}"; do
    count=$(ssh -i $KEY -o StrictHostKeyChecking=no -o ConnectTimeout=3 ubuntu@$ip \
      "ss -tn | grep ':8081' | grep ESTAB | wc -l" 2>/dev/null)
    count=${count:-0}
    total=$((total + count))
    echo -n "$ip: $count | "
  done
  echo "total: $total"
  sleep $INTERVAL
done
