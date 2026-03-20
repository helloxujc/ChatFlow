# Load Test Results

## Batch Size Tuning (500K messages each)

| Test | Batch Size | Flush Interval | DB Writes/sec | Total DB Write Time (s) | Dead Letters | Notes |
|------|-----------|----------------|---------------|------------------------|--------------|-------|
| T1   | 100       | 100ms          |               |                        |              |       |
| T2   | 500       | 500ms          |               |                        |              |       |
| T3   | 1000      | 500ms          |               |                        |              |       |
| T4   | 5000      | 1000ms         |               |                        |              |       |
| T5   | 1000      | 100ms          |               |                        |              |       |

**Optimal configuration chosen:** TX — Batch size: ___, Flush interval: ___ms
**Reason:**

---

## Test 1: Baseline (500K messages)

**Configuration:** Batch size: ___, Flush interval: ___ms

| Metric | Value |
|--------|-------|
| Client throughput (msg/s) | |
| DB write throughput (rows/s) | |
| Write latency p50 (ms) | |
| Write latency p95 (ms) | |
| Write latency p99 (ms) | |
| Peak queue depth | |
| Avg queue depth | |
| Peak DB connections | |
| Dead letter count | |
| Total runtime (s) | |

---

## Test 2: Stress Test (1M messages)

**Configuration:** Batch size: ___, Flush interval: ___ms

| Metric | Value |
|--------|-------|
| Client throughput (msg/s) | |
| DB write throughput (rows/s) | |
| Peak queue depth | |
| Bottleneck identified | |
| Dead letter count | |
| Total runtime (s) | |

---

## Test 3: Endurance Test (sustained ~30 min)

**Configuration:** Batch size: ___, Flush interval: ___ms
**Target rate:** 80% of baseline max throughput = ___ msg/s

| Metric | Start | 10 min | 20 min | 30 min |
|--------|-------|--------|--------|--------|
| DB write throughput (rows/s) | | | | |
| DB connections | | | | |
| Queue depth | | | | |
| Dead letter count | | | | |
| JVM memory (MB) | | | | |
