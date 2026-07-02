package forge.scryfall.uuidmap;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads a Scryfall bulk JSON export and writes one UUID JSON file per set to
 * {@code outputDir/{setCode}.json}.
 *
 * <p>Each file maps collector number to a per-language UUID map:
 * <pre>
 *   {
 *     "1":   {"en": "uuid"}
 *     "2":   {"en": "uuid", "ja": "ja-uuid"}
 *     "A-40":{"en": ["front-uuid", "back-uuid"]}
 *   }
 * </pre>
 *
 * <p>This set-per-file layout lets the runtime fetch exactly one file per set on
 * demand and cache it locally, rather than shipping ~115k individual files with
 * the game distribution.
 */
public final class CdnUuidJsonWriter {

    private CdnUuidJsonWriter() {}

    /**
     * Parses {@code bulkFile} and writes per-set UUID JSON files under {@code outputDir}.
     *
     * @return total number of set files written
     */
    public static long write(Path bulkFile, Path outputDir) throws IOException {
        System.err.println("Parsing UUIDs from " + bulkFile.toAbsolutePath());

        // setCode -> cn -> lang -> [frontUuid, backUuidOrNull]
        Map<String, Map<String, Map<String, String[]>>> bySet = new LinkedHashMap<>(1500);

        CardStreamParser.parse(bulkFile, record -> {
            String setCode = record.setCode().toLowerCase();
            bySet.computeIfAbsent(setCode, k -> new LinkedHashMap<>())
                 .computeIfAbsent(record.collectorNumber(), k -> new LinkedHashMap<>())
                 .put(record.lang(), new String[]{record.frontUuid(), record.backUuid()});
        });

        System.err.printf("  Collected %,d unique sets.%n", bySet.size());

        Files.createDirectories(outputDir);
        long filesWritten = 0;
        for (Map.Entry<String, Map<String, Map<String, String[]>>> setEntry : bySet.entrySet()) {
            Path out = outputDir.resolve(setEntry.getKey() + ".json");
            writeSetJson(out, setEntry.getValue());
            filesWritten++;
        }

        System.err.printf("Done: %,d set files written under %s%n", filesWritten,
                outputDir.toAbsolutePath());
        return filesWritten;
    }

    // -------------------------------------------------------------------------

    /** Writes {@code {cn: {lang: uuid|[front,back]}, ...}} to {@code out}. */
    private static void writeSetJson(Path out,
            Map<String, Map<String, String[]>> cards) throws IOException {
        StringBuilder sb = new StringBuilder(cards.size() * 80);
        sb.append('{');
        boolean firstCn = true;
        for (Map.Entry<String, Map<String, String[]>> cnEntry : cards.entrySet()) {
            if (!firstCn) sb.append(',');
            firstCn = false;
            appendQuoted(sb, cnEntry.getKey());
            sb.append(":{");
            boolean firstLang = true;
            for (Map.Entry<String, String[]> langEntry : cnEntry.getValue().entrySet()) {
                if (!firstLang) sb.append(',');
                firstLang = false;
                String lang  = langEntry.getKey();
                String front = langEntry.getValue()[0];
                String back  = langEntry.getValue()[1];
                appendQuoted(sb, lang);
                sb.append(':');
                if (back != null && !back.equals(front)) {
                    sb.append('[');
                    appendQuoted(sb, front);
                    sb.append(',');
                    appendQuoted(sb, back);
                    sb.append(']');
                } else {
                    appendQuoted(sb, front);
                }
            }
            sb.append('}');
        }
        sb.append('}');
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            pw.println(sb);
        }
    }

    private static void appendQuoted(StringBuilder sb, String s) {
        sb.append('"').append(s).append('"');
    }
}
