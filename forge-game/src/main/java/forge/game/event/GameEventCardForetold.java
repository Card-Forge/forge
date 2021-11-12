package forge.game.event;

import forge.game.player.Player;

public class GameEventCardForetold extends GameEvent {
    public final Player activatingPlayer;

    public GameEventCardForetold(Player player) {
        activatingPlayer = player;
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
        return activatingPlayer.getName()+" has foretold.";
    }
}
