package forge.card.spellability;

import java.util.ArrayList;

import forge.*;
import forge.Constant.Zone;
import forge.card.cost.Cost;
import forge.card.cost.Cost_Payment;
import forge.card.staticAbility.StaticAbility;
import forge.error.ErrorViewer;


/**
 * <p>Abstract Spell class.</p>
 *
 * @author Forge
 * @version $Id$
 */
abstract public class Spell extends SpellAbility implements java.io.Serializable, Cloneable {

    /** Constant <code>serialVersionUID=-7930920571482203460L</code> */
    private static final long serialVersionUID = -7930920571482203460L;

    /**
     * <p>Constructor for Spell.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     */
    public Spell(Card sourceCard) {
        super(SpellAbility.Spell, sourceCard);

        setManaCost(sourceCard.getManaCost());
        setStackDescription(sourceCard.getSpellText());
        getRestrictions().setZone(Constant.Zone.Hand);
    }

    /**
     * <p>Constructor for Spell.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     * @param abCost a {@link forge.card.cost.Cost} object.
     * @param abTgt a {@link forge.card.spellability.Target} object.
     */
    public Spell(Card sourceCard, Cost abCost, Target abTgt) {
        super(SpellAbility.Spell, sourceCard);

        setManaCost(sourceCard.getManaCost());

        setPayCosts(abCost);
        setTarget(abTgt);
        setStackDescription(sourceCard.getSpellText());
        getRestrictions().setZone(Constant.Zone.Hand);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        if (AllZone.getStack().isSplitSecondOnStack()) return false;

        Card card = getSourceCard();
        
        Player activator = getActivatingPlayer();
        
        //CantBeCast static abilities
        CardList allp = AllZoneUtil.getCardsIn(Zone.Battlefield);
        for (Card ca : allp) {
            ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (StaticAbility stAb : staticAbilities) {
                if(stAb.applyAbility("CantBeCast", card, activator)) {
                    return false;
                }
            }
        }

        if (card.isUnCastable())
            return false;

        if (payCosts != null)
            if (!Cost_Payment.canPayAdditionalCosts(payCosts, this))
                return false;

        if (!this.getRestrictions().canPlay(card, this))
            return false;

        return (card.isInstant() || card.hasKeyword("Flash") || Phase.canCastSorcery(card.getController()));
    }//canPlay()

    /** {@inheritDoc} */
    @Override
    public boolean canPlayAI() {
        Card card = getSourceCard();
        if (card.getSVar("NeedsToPlay").length() > 0) {
            String needsToPlay = card.getSVar("NeedsToPlay");
            CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);

            list = list.getValidCards(needsToPlay.split(","), card.getController(), card);
            if (list.isEmpty()) return false;
        }

        return super.canPlayAI();
    }

    /** {@inheritDoc} */
    @Override
    public String getStackDescription() {
        return super.getStackDescription();
    }

    /** {@inheritDoc} */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Spell : clone() error, " + ex);
        }
    }
}
