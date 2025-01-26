package forge.game.event;

public class GameEventSpeedUp extends GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
