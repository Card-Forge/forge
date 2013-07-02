package forge.gui.events;


public abstract class UiEvent {

    public abstract <T> T visit(IUiEventVisitor<T> visitor);
}