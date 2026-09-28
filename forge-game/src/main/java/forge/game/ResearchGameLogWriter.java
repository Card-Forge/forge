package forge.game;

import forge.util.ResearchMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Research-only mirror of the existing {@link GameLog}.
 *
 * <p>Each {@link GameLogEntry} accepted by {@link GameLog} is written as one
 * JSON object per line (JSONL). This deliberately mirrors the existing log
 * rather than introducing new observations, so enabling the logger does not
 * change game visibility or AI decision making.</p>
 */
final class ResearchGameLogWriter {
    static final String OUTPUT_PROPERTY = "forge.research.gameLogJsonl";
    private static final String DEFAULT_OUTPUT = "research-game-log.jsonl";

    private static final AtomicLong NEXT_STREAM_ID = new AtomicLong(1);
    private static boolean warnedAboutWriteFailure = false;

    private ResearchGameLogWriter() {
    }

    static long nextStreamId() {
        return NEXT_STREAM_ID.getAndIncrement();
    }

    static synchronized void write(long streamId, long sequence, GameLogEntry entry) {
        if (!ResearchMode.isEnabled()) {
            return;
        }

        final Path output = Path.of(System.getProperty(OUTPUT_PROPERTY, DEFAULT_OUTPUT));

        try {
            final Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    output,
                    toJsonLine(streamId, sequence, entry) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ex) {
            // Research logging must never alter or abort normal Forge gameplay.
            if (!warnedAboutWriteFailure) {
                warnedAboutWriteFailure = true;
                System.err.println("[research] Failed to write GameLog JSONL: " + ex.getMessage());
            }
        }
    }

    private static String toJsonLine(long streamId, long sequence, GameLogEntry entry) {
        final String sourceCard = entry.sourceCard() == null ? null : entry.sourceCard().toString();

        return "{"
                + "\"stream_id\":" + streamId
                + ",\"sequence\":" + sequence
                + ",\"type\":" + quote(entry.type().name())
                + ",\"caption\":" + quote(entry.type().getCaption())
                + ",\"message\":" + quote(entry.message())
                + ",\"source_card\":" + quoteNullable(sourceCard)
                + "}";
    }

    private static String quoteNullable(String value) {
        return value == null ? "null" : quote(value);
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }

        final StringBuilder out = new StringBuilder(value.length() + 16);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
        return out.toString();
    }
}
