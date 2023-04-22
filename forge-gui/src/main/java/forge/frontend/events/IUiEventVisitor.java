package forge.frontend.events;

public interface IUiEventVisitor<T> {
    T visit(UiEventBlockerAssigned event);
    T visit(UiEventAttackerDeclared event);
    T visit(UiEventNextGameDecision event);
}