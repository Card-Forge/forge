package forge.game.event;

import java.io.Serializable;

public interface GameEvent extends Event, Serializable {

    public abstract <T> T visit(IGameEventVisitor<T> visitor);
}
