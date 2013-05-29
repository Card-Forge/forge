package forge.game.event;

public abstract class GameEvent {

    public abstract <T> T visit(IGameEventVisitor<T> visitor);
}
