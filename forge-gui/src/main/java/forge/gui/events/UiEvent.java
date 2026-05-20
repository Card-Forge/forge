package forge.gui.events;

import forge.game.event.Event;

public interface UiEvent extends Event {

    <T> T visit(IUiEventVisitor<T> visitor);
}