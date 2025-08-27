package forge.game.event;

import java.util.List;

import forge.game.card.Card;

public class GameEventCombatEnded extends GameEvent {

    public final List<Card> attackers;
    public final List<Card> blockers;

    public GameEventCombatEnded(List<Card> attackers, List<Card> blockers) {
        this.attackers = attackers;
        this.blockers = blockers;
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
        return "Combat ended. Attackers: " + attackers + " Blockers: " + blockers;
    }
}
