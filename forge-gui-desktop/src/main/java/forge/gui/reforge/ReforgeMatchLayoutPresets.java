/* REFORGE COMMANDER EXTENSION
 *
 * Generates canonical battlefield (match) layouts for N players and writes them
 * to the user's match.xml so SLayoutIO picks them up on the next match load.
 *
 * Additive-only: reads ForgeConstants.MATCH_LAYOUT_FILE and produces XML in the
 * exact schema readLayout() understands; it never touches any upstream class.
 */
package forge.gui.reforge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import forge.localinstance.properties.ForgeConstants;

public final class ReforgeMatchLayoutPresets {
    private ReforgeMatchLayoutPresets() { }

    public static final int MAX_PLAYERS = 8;

    public static String layoutFor(final int players) { // doc:12a DONE
        final int n = Math.max(1, Math.min(players, MAX_PLAYERS));
        final double leftW = 0.2, rightW = 0.2;
        final double bodyX = 0.2, bodyW = 0.6;
        final double handH = 0.268, bodyH = 1.0 - handH;

        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<layout serial=\"\">\n");

        // Left rail (unchanged from the stock 2-player match.xml)
        railCell(sb, 0.0, 0.0, leftW, 0.617, "REPORT_STACK", "REPORT_COMBAT", "REPORT_LOG", "REPORT_DEPENDENCIES");
        railCell(sb, 0.0, 0.617, leftW, 0.115, "BUTTON_DOCK");
        railCell(sb, 0.0, 0.732, leftW, 0.268, "REPORT_MESSAGE", "DEV_MODE");

        // Body: all N battlefields stacked across the 0..bodyH band (covers 2..8).
        final double fieldH = bodyH / n;
        for (int i = 0; i < n; i++) {
            railCell(sb, bodyX, i * fieldH, bodyW, fieldH, "FIELD_" + i);
        }
        railCell(sb, bodyX, bodyH, bodyW, handH, "HAND_0");

        // Right rail: card detail + picture
        railCell(sb, 0.8, 0.0, rightW, 0.466, "CARD_DETAIL");
        railCell(sb, 0.8, 0.466, rightW, 0.534, "CARD_PICTURE");

        sb.append("</layout>\n");
        return sb.toString();
    }

    private static void railCell(final StringBuilder sb, final double x, final double y,
                                 final double w, final double h, final String... docs) {
        sb.append("\t<cell x=\"").append(x).append("\" y=\"").append(y)
          .append("\" w=\"").append(w).append("\" h=\"").append(h).append("\">\n");
        for (final String d : docs) {
            sb.append("\t\t<doc>").append(d).append("</doc>\n");
        }
        sb.append("\t</cell>\n");
    }

    /** Write the N-player canonical layout into the user's match layout file. */
    public static void apply(final int players) throws IOException {
        final Path dest = Path.of(ForgeConstants.MATCH_LAYOUT_FILE.userPrefLoc);
        Files.createDirectories(dest.toAbsolutePath().getParent());

        // Write to temporary file, then atomically replace destination
        final Path temp = dest.resolveSibling(dest.getFileName() + ".tmp");
        try {
            Files.writeString(temp, layoutFor(players));
            try {
                Files.move(temp, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                                       java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (final java.nio.file.AtomicMoveNotSupportedException ex) {
                // Fallback: non-atomic replacement if atomic move unsupported
                Files.move(temp, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final IOException ex) {
            // Clean up temp file on failure
            Files.deleteIfExists(temp);
            throw ex;
        }
    }

    /** Delete the user's match layout so the stock 2-player default is used again. */
    public static void restoreDefault() throws IOException {
        Files.deleteIfExists(Path.of(ForgeConstants.MATCH_LAYOUT_FILE.userPrefLoc));
    }
}