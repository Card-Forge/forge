package forge.game.event;

import forge.game.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventSpellRemovedFromStack extends GameEvent {
    public final SpellAbility sa;

    public GameEventSpellRemovedFromStack(SpellAbility spellAbility) {
        sa = spellAbility;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        // TODO Auto-generated method stub
        return visitor.visit(this);
    }

}
