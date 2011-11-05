package forge.card.spellability;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.Constant.Zone;

/**
 * <p>
 * Abstract Ability class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Ability extends SpellAbility {

    /**
     * <p>
     * Constructor for Ability.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     */
    public Ability(final Card sourceCard, final String manaCost) {
        super(SpellAbility.getAbility(), sourceCard);
        this.setManaCost(manaCost);
    }

    /**
     * <p>
     * Constructor for Ability.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @param stackDescription
     *            a {@link java.lang.String} object.
     */
    public Ability(final Card sourceCard, final String manaCost, final String stackDescription) {
        this(sourceCard, manaCost);
        this.setStackDescription(stackDescription);
        Log.debug("an ability is being played from" + sourceCard.getName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        if (AllZone.getStack().isSplitSecondOnStack()) {
            return false;
        }

        return AllZoneUtil.isCardInPlay(this.getSourceCard()) && !this.getSourceCard().isFaceDown();
    }
}
