#!/bin/bash
# Start ChatFlow server on all 4 EC2 instances

KEY=~/Downloads/key1.pem
SERVERS=(54.213.224.201 54.186.75.122 35.163.56.167 35.95.35.2)

RABBIT_HOST=172.31.21.132
RABBIT_USER=chatflow
RABBIT_PASS=chatflow123
DB_URL=jdbc:postgresql://172.31.27.231:5432/chatflow
DB_USER=chatflow
DB_PASS=chatflow

for ip in "${SERVERS[@]}"; do
  echo "Injecting key for $ip..."
  INSTANCE_ID=$(aws ec2 describe-instances \
    --filters "Name=ip-address,Values=$ip" \
    --query "Reservations[0].Instances[0].InstanceId" \
    --output text --region us-west-2)
  aws ec2-instance-connect send-ssh-public-key \
    --instance-id $INSTANCE_ID \
    --instance-os-user ubuntu \
    --ssh-public-key file://~/Downloads/key1.pem.pub \
    --region us-west-2 > /dev/null

  echo "Starting server on $ip..."
  ssh -i $KEY -o StrictHostKeyChecking=no ubuntu@$ip \
    "pkill -f 'java.*server.jar'; sleep 1;
    export RABBIT_HOST=$RABBIT_HOST
    export RABBIT_USER=$RABBIT_USER
    export RABBIT_PASS=$RABBIT_PASS
    export DB_URL='$DB_URL'
    export DB_USER=$DB_USER
    export DB_PASS=$DB_PASS
    export BROADCAST_THREADS=32
    nohup java -Xmx700m -jar /home/ubuntu/server.jar > ~/server.log 2>&1 &" &
done
wait

echo "Waiting 15s for JVM + RabbitMQ connection..."
sleep 15

for ip in "${SERVERS[@]}"; do
  INSTANCE_ID=$(aws ec2 describe-instances \
    --filters "Name=ip-address,Values=$ip" \
    --query "Reservations[0].Instances[0].InstanceId" \
    --output text --region us-west-2)
  aws ec2-instance-connect send-ssh-public-key \
    --instance-id $INSTANCE_ID \
    --instance-os-user ubuntu \
    --ssh-public-key file://~/Downloads/key1.pem.pub \
    --region us-west-2 > /dev/null
  echo -n "$ip: "
  ssh -i $KEY -o StrictHostKeyChecking=no ubuntu@$ip \
    "pgrep -f 'java.*server.jar' > /dev/null && echo RUNNING || echo FAILED"
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
