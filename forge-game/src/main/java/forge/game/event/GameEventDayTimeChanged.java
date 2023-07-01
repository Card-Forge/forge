package forge.game.event;

public class GameEventDayTimeChanged extends GameEvent {
    public final boolean daytime;

    public GameEventDayTimeChanged(final boolean daytime) {
        this.daytime = daytime;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}