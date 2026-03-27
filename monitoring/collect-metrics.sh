#!/bin/bash
# Monitor active WebSocket connections per server during load test

KEY=~/Downloads/key1.pem
PUB_KEY=~/Downloads/key1.pem.pub
SERVERS=(54.213.224.201 54.186.75.122 35.163.56.167 35.95.35.2)
INTERVAL=${1:-5}
REGION=us-west-2

inject_and_count() {
  local ip=$1
  local id
  id=$(aws ec2 describe-instances \
    --filters "Name=ip-address,Values=$ip" \
    --query "Reservations[0].Instances[0].InstanceId" \
    --output text --region $REGION 2>/dev/null)
  aws ec2-instance-connect send-ssh-public-key \
    --instance-id "$id" \
    --instance-os-user ubuntu \
    --ssh-public-key file://$PUB_KEY \
    --region $REGION > /dev/null 2>&1
  count=$(ssh -i $KEY -o StrictHostKeyChecking=no -o ConnectTimeout=3 ubuntu@$ip \
    "ss -tn | grep ':8081' | grep ESTAB | wc -l" 2>/dev/null)
  echo "${count:-0}"
}

echo "Monitoring connection distribution (Ctrl+C to stop)..."
echo "Interval: ${INTERVAL}s"
echo ""

while true; do
  total=0
  line="$(date +%H:%M:%S) |"
  for ip in "${SERVERS[@]}"; do
    count=$(inject_and_count "$ip")
    total=$((total + count))
    line="$line $ip: $count |"
  done
  echo "$line total: $total"
  sleep $INTERVAL
done
