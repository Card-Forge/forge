package forge.game.event;

public class GameEventRandomLog extends GameEvent {

    public final String message;

    public GameEventRandomLog(String message) {
        this.message = message;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
