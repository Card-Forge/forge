package forge.game.event;

import forge.card.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventSpellAbilityCast extends GameEvent {

    public final SpellAbility sa; 
    public final boolean replicate;
    
    public GameEventSpellAbilityCast(SpellAbility sp, boolean replicate) {
        sa = sp;
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
