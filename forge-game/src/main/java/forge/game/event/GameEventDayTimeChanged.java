package forge.game.event;

public record GameEventDayTimeChanged(boolean daytime) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}