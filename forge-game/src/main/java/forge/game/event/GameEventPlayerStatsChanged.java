package forge.game.event;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.Iterables;

import forge.game.player.Player;
import forge.util.Lang;

/**
 * This means card's characteristics have changed on server, clients must re-request them
 */
public class GameEventPlayerStatsChanged extends GameEvent {

    public final Collection<Player> players;
    public GameEventPlayerStatsChanged(Player affected) {
        players = Arrays.asList(affected);
    }

    public GameEventPlayerStatsChanged(Collection<Player> affected) {
        players = affected;
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
        return String.format("Player state changes: %s", Lang.joinHomogenous(players));
    }

}
