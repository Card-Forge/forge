package forge.screens.match.arena;

import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;

/**
 * The Arena card browser (port of the Swing {@code ArenaCardBrowser}): browsing a zone, and the card choices,
 * reveals and orderings the match asks for.
 *
 * <p>Contract stub shared between workers: the battlefield's zone piles call {@link #browseZone}; the dialog
 * port fills it in. Keep this signature.</p>
 */
public final class ArenaCardBrowser {
    private ArenaCardBrowser() {
    }

    /**
     * Opens the browser on a player's zone (graveyard, exile, library if visible, ...).
     *
     * @return {@code true} if the Arena browser opened; {@code false} if the caller should fall back to the default
     *         zone display (Arena look off, or nothing to show).
     */
    public static boolean browseZone(final PlayerView player, final ZoneType zone) {
        return false;
    }
}
