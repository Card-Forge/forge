package forge.screens.match;

import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.view.arcane.FloatingCardArea;

/**
 * Receives click and programmatic requests for viewing data stacks in the
 * "zones" of a player field: hand, library, etc.
 *
 */
public final class ZoneAction implements Runnable {
    private final CMatchUI matchUI;
    private final PlayerView player;
    private final ZoneType zone;

    /**
     * Receives click and programmatic requests for viewing data stacks in
     * the "zones" of a player field: hand, graveyard, etc. The library
     * "zone" is an exception to the rule; it's handled in DeckListAction.
     */
    public ZoneAction(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        this.matchUI = matchUI;
        this.player = player;
        this.zone = zone;
    }

    @Override
    public void run() {
        FloatingCardArea.showOrHide(matchUI, player, zone);
    }
}