package forge.game.event;

import forge.game.spellability.SpellAbilityView;

public record GameEventSpellRemovedFromStack(SpellAbilityView sa) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Stack removed " + sa;
    }
}
