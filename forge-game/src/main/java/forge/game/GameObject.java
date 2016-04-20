package forge.game;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public abstract class GameObject {

    public boolean canBeTargetedBy(final SpellAbility sa) {
        return false;
    }
    
    /**
     * Checks if is valid.
     * 
     * @param restrictions
     *            the restrictions
     * @param sourceController
     *            the source controller
     * @param source
     *            the source
     * @param spellAbility
     * @return true, if is valid
     */
    public boolean isValid(final String[] restrictions, final Player sourceController, final Card source, SpellAbility spellAbility) {
        for (final String restriction : restrictions) {
            if (this.isValid(restriction, sourceController, source, spellAbility)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if is valid.
     * 
     * @param restriction
     *            the restriction
     * @param sourceController
     *            the source controller
     * @param source
     *            the source
     * @param spellAbility
     * @return true, if is valid
     */
    public boolean isValid(final String restriction, final Player sourceController, final Card source, SpellAbility spellAbility) {
        return false;
    }

    /**
     * Checks for property.
     * 
     * @param property
     *            the property
     * @param sourceController
     *            the source controller
     * @param source
     *            the source
     * @param spellAbility
     * @return true, if successful
     */
    public boolean hasProperty(final String property, final Player sourceController, final Card source, SpellAbility spellAbility) {
        return false;
    }
}