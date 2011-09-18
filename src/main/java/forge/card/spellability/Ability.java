package forge.card.spellability;

import com.esotericsoftware.minlog.Log;
import forge.*;
import forge.Constant.Zone;


/**
 * <p>Abstract Ability class.</p>
 *
 * @author Forge
 * @version $Id$
 */
abstract public class Ability extends SpellAbility {
    //Slight hack for Pithing Needle
    private String sourceCardName;

    /**
     * <p>Constructor for Ability.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     * @param manaCost a {@link java.lang.String} object.
     */
    public Ability(Card sourceCard, String manaCost) {
        super(SpellAbility.Ability, sourceCard);
        setManaCost(manaCost);
        sourceCardName = sourceCard.getName();
    }

    /**
     * <p>Constructor for Ability.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     * @param manaCost a {@link java.lang.String} object.
     * @param stackDescription a {@link java.lang.String} object.
     */
    public Ability(Card sourceCard, String manaCost, String stackDescription) {
        this(sourceCard, manaCost);
        setStackDescription(stackDescription);
        Log.debug("an ability is being played from" + sourceCard.getName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        if (AllZone.getStack().isSplitSecondOnStack()) return false;

        CardList pithing = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        pithing.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield));
        pithing = pithing.getName("Pithing Needle");
        pithing = pithing.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.getSVar("PithingTarget").equals(sourceCardName);
            }
        });

        return AllZoneUtil.isCardInPlay(getSourceCard()) && !getSourceCard().isFaceDown() && getSourceCard().getName().equals("Spreading Seas") == false && pithing.size() == 0;
    }
}
