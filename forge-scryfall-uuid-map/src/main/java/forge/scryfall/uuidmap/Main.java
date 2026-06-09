package forge.scryfall.uuidmap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Entry point for the Scryfall CDN UUID map generator.
 *
 * <p>Reads a Scryfall bulk JSON file and writes one JSON file per card print:
 * <pre>
 *   {outputDir}/{setCode}/{collectorNumber}.json  →  {"en":"uuid","ja":"uuid",...}
 * </pre>
 *
 * <p>Uses the {@code all_cards} Scryfall dataset by default (every language, every
 * art variant, ~2.5 GB) so that per-language UUID entries are fully populated.
 * Pass {@code --default-cards} to instead fetch {@code default_cards} (~100 MB,
 * one entry per card in English or nearest language only).
 *
 * <p>CDN URL formula (for reference):
 * {@code https://cards.scryfall.io/{size}/{front|back}/{uuid[0]}/{uuid[1]}/{uuid}.jpg}
 *
 * <p>Usage:
 * <pre>
 *   # Download all_cards from Scryfall and write to cdn_uuid (full multi-language)
 *   java -jar forge-scryfall-uuid-map.jar --output-dir path/to/res/cdn_uuid
 *
 *   # Provide a pre-downloaded bulk file
 *   java -jar forge-scryfall-uuid-map.jar --bulk-file all-cards-20260608.json \
 *                                          --output-dir path/to/res/cdn_uuid
 *
 *   # English-only, smaller download (~100 MB)
 *   java -jar forge-scryfall-uuid-map.jar --output-dir path/to/res/cdn_uuid --default-cards
 * </pre>
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        Path    bulkFile     = null;
        Path    outputDir    = null;
        boolean defaultCards = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--bulk-file":     bulkFile     = Path.of(args[++i]); break;
                case "--output-dir":   outputDir    = Path.of(args[++i]); break;
                case "--default-cards": defaultCards = true;              break;
                case "--help":         printUsage(); return;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    printUsage();
                    System.exit(1);
            }
        }

        if (outputDir == null) {
            System.err.println("Error: --output-dir is required.");
            printUsage();
            System.exit(1);
        }
        Files.createDirectories(outputDir);

        if (bulkFile == null) {
            bulkFile = findLocalBulkFile();
            if (bulkFile != null) {
                System.err.println("Auto-detected: " + bulkFile);
            }
        }

        if (bulkFile == null || !Files.exists(bulkFile)) {
            if (defaultCards) {
                System.err.println("Fetching 'default_cards' from Scryfall (~100 MB, English only).");
                String uri = BulkDataFetcher.fetchDefaultCardsUri();
                bulkFile = Path.of(uriFilename(uri));
                BulkDataFetcher.downloadToFile(uri, bulkFile);
            } else {
                System.err.println("Fetching 'all_cards' from Scryfall (~2.5 GB, all languages).");
                System.err.println("Use --default-cards for a smaller English-only download.");
                String uri = BulkDataFetcher.fetchAllCardsUri();
                bulkFile = Path.of(uriFilename(uri));
                BulkDataFetcher.downloadToFile(uri, bulkFile);
            }
        }

        System.err.println("Input:      " + bulkFile.toAbsolutePath());
        System.err.println("Output dir: " + outputDir.toAbsolutePath());

        long startMs   = System.currentTimeMillis();
        long written   = CdnUuidJsonWriter.write(bulkFile, outputDir);
        long elapsedMs = System.currentTimeMillis() - startMs;

        System.err.printf("%nDone: %,d card entries written in %.1f s%n", written, elapsedMs / 1000.0);
    }

    private static String uriFilename(String uri) {
        String path = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static Path findLocalBulkFile() {
        try (Stream<Path> entries = Files.list(Path.of("."))) {
            return entries
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return (name.startsWith("all-cards-") || name.startsWith("default-cards-"))
                                && name.endsWith(".json");
                    })
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java -jar forge-scryfall-uuid-map.jar --output-dir DIR [options]");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  --output-dir DIR    Output directory for JSON files (required)");
        System.err.println("                      e.g. forge-gui/res/cdn_uuid");
        System.err.println("  --bulk-file FILE     Pre-downloaded Scryfall all_cards or default_cards JSON.");
        System.err.println("                       Auto-detected if a matching file exists locally.");
        System.err.println("                       If absent, all_cards is downloaded from Scryfall.");
        System.err.println("  --default-cards      Download default_cards (~100 MB) instead of all_cards");
        System.err.println("                       (~2.5 GB). Produces English-only output.");
        System.err.println();
        System.err.println("Output: {outputDir}/{setCode}/{collectorNumber}.json");
        System.err.println("  Each file maps language code -> UUID string, or [front, back] for DFCs");
        System.err.println("  with distinct face UUIDs (rare). Example:");
        System.err.println("    {\"en\":\"4e7a547f-...\",\"ja\":\"9b2c1234-...\"}");
        System.err.println();
        System.err.println("CDN URL formula:");
        System.err.println("  https://cards.scryfall.io/{size}/{front|back}/{uuid[0]}/{uuid[1]}/{uuid}.jpg");
    }
}
