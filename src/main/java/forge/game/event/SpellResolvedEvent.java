package forge.game.event;

import forge.Card;
import forge.card.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class SpellResolvedEvent extends Event{

    final public Card Source;
    final public SpellAbility Spell;

    /**
     * TODO: Write javadoc for Constructor.
     * @param source
     * @param sa
     */
    public SpellResolvedEvent(Card source, SpellAbility sa) {
        // TODO Auto-generated constructor stub
        Source = source;
        Spell = sa;
    }

    
    
}
