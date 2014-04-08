package forge.events;


public abstract class UiEvent {

    public abstract <T> T visit(IUiEventVisitor<T> visitor);
}