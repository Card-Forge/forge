package forge.gui.events;

import forge.game.event.Event;

public interface UiEvent extends Event {

    public abstract <T> T visit(IUiEventVisitor<T> visitor);
}