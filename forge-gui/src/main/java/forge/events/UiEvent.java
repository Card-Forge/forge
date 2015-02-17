package forge.events;

import forge.game.event.Event;

public abstract class UiEvent extends Event {

    public abstract <T> T visit(IUiEventVisitor<T> visitor);
}