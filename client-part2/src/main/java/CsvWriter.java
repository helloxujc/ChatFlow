import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes latency records to a CSV file.
 */
public final class CsvWriter {

  private CsvWriter() {}

  /**
   * Writes the given records to a CSV file.
   *
   * @param outPath output file path
   * @param records latency records
   * @throws IOException if writing fails
   */
  public static void writeLatencyCsv(Path outPath, List<LatencyRecord> records) throws IOException {
    Files.createDirectories(outPath.getParent());
    try (BufferedWriter w = Files.newBufferedWriter(outPath)) {
      w.write("timestamp,messageType,latency,statusCode,roomId");
      w.newLine();
      for (LatencyRecord r : records) {
        w.write(r.getTimestamp().toString());
        w.write(',');
        w.write(r.getMessageType().name());
        w.write(',');
        w.write(Long.toString(r.getLatencyMs()));
        w.write(',');
        w.write(r.getStatusCode());
        w.write(',');
        w.write(Integer.toString(r.getRoomId()));
        w.newLine();
      }
    }
  }
}

