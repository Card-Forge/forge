package forge.game;

import forge.game.card.CardView;
import forge.util.ResearchMode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Research-only mirror of the existing GameLog stream.
 *
 * <p>Each accepted GameLogEntry is written as one JSON object per line.
 * This class deliberately observes only entries that already reached
 * GameLog.add(...); it does not inspect game state or expand visibility.</p>
 */
final class ResearchGameLogJson {
    static final String SYSTEM_PROPERTY = "forge.research.jsonLog";
    static final String ENVIRONMENT_VARIABLE = "FORGE_RESEARCH_JSON_LOG";
    private static final String DEFAULT_PATH = "research-playlog.jsonl";

    private static final AtomicLong NEXT_LOG_ID = new AtomicLong(1);
    private static boolean initialized = false;
    private static boolean warned = false;

    private ResearchGameLogJson() {
    }

    static long nextLogId() {
        return NEXT_LOG_ID.getAndIncrement();
    }

    static synchronized void append(long logId, long eventIndex, GameLogEntry entry) {
        if (!ResearchMode.isEnabled()) {
            return;
        }

        Path path = outputPath();
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            StandardOpenOption[] options;
            if (!initialized) {
                options = new StandardOpenOption[] {
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                };
                initialized = true;
            } else {
                options = new StandardOpenOption[] {
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND,
                        StandardOpenOption.WRITE
                };
            }

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, options)) {
                writer.write(toJson(logId, eventIndex, entry));
                writer.newLine();
            }
        } catch (IOException | RuntimeException ex) {
            // Research logging must never change normal Forge execution.
            if (!warned) {
                warned = true;
                System.err.println("Research JSON log write failed: " + ex.getMessage());
            }
        }
    }

    private static Path outputPath() {
        String configured = System.getProperty(SYSTEM_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(ENVIRONMENT_VARIABLE);
        }
        if (configured == null || configured.isBlank()) {
            configured = DEFAULT_PATH;
        }
        return Path.of(configured);
    }

    private static String toJson(long logId, long eventIndex, GameLogEntry entry) {
        CardView source = entry.sourceCard();
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        field(sb, "schema", "forge-research-gamelog-v1").append(',');
        numberField(sb, "log_id", logId).append(',');
        numberField(sb, "event_index", eventIndex).append(',');
        numberField(sb, "timestamp_ms", System.currentTimeMillis()).append(',');
        field(sb, "type", entry.type().name()).append(',');
        field(sb, "caption", entry.type().getCaption()).append(',');
        field(sb, "message", entry.message()).append(',');
        if (source == null) {
            sb.append("\"source_card\":null");
        } else {
            field(sb, "source_card", source.getName());
        }
        sb.append('}');
        return sb.toString();
    }

    private static StringBuilder field(StringBuilder sb, String key, String value) {
        sb.append('"').append(escape(key)).append("\":");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"').append(escape(value)).append('"');
        }
        return sb;
    }

    private static StringBuilder numberField(StringBuilder sb, String key, long value) {
        return sb.append('"').append(escape(key)).append("\":").append(value);
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int)c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
