%% Architecture diagram (Mermaid)
flowchart LR
  Client[Client]
  LoadClient["Load Tester"]
  ALB["ALB / LB"]
  WS["WebSocket Servers (N)"]
  MQ["Message Queue (RabbitMQ/Kafka)"]
  CON["Consumer-v3\nBatch Writer, Broadcaster, DeadLetter"]
  PG["Postgres (messages, dead_letters)"]
  MET["Metrics Exporter / API"]

  Client --> ALB
  LoadClient --> ALB
  ALB --> WS
  WS --> MQ
  MQ --> CON
  CON --> PG
  CON --> WS
  CON --> PG
  CON --> MET
  PG --> MET
  MET --> LoadClient

  classDef infra fill:#f3f4f6,stroke:#333
  class ALB,WS,MQ,CON,PG,MET infra
