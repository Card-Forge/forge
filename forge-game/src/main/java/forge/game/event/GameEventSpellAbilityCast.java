package forge.game.event;

import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventSpellAbilityCast extends GameEvent {

    public final SpellAbility sa; 
    public final SpellAbilityStackInstance si;
    public final int stackIndex;

    public GameEventSpellAbilityCast(SpellAbility sp, SpellAbilityStackInstance si, int stackIndex) {
        sa = sp;
        this.si = si;
        this.stackIndex = stackIndex;
    }

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
