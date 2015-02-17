package forge.screens.match;

import java.awt.event.ActionEvent;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.ForgeAction;
import forge.match.MatchConstants;
import forge.view.arcane.FloatingCardArea;

/**
 * Receives click and programmatic requests for viewing data stacks in the
 * "zones" of a player field: hand, library, etc.
 * 
 */
public class ZoneAction extends ForgeAction {
    private static final long serialVersionUID = -5822976087772388839L;
    private final CMatchUI matchUI;
    private final PlayerView player;
    private final ZoneType zone;

    /**
     * Receives click and programmatic requests for viewing data stacks in
     * the "zones" of a player field: hand, graveyard, etc. The library
     * "zone" is an exception to the rule; it's handled in DeckListAction.
     */
    public ZoneAction(final CMatchUI matchUI, final PlayerView player0, final ZoneType zone0, final MatchConstants property) {
        super(property);
        this.matchUI = matchUI;
        player = player0;
        zone = zone0;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        FloatingCardArea.show(matchUI, player, zone);
    }
}