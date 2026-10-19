package forge.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Neutralises text that came from somewhere else before it reaches a log line
 * or a chat broadcast.
 *
 * <p>Log files are read one record per line, so a value containing a newline
 * can forge an entry that reads as something the server said; the same trick
 * on a name echoed into chat draws fake system messages in other players'
 * panes.
 *
 * <p>Two shapes because the right answer differs: {@link #forLog} keeps the
 * escapes visible so a reader can see what was actually sent, while
 * {@link #forDisplay} strips the control characters outright, since a UI has no
 * use for them.
 */
public final class LogSafe {

    /** Generous for a name or a chat line; short enough to bound a flood. */
    public static final int DEFAULT_MAX_LENGTH = 512;

    private static final String TRUNCATED = "…[truncated]";

    private LogSafe() {
    }

    public static String forLog(final String text) {
        return scrub(text, DEFAULT_MAX_LENGTH, true);
    }

    public static String forLog(final String text, final int maxLength) {
        return scrub(text, maxLength, true);
    }

    public static String forDisplay(final String text) {
        return scrub(text, DEFAULT_MAX_LENGTH, false);
    }

    public static String forDisplay(final String text, final int maxLength) {
        return scrub(text, maxLength, false);
    }

    private static String scrub(final String text, final int maxLength, final boolean escape) {
        if (text == null) {
            return null;
        }
        final String clipped = StringUtils.truncate(text, maxLength);
        final StringBuilder out = new StringBuilder(clipped.length() + 8);
        for (int i = 0; i < clipped.length(); i++) {
            final char c = clipped.charAt(i);
            if (!isControl(c)) {
                out.append(c);
            } else if (escape) {
                switch (c) {
                    case '\n': out.append("\\n"); break;
                    case '\r': out.append("\\r"); break;
                    case '\t': out.append("\\t"); break;
                    default:   out.append(String.format("\\u%04x", (int) c));
                }
            }
        }
        if (clipped.length() < text.length()) {
            out.append(TRUNCATED);
        }
        return out.toString();
    }

    /**
     * C0 controls, DEL and the C1 range. Deliberately narrower than
     * {@code StringEscapeUtils.escapeJava}, which escapes everything outside
     * ASCII and would render a CJK player's name unreadable in every log line.
     */
    private static boolean isControl(final char c) {
        return c < 0x20 || (c >= 0x7F && c <= 0x9F);
    }
}
