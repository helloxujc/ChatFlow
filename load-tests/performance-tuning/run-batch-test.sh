#!/bin/bash
# Run a single batch tuning test.
# Usage: bash run-batch-test.sh batch-t3.env
# Run from the consumer-v3 directory on the consumer EC2 instance.

if [ -z "$1" ]; then
  echo "Usage: bash run-batch-test.sh <config.env>"
  echo "Example: bash run-batch-test.sh batch-t3.env"
  exit 1
fi

CONFIG_FILE="$(dirname "$0")/$1"
if [ ! -f "$CONFIG_FILE" ]; then
  echo "Config file not found: $CONFIG_FILE"
  exit 1
fi

# Load batch/flush config for this test
source "$CONFIG_FILE"
echo "=== Starting test with: $1 ==="
echo "DB_BATCH_SIZE=$DB_BATCH_SIZE  DB_FLUSH_MS=$DB_FLUSH_MS"
echo ""

# Remove previous dead letter log so counts are fresh
rm -f failed-db-writes.log

# Start consumer-v3 (adjust jar path if needed)
java -jar build/libs/consumer-v3.jar
