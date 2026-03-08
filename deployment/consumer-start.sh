#!/bin/bash
# Start ChatFlow consumer on EC2 via systemd
# Usage: ./consumer-start.sh [STRIPE_COUNT]

KEY=~/Downloads/chatflow-key.pem
CONSUMER_IP=54.218.236.208
STRIPE_COUNT=${1:-20}

echo "Deploying consumer with STRIPE_COUNT=$STRIPE_COUNT..."

# Update ExecStart line in service file with new stripe count
ssh -i $KEY -o StrictHostKeyChecking=no ubuntu@$CONSUMER_IP \
  "sudo sed -i 's|ExecStart=.*|ExecStart=/usr/bin/java -jar /home/ubuntu/consumer.jar $STRIPE_COUNT|' \
   /etc/systemd/system/consumer.service && \
   sudo systemctl daemon-reload && \
   sudo systemctl restart consumer && \
   sleep 2 && \
   sudo systemctl status consumer --no-pager | head -5"

echo "Consumer started with STRIPE_COUNT=$STRIPE_COUNT"
