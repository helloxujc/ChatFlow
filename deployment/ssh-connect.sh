#!/bin/bash
# Usage: bash ssh-connect.sh <public-ip>
# Example: bash ssh-connect.sh 54.213.224.201

IP=$1
KEY=~/Downloads/key1.pem
PUB_KEY=~/Downloads/key1.pem.pub
REGION=us-west-2

INSTANCE_ID=$(aws ec2 describe-instances \
  --filters "Name=ip-address,Values=$IP" \
  --query "Reservations[0].Instances[0].InstanceId" \
  --output text \
  --region $REGION)

echo "Injecting key for $INSTANCE_ID ($IP)..."
aws ec2-instance-connect send-ssh-public-key \
  --instance-id $INSTANCE_ID \
  --instance-os-user ubuntu \
  --ssh-public-key file://$PUB_KEY \
  --region $REGION

echo "Connecting..."
ssh -i $KEY ubuntu@$IP
