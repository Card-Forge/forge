package forge.screens.match.arena;

import forge.Forge;
import forge.toolbox.FOptionPane;

/**
 * The list of the match keyboard shortcuts ({@link ArenaShortcuts}), opened from the corner menu and by H. One line per
 * action: its keys, then what it does.
 */
public final class ArenaShortcutSheet {
    private ArenaShortcutSheet() {
    }

    /** The sheet's text: "Space / Enter - Pass priority / OK", one action per line, in {@link ArenaShortcuts#all()} order. */
    public static String text() {
        final StringBuilder sb = new StringBuilder();
        ArenaShortcuts.Action last = null;
        for (final ArenaShortcuts.Shortcut s : ArenaShortcuts.all()) {
            if (s.action == last || s.action == ArenaShortcuts.Action.ZOOM_CARD) {
                continue; // keyLabels lists every key of the action on its first line; zoom is not wired yet
            }
            last = s.action;
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(ArenaShortcuts.keyLabels(s.action)).append("  -  ").append(s.description());
        }
        return sb.toString();
    }

    /** Shows the sheet over the match. */
    public static void open() {
        FOptionPane.showMessageDialog(text(), Forge.getLocalizer().getMessageorUseDefault("lblArenaGdxShortcuts", "Keyboard shortcuts"));
    }
}
