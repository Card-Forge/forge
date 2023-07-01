package forge.game.event;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.Iterables;

import forge.game.player.Player;
import forge.util.Lang;
import forge.util.TextUtil;

/**
 * This means card's characteristics have changed on server, clients must re-request them
 */
public class GameEventPlayerStatsChanged extends GameEvent {

    public final Collection<Player> players;
    public final boolean updateCards;
    public GameEventPlayerStatsChanged(Player affected, boolean updateCards) {
        players = Arrays.asList(affected);
        this.updateCards = updateCards;
    }

    public GameEventPlayerStatsChanged(Collection<Player> affected, boolean updateCards) {
        players = affected;
        this.updateCards = updateCards;
    }

    /* (non-Javadoc)
     * @see forge.game.event.GameEvent#visit(forge.game.event.IGameEventVisitor)
     */
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        // TODO Auto-generated method stub
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        if (null == players || Iterables.isEmpty(players)) {
            return "Player state changes: (empty list)";
        }
        return TextUtil.concatWithSpace("Player state changes:", Lang.joinHomogenous(players));
    }

}
