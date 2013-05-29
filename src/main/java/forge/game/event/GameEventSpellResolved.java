package forge.game.event;

import forge.Card;
import forge.card.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventSpellResolved extends GameEvent {

    public final Card Source;
    public final SpellAbility Spell;

    /**
     * TODO: Write javadoc for Constructor.
     * @param source
     * @param sa
     */
    public GameEventSpellResolved(Card source, SpellAbility sa) {
        // TODO Auto-generated constructor stub
        Source = source;
        Spell = sa;
    }


}
