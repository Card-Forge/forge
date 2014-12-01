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
    public final boolean replicate;
    
    public GameEventSpellAbilityCast(SpellAbility sp, SpellAbilityStackInstance si, boolean replicate) {
        sa = sp;
        this.si = si;
        this.replicate = replicate;
    }

    /* (non-Javadoc)
     * @see forge.game.event.GameEvent#visit(forge.game.event.IGameEventVisitor)
     */
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
