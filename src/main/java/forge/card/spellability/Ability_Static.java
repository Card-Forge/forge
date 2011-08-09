package forge.card.spellability;


import forge.Card;

/**
 * <p>Abstract Ability_Static class.</p>
 *
 * @author Forge
 * @version $Id$
 */
abstract public class Ability_Static extends Ability {
    /**
     * <p>Constructor for Ability_Static.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     * @param manaCost a {@link java.lang.String} object.
     */
    public Ability_Static(Card sourceCard, String manaCost) {
        super(sourceCard, manaCost);
    }
}
