import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Generates a simple throughput line chart PNG.
 */
public final class ChartGenerator {

  private ChartGenerator() {}

  /**
   * Writes a throughput chart to PNG.
   *
   * @param outPath output file path
   * @param series ordered map of bucketStartEpochMs -> messagesPerSecond
   * @throws IOException if writing fails
   */
  public static void writeThroughputChart(Path outPath, Map<Long, Double> series)
      throws IOException {

    Files.createDirectories(outPath.getParent());

    int width = 900;
    int height = 500;
    int pad = 70;

    BufferedImage img =
        new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    Graphics2D g = img.createGraphics();

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);

    // Background
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, width, height);

    if (series.isEmpty()) {
      ImageIO.write(img, "png", outPath.toFile());
      g.dispose();
      return;
    }

    List<Double> ys = new ArrayList<>(series.values());

    double maxY = ys.stream()
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(1.0);

    if (maxY <= 0.0) {
      maxY = 1.0;
    }

    int plotW = width - 2 * pad;
    int plotH = height - 2 * pad;

    // Axes
    g.setColor(Color.BLACK);
    g.setStroke(new BasicStroke(2f));
    g.drawLine(pad, height - pad, width - pad, height - pad);
    g.drawLine(pad, pad, pad, height - pad);

    // Title
    g.setFont(new Font("SansSerif", Font.BOLD, 18));
    g.drawString("Throughput Over Time (10s buckets)",
        width / 2 - 170, 35);

    int n = ys.size();
    List<Integer> px = new ArrayList<>(n);
    List<Integer> py = new ArrayList<>(n);

    for (int i = 0; i < n; i++) {
      double xNorm = (n == 1) ? 0.0 : (double) i / (n - 1);
      double yNorm = ys.get(i) / maxY;

      int x = pad + (int) (xNorm * plotW);
      int y = (height - pad) - (int) (yNorm * plotH);

      px.add(x);
      py.add(y);
    }

    // Line / point
    g.setColor(new Color(30, 144, 255)); // blue
    g.setStroke(new BasicStroke(3f));

    if (n == 1) {
      int x = px.get(0);
      int y = py.get(0);
      g.fillOval(x - 5, y - 5, 10, 10);
    } else {
      for (int i = 1; i < n; i++) {
        g.drawLine(px.get(i - 1), py.get(i - 1),
            px.get(i), py.get(i));
      }
    }

    ImageIO.write(img, "png", outPath.toFile());
    g.dispose();
  }
}

