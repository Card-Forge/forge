package forge.toolbox;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rules text for the Arena overlays: word wrapping that keeps mana symbols such as
 * {2} or {W/U} as icons the height of the text, and drawing the wrapped lines.
 */
public final class ArenaText {
    private static final Pattern TOKEN = Pattern.compile("\\{[^}]+\\}|\\s+|[^\\s{]+");

    private ArenaText() { }

    /** "{W/U}" becomes "WU", the key mana images go by; null for plain text or symbols there is no image for. */
    public static String symbolKey(final String token) {
        if (token.length() < 3 || token.charAt(0) != '{' || token.charAt(token.length() - 1) != '}') {
            return null;
        }
        final String key = token.substring(1, token.length() - 1).replace("/", "");
        return CardFaceSymbols.hasSymbol(key) ? key : null;
    }

    /** Word-wraps text that may hold mana symbols, keeping its own line breaks. */
    public static List<List<String>> wrap(final String text, final FontMetrics fm, final int width) {
        final int symbolWidth = fm.getAscent() + 1;
        final List<List<String>> lines = new ArrayList<>();
        for (final String paragraph : text.split("\\r?\\n")) {
            List<String> line = new ArrayList<>();
            int lineW = 0;
            final Matcher m = TOKEN.matcher(paragraph.trim());
            while (m.find()) {
                final String token = m.group();
                final boolean space = token.isBlank();
                final int tw = symbolKey(token) != null ? symbolWidth : fm.stringWidth(space ? " " : token);
                if (!space && lineW + tw > width && !line.isEmpty()) {
                    lines.add(line);
                    line = new ArrayList<>();
                    lineW = 0;
                }
                if (space && line.isEmpty()) {
                    continue;
                }
                line.add(space ? " " : token);
                lineW += tw;
            }
            lines.add(line);
        }
        return lines;
    }

    /**
     * Draws wrapped lines from {@code y} down with the graphics' current font and colour,
     * stopping with an ellipsis before {@code maxY}. Returns the y below the last line.
     */
    public static int draw(final Graphics2D g, final List<List<String>> lines, final int x, final int y, final int maxY) {
        final FontMetrics fm = g.getFontMetrics();
        final int lineH = fm.getHeight();
        final int symbol = fm.getAscent();
        int cy = y;
        for (final List<String> line : lines) {
            if (cy + lineH > maxY) {
                g.drawString("…", x, cy + fm.getAscent());
                break;
            }
            int lx = x;
            for (final String token : line) {
                final String key = symbolKey(token);
                if (key != null) {
                    CardFaceSymbols.drawSymbol(key, g, lx, cy + (lineH - symbol) / 2, symbol);
                    lx += symbol + 1;
                } else {
                    g.drawString(token, lx, cy + fm.getAscent());
                    lx += fm.stringWidth(token);
                }
            }
            cy += lineH;
        }
        return cy;
    }

    /** Width one wrapped line takes. */
    public static int width(final List<String> line, final FontMetrics fm) {
        int w = 0;
        for (final String token : line) {
            w += symbolKey(token) != null ? fm.getAscent() + 1 : fm.stringWidth(token);
        }
        return w;
    }

    /** Height the wrapped lines take. */
    public static int height(final List<List<String>> lines, final FontMetrics fm) {
        return lines.size() * fm.getHeight();
    }
}
