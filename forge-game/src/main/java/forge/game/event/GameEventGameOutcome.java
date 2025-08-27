package forge.game.event;

import java.util.Collection;

import forge.game.GameOutcome;

public class GameEventGameOutcome extends GameEvent {
    public final GameOutcome result;
    public final Collection<GameOutcome> history;

    public GameEventGameOutcome(GameOutcome lastOne, Collection<GameOutcome> history) {
        this.result = lastOne;
        this.history = history;
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
        return "Game Outcome: " + result.getOutcomeStrings();
    }
}