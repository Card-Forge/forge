package forge.game.event;

import forge.game.spellability.SpellAbility;

public record GameEventSpellResolved(SpellAbility spell, boolean hasFizzled) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Stack resolved " + spell + (hasFizzled ? " (fizzled)" : "");
    }
}
