package forge.game.event;

public abstract class GameEvent {

    public abstract <T,U> U visit(IGameEventVisitor<T, U> visitor, T params);
}
