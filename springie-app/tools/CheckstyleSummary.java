import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Aggregates the per-module checkstyle results of a full build into one
 * set of totals. Run by the springie-app build at verify time (the app
 * module builds last, so every module's result file for this build is
 * already on disk); not part of the shipped application.
 *
 * Usage: java CheckstyleSummary.java <repo root> [summary file]
 *
 * Scans the immediate subdirectories of the repo root for
 * target/checkstyle-result.xml, counts the violations by rule and by
 * module, prints the totals, and optionally writes them to a file.
 * Informational only: always exits 0.
 */
public class CheckstyleSummary {
  public static void main(final String[] args) {
    try {
      run(args);
    } catch (final Exception e) {
      // Never fail the build over a summary.
      System.out.println("Checkstyle summary failed: " + e);
    }
  }

  private static void run(final String[] args) throws Exception {
    final Path root = Path.of(args[0]).toAbsolutePath().normalize();
    final List<Path> modules = new ArrayList<>();
    try (var stream = Files.list(root)) {
      stream.filter(Files::isDirectory)
          .filter(dir -> Files.isRegularFile(
              dir.resolve("target/checkstyle-result.xml")))
          .sorted(Comparator.comparing(dir -> dir.getFileName().toString()))
          .forEach(modules::add);
    }
    if (modules.isEmpty()) {
      System.out.println(
          "Checkstyle summary: no checkstyle-result.xml files found under "
              + root);
      return;
    }

    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",
        true);
    final Map<String, Integer> by_rule = new TreeMap<>();
    final Map<String, Integer> by_module = new TreeMap<>();
    int total = 0;
    for (final Path module : modules) {
      final Document doc = factory.newDocumentBuilder().parse(
          module.resolve("target/checkstyle-result.xml").toFile());
      final NodeList errors = doc.getElementsByTagName("error");
      for (int i = 0; i < errors.getLength(); i++) {
        final String source = ((Element) errors.item(i)).getAttribute("source");
        String rule = source.substring(source.lastIndexOf('.') + 1);
        if (rule.endsWith("Check")) {
          rule = rule.substring(0, rule.length() - "Check".length());
        }
        by_rule.merge(rule, 1, Integer::sum);
      }
      by_module.put(module.getFileName().toString(), errors.getLength());
      total += errors.getLength();
    }

    final StringBuilder out = new StringBuilder();
    out.append("Checkstyle: ").append(total).append(" warnings across ")
        .append(modules.size()).append(" modules\n\nBy rule:\n");
    by_rule.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
            .thenComparing(Map.Entry.comparingByKey()))
        .forEach(e -> out.append(String.format("  %5d  %s%n", e.getValue(),
            e.getKey())));
    out.append("\nBy module:\n");
    by_module.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
            .thenComparing(Map.Entry.comparingByKey()))
        .forEach(e -> out.append(String.format("  %5d  %s%n", e.getValue(),
            e.getKey())));
    System.out.print(out);

    if (args.length > 1) {
      final Path summary = Path.of(args[1]);
      Files.createDirectories(summary.toAbsolutePath().getParent());
      Files.writeString(summary, out.toString());
    }
  }
}
