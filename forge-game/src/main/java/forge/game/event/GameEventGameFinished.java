package forge.game.event;

public class GameEventGameFinished extends GameEvent {
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Game finished";
    }
} // need this class to launch after log was built via previous event