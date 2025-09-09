package forge.game.event;

import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;

public record GameEventSpellAbilityCast(SpellAbility sa, SpellAbilityStackInstance si, int stackIndex) implements GameEvent {

    /* (non-Javadoc)
     * @see forge.game.event.GameEvent#visit(forge.game.event.IGameEventVisitor)
     */
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + sa.getActivatingPlayer() + (sa.isSpell() ? " cast " : sa.isActivatedAbility() ? " activated " : " triggered ") + sa;
    }
}
