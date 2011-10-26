package forge.card.spellability;

import java.util.ArrayList;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.Constant.Zone;
import forge.Player;
import forge.card.cost.Cost;
import forge.card.cost.Cost_Payment;
import forge.card.staticAbility.StaticAbility;

/**
 * <p>
 * Abstract Ability_Activated class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Ability_Activated extends SpellAbility implements java.io.Serializable {
    /** Constant <code>serialVersionUID=1L</code>. */
    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * Constructor for Ability_Activated.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param manacost
     *            a {@link java.lang.String} object.
     */
    public Ability_Activated(final Card card, final String manacost) {
        this(card, new Cost(manacost, card.getName(), true), null);
    }

    /**
     * <p>
     * Constructor for Ability_Activated.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param abCost
     *            a {@link forge.card.cost.Cost} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     */
    public Ability_Activated(final Card sourceCard, final Cost abCost, final Target tgt) {
        super(SpellAbility.Ability, sourceCard);
        setManaCost(abCost.getTotalMana());
        setPayCosts(abCost);
        if (tgt != null && tgt.doesTarget()) {
            setTarget(tgt);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        if (AllZone.getStack().isSplitSecondOnStack()) {
            return false;
        }

        final Card c = getSourceCard();
        if (c.isFaceDown() && isIntrinsic()) { // Intrinsic abilities can't be
                                               // activated by face down cards
            return false;
        }

        Player activator = getActivatingPlayer();

        // CantBeActivated static abilities
        CardList allp = AllZoneUtil.getCardsIn(Zone.Battlefield);
        for (Card ca : allp) {
            ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (StaticAbility stAb : staticAbilities) {
                if (stAb.applyAbility("CantBeActivated", c, activator)) {
                    return false;
                }
            }
        }

        if (c.hasKeyword("CARDNAME's activated abilities can't be activated.") || isSuppressed()) {
            return false;
        }

        CardList pithing = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        pithing.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield));
        pithing = pithing.getName("Pithing Needle");
        pithing = pithing.filter(new CardListFilter() {
            public boolean addCard(final Card crd) {
                return crd.getSVar("PithingTarget").equals(c.getName());
            }
        });

        if (pithing.size() != 0 && !(this instanceof Ability_Mana)) {
            return false;
        }

        if (!(getRestrictions().canPlay(c, this))) {
            return false;
        }

        return Cost_Payment.canPayAdditionalCosts(payCosts, this);
    }
}
