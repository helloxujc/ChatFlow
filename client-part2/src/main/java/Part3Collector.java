import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Collects per-message latency by correlating outbound messages with acks.
 */
public final class Part3Collector {

  private static final class Inflight {
    private final long startNano;
    private final Instant sendTimestamp;
    private final MessageType type;
    private final int roomId;

    private Inflight(long startNano, Instant sendTimestamp, MessageType type, int roomId) {
      this.startNano = startNano;
      this.sendTimestamp = sendTimestamp;
      this.type = type;
      this.roomId = roomId;
    }
  }

  private final Map<Long, Inflight> inflight = new ConcurrentHashMap<>();
  private final List<LatencyRecord> records = Collections.synchronizedList(new ArrayList<>());
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Records send timing data for the given message.
   *
   * @param message outbound message
   */
  public void onSend(OutboundMessage message) {
    inflight.put(
        message.getSeqId(),
        new Inflight(
            System.nanoTime(),
            message.getTimestamp(),
            message.getMessageType(),
            message.getRoomId()));
  }

   /**
   * Records an ack and computes latency if correlation succeeds.
   *
   * @param ackText raw ack text
   */
  public void onAck(String ackText) {

    long seq = parseSeqIdFromAck(ackText);
    if (seq < 0) {
      return;
    }

    Inflight in = inflight.remove(seq);
    if (in == null) {
      return;
    }

    long latencyMs = Math.max(0L, (System.nanoTime() - in.startNano) / 1_000_000L);
    String status = parseStatusFromAck(ackText);
    records.add(new LatencyRecord(Instant.now(), in.type, latencyMs, status, in.roomId));
  }

  /**
   * Returns a snapshot of collected latency records.
   *
   * @return snapshot list
   */
  public List<LatencyRecord> snapshot() {
    synchronized (records) {
      return new ArrayList<>(records);
    }
  }

  /**
   * Parses seqId from the ack JSON payload's data.message field.
   */
  private long parseSeqIdFromAck(String ackText) {
    try {
      JsonNode root = MAPPER.readTree(ackText);
      JsonNode data = root.get("data");
      if (data == null) {
        return -1;
      }
      JsonNode msgNode = data.get("message");
      if (msgNode == null || !msgNode.isTextual()) {
        return -1;
      }
      return parseSeqFromMessage(msgNode.asText());
    } catch (Exception e) {
      return -1;
    }
  }

  /**
   * Extracts the numeric seq from a message like "seq:123|...".
   */
  private long parseSeqFromMessage(String message) {
    int i = message.indexOf("seq:");
    if (i < 0) {
      return -1;
    }
    int bar = message.indexOf('|', i);
    if (bar < 0) {
      return -1;
    }
    String num = message.substring(i + 4, bar).trim();
    try {
      return Long.parseLong(num);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private String parseStatusFromAck(String ackText) {
    try {
      JsonNode root = MAPPER.readTree(ackText);
      JsonNode status = root.get("status");
      if (status == null || !status.isTextual()) {
        return "UNKNOWN";
      }
      return status.asText();
    } catch (Exception e) {
      return "UNKNOWN";
    }
  }

  /**
   * Returns the number of collected latency records.
   *
   * @return record count
   */
  public int recordCount() {
    synchronized (records) {
      return records.size();
    }
  }

  /**
   * Returns the number of in-flight messages waiting for acks.
   *
   * @return in-flight count
   */
  public int inflightCount() {
    return inflight.size();
  }
}
