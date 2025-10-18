package forge.game.event;

public interface GameEvent extends Event {

    public abstract <T> T visit(IGameEventVisitor<T> visitor);
}
