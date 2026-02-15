package forge.game.event;

import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.util.TextUtil;

public record GameEventTurnBegan(PlayerView turnOwner, int turnNumber) implements GameEvent {

    public GameEventTurnBegan(Player turnOwner, int turnNumber) {
        this(PlayerView.get(turnOwner), turnNumber);
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
        return TextUtil.concatWithSpace("Turn", String.valueOf(turnNumber), TextUtil.enclosedParen(turnOwner.toString()));
    }
}