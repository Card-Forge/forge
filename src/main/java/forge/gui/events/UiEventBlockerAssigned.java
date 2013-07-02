package forge.gui.events;

public class UiEventBlockerAssigned extends UiEvent {

    @Override
    public <T> T visit(IUiEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
    

}