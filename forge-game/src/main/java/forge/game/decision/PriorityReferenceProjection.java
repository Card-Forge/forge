package forge.game.decision;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Canonical semantic projection of Priority diagnostics, excluding process and measurement metadata. */
public final class PriorityReferenceProjection {
    public static final String VERSION = "PRIORITY_REFERENCE_V1";
    private static final Set<String> EXCLUDED_COLUMNS = Set.of(
            "process_id", "request_generation_ns", "native_callback_ns", "feasibility_ns",
            "adjustment_preview_ns");

    private PriorityReferenceProjection() {
    }

    public static List<String> readAndProject(final Path csv) throws IOException {
        final List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return List.of();
        }
        return project(lines.get(0), lines.subList(1, lines.size()));
    }

    public static Set<String> readColumnValues(final Path csv, final String column) throws IOException {
        final List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return Set.of();
        }
        return columnValues(lines.get(0), lines.subList(1, lines.size()), column);
    }

    public static Set<String> columnValues(final String header, final List<String> rows, final String column) {
        final List<String> columns = parseCsv(header);
        final int columnIndex = columns.indexOf(column);
        if (columnIndex < 0) {
            throw new IllegalArgumentException("CSV column not found: " + column);
        }
        final Set<String> valuesInColumn = new LinkedHashSet<>();
        for (final String row : rows) {
            if (row.isBlank()) {
                continue;
            }
            final List<String> values = parseCsv(row);
            if (values.size() != columns.size()) {
                throw new IllegalArgumentException("CSV column count mismatch: " + values.size()
                        + " != " + columns.size());
            }
            valuesInColumn.add(values.get(columnIndex));
        }
        return Set.copyOf(valuesInColumn);
    }

    public static List<String> project(final String header, final List<String> rows) {
        final List<String> columns = parseCsv(header);
        final List<Integer> includedIndices = new ArrayList<>();
        for (int index = 0; index < columns.size(); index++) {
            if (!EXCLUDED_COLUMNS.contains(columns.get(index))) {
                includedIndices.add(index);
            }
        }
        final List<String> records = new ArrayList<>();
        for (final String row : rows) {
            if (row.isBlank()) {
                continue;
            }
            final List<String> values = parseCsv(row);
            if (values.size() != columns.size()) {
                throw new IllegalArgumentException("Priority CSV column count mismatch: " + values.size()
                        + " != " + columns.size());
            }
            final StringBuilder record = new StringBuilder(VERSION).append('|').append(records.size());
            for (final int index : includedIndices) {
                record.append('|').append(canonical(columns.get(index))).append('=')
                        .append(canonical(values.get(index)));
            }
            records.add(record.toString());
        }
        return List.copyOf(records);
    }

    public static String hash(final List<String> records) {
        return DeterminismTraceHasher.sha256(records);
    }

    private static List<String> parseCsv(final String row) {
        final List<String> values = new ArrayList<>();
        final StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < row.length(); index++) {
            final char current = row.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < row.length() && row.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unterminated quoted Priority CSV field");
        }
        values.add(value.toString());
        return values;
    }

    private static String canonical(final String value) {
        return value.replace("%", "%25").replace("|", "%7C").replace("=", "%3D")
                .replace("\r", "%0D").replace("\n", "%0A");
    }
}
