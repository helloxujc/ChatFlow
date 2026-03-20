#!/bin/bash
# Poll PostgreSQL metrics during load tests.
# Run this on the DB EC2 instance, or remotely if psql is installed locally.
# Usage: bash postgres-stats.sh [interval_seconds]

DB_HOST=${DB_HOST:-localhost}
DB_NAME=${DB_NAME:-chatflow}
DB_USER=${DB_USER:-chatflow}
INTERVAL=${1:-5}

export PGPASSWORD=${DB_PASS:-chatflow}

echo "Polling PostgreSQL metrics every ${INTERVAL}s (Ctrl+C to stop)..."
echo ""
printf "%-10s %-12s %-12s %-12s %-15s\n" \
  "Time" "TotalRows" "RowsPerSec" "Connections" "DeadLetters"
echo "--------------------------------------------------------------------"

prev_count=0
prev_time=$(date +%s)

while true; do
  now=$(date +%s)
  elapsed=$((now - prev_time))

  # Total rows in messages table
  total=$(psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -tAc \
    "SELECT COUNT(*) FROM messages;" 2>/dev/null | tr -d '[:space:]')
  total=${total:-0}

  # Active connections to chatflow database
  conns=$(psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -tAc \
    "SELECT COUNT(*) FROM pg_stat_activity WHERE datname = '$DB_NAME';" \
    2>/dev/null | tr -d '[:space:]')
  conns=${conns:-0}

  # Rows written per second since last poll
  if [ "$elapsed" -gt 0 ] && [ "$prev_count" -gt 0 ]; then
    diff=$((total - prev_count))
    rate=$((diff / elapsed))
  else
    rate=0
  fi

  # Dead letter count from log file (if running on same host as consumer)
  dlq=0
  if [ -f "failed-db-writes.log" ]; then
    dlq=$(wc -l < "failed-db-writes.log" | tr -d '[:space:]')
  fi

  printf "%-10s %-12s %-12s %-12s %-15s\n" \
    "$(date +%H:%M:%S)" "$total" "${rate}/s" "$conns" "$dlq"

  prev_count=$total
  prev_time=$now
  sleep "$INTERVAL"
done
