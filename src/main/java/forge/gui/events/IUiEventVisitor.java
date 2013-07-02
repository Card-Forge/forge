package forge.gui.events;

public interface IUiEventVisitor<T> {
    T visit(UiEventBlockerAssigned event);
    
}