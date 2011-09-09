package forge.card.spellability;

import forge.*;
import forge.card.cost.Cost;
import forge.card.cost.Cost_Payment;


/**
 * <p>Abstract Ability_Activated class.</p>
 *
 * @author Forge
 * @version $Id$
 */
abstract public class Ability_Activated extends SpellAbility implements java.io.Serializable {
    /** Constant <code>serialVersionUID=1L</code> */
    private static final long serialVersionUID = 1L;

    /**
     * <p>Constructor for Ability_Activated.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param manacost a {@link java.lang.String} object.
     */
    public Ability_Activated(Card card, String manacost) {
        this(card, new Cost(manacost, card.getName(), true), null);
    }

    /**
     * <p>Constructor for Ability_Activated.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     * @param abCost a {@link forge.card.cost.Cost} object.
     * @param tgt a {@link forge.card.spellability.Target} object.
     */
    public Ability_Activated(Card sourceCard, Cost abCost, Target tgt) {
        super(SpellAbility.Ability, sourceCard);
        setManaCost(abCost.getTotalMana());
        setPayCosts(abCost);
        if (tgt != null && tgt.doesTarget())
            setTarget(tgt);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        if (AllZone.getStack().isSplitSecondOnStack()) return false;

        final Card c = getSourceCard();
        if (c.isFaceDown() && isIntrinsic()) {   // Intrinsic abilities can't be activated by face down cards
            return false;
        }
        
        if (c.hasKeyword("CARDNAME's activated abilities can't be activated.") || isSuppressed()) {
            return false;
        }

        CardList pithing = AllZoneUtil.getPlayerCardsInPlay(AllZone.getHumanPlayer());
        pithing.addAll(AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer()));
        pithing = pithing.getName("Pithing Needle");
        pithing = pithing.filter(new CardListFilter() {
            public boolean addCard(Card crd) {
                return crd.getSVar("PithingTarget").equals(c.getName());
            }
        });

        if (pithing.size() != 0 && !(this instanceof Ability_Mana)) return false;

        if (!(getRestrictions().canPlay(c, this)))
            return false;

        return Cost_Payment.canPayAdditionalCosts(payCosts, this);
    }
}
