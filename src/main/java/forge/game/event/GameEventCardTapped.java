package forge.game.event;

public class GameEventCardTapped extends GameEvent {
    public final boolean Tapped;

    public GameEventCardTapped(boolean tapped) {
        Tapped = tapped;
    }
    
    
    @Override
    public <T, U> U visit(IGameEventVisitor<T, U> visitor, T params) {
        return visitor.visit(this, params);
    }
}
