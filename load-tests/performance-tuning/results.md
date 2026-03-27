# Load Test Results

## Batch Size Tuning (500K messages each)

| Test | Batch Size | Flush Interval | Throughput  | p50  | p95  | p99   | Dead Letters |
|------|------------|----------------|-------------|------|------|-------|--------------|
| T1   | 100        | 100ms          | 1,847 msg/s | 26ms | 31ms | 55ms  | 0            |
| T2   | 500        | 500ms          | 1,846 msg/s | 25ms | 31ms | 46ms  | 0            |
| T3   | 1,000      | 500ms          | 1,850 msg/s | 26ms | 33ms | 99ms  | 0            |
| T4   | 5,000      | 1,000ms        | 1,845 msg/s | 12ms | 31ms | 101ms | 0            |
| T5   | 1,000      | 100ms          | 1,856 msg/s | 26ms | 31ms | 70ms  | 0            |

**Optimal configuration chosen:** T5 — Batch size: 1000, Flush interval: 100ms
**Reason:** Highest throughput (1,856 msg/s) with flush latency bounded at 100ms. At most 1,000 messages at risk per crash.
