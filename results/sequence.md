%% Sequence diagram (Mermaid)
sequenceDiagram
  participant Client
  participant ALB
  participant WS as "WS Server"
  participant MQ as "Message Queue"
  participant CON as "Consumer Pool"
  participant PG as "Postgres"
  participant MET as "Metrics API"

  Client->>ALB: open WS / send message(json)
  ALB->>WS: deliver frame
  WS->>WS: validate schema & user/session
  alt valid
    WS->>MQ: publish message (roomId,userId,...)
    MQ->>CON: deliver message
    loop batching
      CON->>CON: accumulate batch (thread-safe)
      alt flush condition (size/time)
        CON->>PG: multi-row INSERT ... ON CONFLICT (transaction)
        PG-->>CON: commit OK
        CON->>WS: broadcast to connected clients in room
        CON->>MET: record batch metrics (count, latency)
      end
    end
  else invalid
    WS-->>Client: send error (validation failed)
  end
  alt DB write failure (transient)
    CON->>CON: retry (exp backoff) up to N
    CON->>MET: record retry
  else DB permanent failure
    CON->>PG: insert into dead_letters
    CON->>MET: record dead_letter
  end
  WS-->>Client: receive broadcast / server ack
