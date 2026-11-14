package forge.game.event;

import java.io.Serializable;

public interface GameEvent extends Event, Serializable {

    <T> T visit(IGameEventVisitor<T> visitor);

    /**
     * True for an event that tells a GUI that something it shows, such as a zone or a card's stats,
     * may be out of date and should be drawn again. It often fires when nothing has changed, and a
     * real change is reported by another event too, so on its own it does not mark a step of the game.
     */
    default boolean isRedrawHint() {
        return false;
    }
}
