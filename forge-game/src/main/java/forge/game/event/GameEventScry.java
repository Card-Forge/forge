package forge.game.event;

import forge.game.player.Player;

public class GameEventScry extends GameEvent {
    
    public final Player player;
    public final int toTop, toBottom;
    
    public GameEventScry(Player player, int toTop, int toBottom) {
        this.player = player;
        this.toTop = toTop;
        this.toBottom = toBottom;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

