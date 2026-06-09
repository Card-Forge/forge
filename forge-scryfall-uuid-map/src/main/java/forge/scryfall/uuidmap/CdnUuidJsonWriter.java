package forge.scryfall.uuidmap;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads a Scryfall bulk JSON export and writes one UUID JSON file per card print to
 * {@code outputDir/{setCode}/{collectorNumber}.json}.
 *
 * <p>Each file maps language code(s) to the CDN UUID for that card:
 * <pre>
 *   {"en": "uuid"}                           — single language
 *   {"en": "uuid", "ja": "ja-uuid"}          — multiple languages (same collector number)
 *   {"en": ["front-uuid", "back-uuid"]}      — DFC with distinct face UUIDs (rare)
 * </pre>
 *
 * <p>All languages present in the bulk data are written for each card, so callers
 * can prefer a specific language and fall back to English when no language-specific
 * entry exists.
 */
public final class CdnUuidJsonWriter {

    private CdnUuidJsonWriter() {}

    /**
     * Parses {@code bulkFile} and writes UUID JSON files under {@code outputDir}.
     *
     * @return total number of per-collector-number files written
     */
    public static long write(Path bulkFile, Path outputDir) throws IOException {
        System.err.println("Parsing UUIDs from " + bulkFile.toAbsolutePath());

        // setCode/cn -> lang -> [frontUuid, backUuidOrNull]
        Map<String, Map<String, String[]>> byCard = new LinkedHashMap<>(700_000);

        CardStreamParser.parse(bulkFile, record -> {
            String key = record.setCode().toLowerCase() + "/" + record.collectorNumber();
            byCard.computeIfAbsent(key, k -> new LinkedHashMap<>())
                  .put(record.lang(), new String[]{record.frontUuid(), record.backUuid()});
        });

        System.err.printf("  Collected %,d unique set/collector-number entries.%n", byCard.size());

        long filesWritten = 0;
        for (Map.Entry<String, Map<String, String[]>> e : byCard.entrySet()) {
            String[] parts = e.getKey().split("/", 2);
            String setCode = parts[0];
            String cn      = parts[1];
            Map<String, String[]> langs = e.getValue();

            Path dir = outputDir.resolve(setCode);
            Files.createDirectories(dir);
            Path out = dir.resolve(cn + ".json");
            writeJson(out, langs);
            filesWritten++;
        }

        System.err.printf("Done: %,d files written under %s%n", filesWritten, outputDir.toAbsolutePath());
        return filesWritten;
    }

    // -------------------------------------------------------------------------

    private static void writeJson(Path out, Map<String, String[]> langs) throws IOException {
        StringBuilder sb = new StringBuilder(langs.size() * 60);
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, String[]> e : langs.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            String lang  = e.getKey();
            String front = e.getValue()[0];
            String back  = e.getValue()[1];
            sb.append('"').append(lang).append('"').append(':');
            if (back != null && !back.equals(front)) {
                sb.append('[').append('"').append(front).append('"')
                  .append(',').append('"').append(back).append('"').append(']');
            } else {
                sb.append('"').append(front).append('"');
            }
        }
        sb.append('}');
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            pw.print(sb);
            pw.println();
        }
    }
}
