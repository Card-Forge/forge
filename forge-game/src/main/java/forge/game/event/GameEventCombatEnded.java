package forge.game.event;

import java.util.List;

import forge.game.card.Card;

public record GameEventCombatEnded(List<Card> attackers, List<Card> blockers) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Combat ended. Attackers: " + attackers + " Blockers: " + blockers;
    }
}
