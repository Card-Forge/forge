package forge.game.event;

import forge.game.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventSpellResolved extends GameEvent {

    public final SpellAbility spell;
    public final boolean hasFizzled;

    /**
     * TODO: Write javadoc for Constructor.
     * @param source
     * @param sa
     * @param hasFizzled 
     */
    public GameEventSpellResolved(SpellAbility sa, boolean hasFizzled) {
        // TODO Auto-generated constructor stub
        this.spell = sa;
        this.hasFizzled = hasFizzled;
    }


    
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
