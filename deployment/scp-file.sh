#!/bin/bash
# Usage: bash scp-file.sh <local-file> <public-ip> <remote-path>
# Example: bash scp-file.sh server-v2/build/libs/server-1.0-SNAPSHOT.jar 54.213.224.201 ~/server.jar

LOCAL=$1
IP=$2
REMOTE=$3
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

echo "Uploading $LOCAL to $IP:$REMOTE ..."
scp -i $KEY $LOCAL ubuntu@$IP:$REMOTE
echo "Done."
