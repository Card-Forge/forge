package forge.game.event;

import java.util.Collection;

import forge.game.GameOutcome;

public record GameEventGameOutcome(GameOutcome result, Collection<GameOutcome> history) implements GameEvent {

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