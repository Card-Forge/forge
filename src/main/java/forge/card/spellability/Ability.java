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
    // Slight hack for Pithing Needle
    private final String sourceCardName;

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
        this.sourceCardName = sourceCard.getName();
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

        CardList pithing = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        pithing.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield));
        pithing = pithing.getName("Pithing Needle");
        pithing = pithing.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return c.getSVar("PithingTarget").equals(Ability.this.sourceCardName);
            }
        });

        return AllZoneUtil.isCardInPlay(this.getSourceCard()) && !this.getSourceCard().isFaceDown()
                && !this.getSourceCard().getName().equals("Spreading Seas") && (pithing.size() == 0);
    }
}
