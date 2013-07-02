package forge.game.event;

import forge.game.player.Player;
import forge.util.Lang;

public class GameEventShuffle extends GameEvent {
    
    public final Player player;
    
    public GameEventShuffle(Player player) {
        this.player = player;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("%s %s his/her/its library", player, Lang.joinVerb(player.getName(), "shuffle"));
    }
}
