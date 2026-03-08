#!/bin/bash
# Poll RabbitMQ Management API and print queue metrics
# Run this in a separate terminal during load tests

RABBIT_URL="http://54.218.236.208:15672/api/overview"
AUTH="chatflow:chatflow123"
INTERVAL=${1:-2}  # polling interval in seconds, default 2s

echo "Polling RabbitMQ metrics (Ctrl+C to stop)..."
echo "Interval: ${INTERVAL}s"
echo ""
printf "%-10s %-15s %-15s %-15s %-15s\n" "Time" "QueueDepth" "Unacked" "PublishRate" "DeliverRate"
echo "------------------------------------------------------------------------"

while true; do
  response=$(curl -s -u $AUTH --max-time 3 "$RABBIT_URL" 2>/dev/null)
  if [ -n "$response" ]; then
    depth=$(echo $response | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('queue_totals',{}).get('messages_ready',0))" 2>/dev/null)
    unacked=$(echo $response | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('queue_totals',{}).get('messages_unacknowledged',0))" 2>/dev/null)
    pub=$(echo $response | python3 -c "import sys,json; d=json.load(sys.stdin); print(round(d.get('message_stats',{}).get('publish_details',{}).get('rate',0),1))" 2>/dev/null)
    del=$(echo $response | python3 -c "import sys,json; d=json.load(sys.stdin); print(round(d.get('message_stats',{}).get('deliver_details',{}).get('rate',0),1))" 2>/dev/null)
    printf "%-10s %-15s %-15s %-15s %-15s\n" "$(date +%H:%M:%S)" "${depth:-0}" "${unacked:-0}" "${pub:-0}/s" "${del:-0}/s"
  fi
  sleep $INTERVAL
done
