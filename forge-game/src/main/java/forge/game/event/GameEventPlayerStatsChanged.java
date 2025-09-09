package forge.game.event;

import java.util.Arrays;
import java.util.Collection;

import forge.game.player.Player;
import forge.util.Lang;
import forge.util.TextUtil;

/**
 * This means card's characteristics have changed on server, clients must re-request them
 */
public record GameEventPlayerStatsChanged(Collection<Player> players, boolean updateCards) implements GameEvent {

    public GameEventPlayerStatsChanged(Player affected, boolean updateCards) {
        this(Arrays.asList(affected), updateCards);
    }

    /* (non-Javadoc)
     * @see forge.game.event.GameEvent#visit(forge.game.event.IGameEventVisitor)
     */
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        if (null == players || players.isEmpty()) {
            return "Player state changes: (empty list)";
        }
        return TextUtil.concatWithSpace("Player state changes:", Lang.joinHomogenous(players));
    }

}
