package forge.game.event;

import forge.game.player.Player;
import forge.util.TextUtil;

public class GameEventTurnBegan extends GameEvent {

    public final Player turnOwner;
    public final int turnNumber;

    public GameEventTurnBegan(Player turnOwner, int turnNumber) {
        super();
        this.turnOwner = turnOwner;
        this.turnNumber = turnNumber;
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