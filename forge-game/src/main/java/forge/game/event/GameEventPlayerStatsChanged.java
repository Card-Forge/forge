package forge.game.event;

import java.util.Arrays;
import java.util.Collection;

import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.util.Lang;
import forge.util.TextUtil;
import forge.util.collect.FCollection;

public record GameEventPlayerStatsChanged(FCollection<PlayerView> players) implements GameEvent {

    public GameEventPlayerStatsChanged(Collection<Player> players) {
        this(PlayerView.getCollection(players));
    }

    public GameEventPlayerStatsChanged(Player affected) {
        this(Arrays.asList(affected));
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
