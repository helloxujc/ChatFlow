#!/bin/bash
# Start ChatFlow server on all 4 EC2 instances

KEY=~/Downloads/chatflow-key.pem
SERVERS=(54.213.224.201 54.186.75.122 35.163.56.167 35.95.35.2)

RABBIT_HOST=172.31.21.132
RABBIT_USER=chatflow
RABBIT_PASS=chatflow123

for ip in "${SERVERS[@]}"; do
  echo "Starting server on $ip..."
  ssh -i $KEY -o StrictHostKeyChecking=no ubuntu@$ip \
    "pkill -f chatflow.server.ServerMain; sleep 1; nohup bash -c 'ulimit -n 65536; exec env \
      RABBIT_HOST=$RABBIT_HOST \
      RABBIT_USER=$RABBIT_USER \
      RABBIT_PASS=$RABBIT_PASS \
      BROADCAST_THREADS=32 \
      java -Xmx700m -classpath /home/ubuntu/server-1.0-SNAPSHOT/lib/\* \
      chatflow.server.ServerMain' > ~/server.log 2>&1 &" &
done
wait

echo "Waiting 15s for JVM + RabbitMQ connection..."
sleep 15

for ip in "${SERVERS[@]}"; do
  echo -n "$ip: "
  ssh -i $KEY -o StrictHostKeyChecking=no ubuntu@$ip \
    "pgrep -f chatflow.server.ServerMain > /dev/null && echo RUNNING || echo FAILED"
done

ALB=chatflow-alb-1246938090.us-west-2.elb.amazonaws.com
echo ""
echo "Waiting for ALB health checks to pass (up to 90s)..."
for i in $(seq 1 18); do
  code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 http://$ALB/health)
  if [ "$code" = "200" ]; then
    echo "ALB healthy after $((i * 5))s"
    break
  fi
  echo "  ${i}/${18}: ALB returned $code, waiting 5s..."
  sleep 5
done
