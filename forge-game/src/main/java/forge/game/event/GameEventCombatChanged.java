package forge.game.event;

public class GameEventCombatChanged extends GameEvent {

    public GameEventCombatChanged() {
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Combat changed";
    }
}
