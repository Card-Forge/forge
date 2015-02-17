package forge.game.event;

public abstract class GameEvent extends Event {

    public abstract <T> T visit(IGameEventVisitor<T> visitor);
}
