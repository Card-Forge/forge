package forge.game.event;

import java.io.Serializable;

public interface GameEvent extends Event, Serializable {

    <T> T visit(IGameEventVisitor<T> visitor);
}
