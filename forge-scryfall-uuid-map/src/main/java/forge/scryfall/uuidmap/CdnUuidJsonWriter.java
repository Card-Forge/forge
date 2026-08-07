package forge.scryfall.uuidmap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

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
 *
 * <p>Collector-number and language keys are written in sorted order rather than
 * bulk-file insertion order, and a file is only touched on disk if its content
 * actually changed. The Scryfall bulk export doesn't guarantee stable card
 * ordering between snapshots, so without this, re-running against a newer
 * snapshot would rewrite nearly every set file with the same data in a
 * different key order — a huge, meaningless diff every time this tool runs.
 * With it, a re-run against unchanged sets touches nothing.
 */
public final class CdnUuidJsonWriter {

    private static final Comparator<String> COLLECTOR_NUMBER_ORDER = CdnUuidJsonWriter::compareCollectorNumbers;

    private CdnUuidJsonWriter() {}

    /**
     * Parses {@code bulkFile} and writes per-set UUID JSON files under {@code outputDir}.
     * Existing files whose content is unchanged are left untouched.
     *
     * @return number of set files created or updated (excludes unchanged files)
     */
    public static long write(Path bulkFile, Path outputDir) throws IOException {
        System.err.println("Parsing UUIDs from " + bulkFile.toAbsolutePath());

        // setCode -> cn -> lang -> [frontUuid, backUuidOrNull]
        // TreeMaps at every level so serialized key order is deterministic
        // regardless of card order in the bulk export.
        Map<String, Map<String, Map<String, String[]>>> bySet = new TreeMap<>();

        CardStreamParser.parse(bulkFile, record -> {
            String setCode = record.setCode().toLowerCase();
            bySet.computeIfAbsent(setCode, k -> new TreeMap<>(COLLECTOR_NUMBER_ORDER))
                 .computeIfAbsent(record.collectorNumber(), k -> new TreeMap<>())
                 .put(record.lang(), new String[]{record.frontUuid(), record.backUuid()});
        });

        System.err.printf("  Collected %,d unique sets.%n", bySet.size());

        Files.createDirectories(outputDir);
        long created = 0, updated = 0, unchanged = 0;
        for (Map.Entry<String, Map<String, Map<String, String[]>>> setEntry : bySet.entrySet()) {
            Path out = outputDir.resolve(setEntry.getKey() + ".json");
            String json = buildSetJson(setEntry.getValue());
            boolean existed = Files.exists(out);
            if (existed && json.equals(Files.readString(out, StandardCharsets.UTF_8))) {
                unchanged++;
                continue;
            }
            Files.writeString(out, json, StandardCharsets.UTF_8);
            if (existed) updated++; else created++;
        }

        System.err.printf("Done: %,d new, %,d updated, %,d unchanged set files under %s%n",
                created, updated, unchanged, outputDir.toAbsolutePath());
        return created + updated;
    }

    // -------------------------------------------------------------------------

    /** Natural-ish order: numeric prefix compared as a number, then the remainder as text. */
    private static int compareCollectorNumbers(String a, String b) {
        int ai = 0, bi = 0;
        while (ai < a.length() && Character.isDigit(a.charAt(ai))) ai++;
        while (bi < b.length() && Character.isDigit(b.charAt(bi))) bi++;
        if (ai > 0 && bi > 0) {
            int cmp = Long.compare(Long.parseLong(a.substring(0, ai)), Long.parseLong(b.substring(0, bi)));
            if (cmp != 0) return cmp;
        }
        return a.compareTo(b);
    }

    /** Builds {@code {cn: {lang: uuid|[front,back]}, ...}} followed by a trailing newline. */
    private static String buildSetJson(Map<String, Map<String, String[]>> cards) {
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
        sb.append('\n');
        return sb.toString();
    }

    private static void appendQuoted(StringBuilder sb, String s) {
        sb.append('"').append(s).append('"');
    }
}
