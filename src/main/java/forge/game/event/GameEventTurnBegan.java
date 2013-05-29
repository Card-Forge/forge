package forge.game.event;

import forge.game.player.Player;

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
}