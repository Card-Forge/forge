package forge.card.spellability;

import java.util.ArrayList;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Constant.Zone;
import forge.Phase;
import forge.Player;
import forge.card.cost.Cost;
import forge.card.cost.Cost_Payment;
import forge.card.staticAbility.StaticAbility;
import forge.error.ErrorViewer;

/**
 * <p>
 * Abstract Spell class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Spell extends SpellAbility implements java.io.Serializable, Cloneable {

    /** Constant <code>serialVersionUID=-7930920571482203460L</code>. */
    private static final long serialVersionUID = -7930920571482203460L;

    /**
     * <p>
     * Constructor for Spell.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     */
    public Spell(final Card sourceCard) {
        super(SpellAbility.getSpell(), sourceCard);

        this.setManaCost(sourceCard.getManaCost());
        this.setStackDescription(sourceCard.getSpellText());
        this.getRestrictions().setZone(Constant.Zone.Hand);
    }

    /**
     * <p>
     * Constructor for Spell.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param abCost
     *            a {@link forge.card.cost.Cost} object.
     * @param abTgt
     *            a {@link forge.card.spellability.Target} object.
     */
    public Spell(final Card sourceCard, final Cost abCost, final Target abTgt) {
        super(SpellAbility.getSpell(), sourceCard);

        this.setManaCost(sourceCard.getManaCost());

        this.setPayCosts(abCost);
        this.setTarget(abTgt);
        this.setStackDescription(sourceCard.getSpellText());
        this.getRestrictions().setZone(Constant.Zone.Hand);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        if (AllZone.getStack().isSplitSecondOnStack()) {
            return false;
        }

        final Card card = this.getSourceCard();

        final Player activator = this.getActivatingPlayer();

        // CantBeCast static abilities
        final CardList allp = AllZoneUtil.getCardsIn(Zone.Battlefield);
        allp.add(card);
        for (final Card ca : allp) {
            final ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.applyAbility("CantBeCast", card, activator)) {
                    return false;
                }
            }
        }

        if (card.isUnCastable()) {
            return false;
        }

        if (this.getPayCosts() != null) {
            if (!Cost_Payment.canPayAdditionalCosts(this.getPayCosts(), this)) {
                return false;
            }
        }

        if (!this.getRestrictions().canPlay(card, this)) {
            return false;
        }

        return (card.isInstant() || card.hasKeyword("Flash") || Phase.canCastSorcery(card.getController()));
    } // canPlay()

    /** {@inheritDoc} */
    @Override
    public boolean canPlayAI() {
        final Card card = this.getSourceCard();
        if (card.getSVar("NeedsToPlay").length() > 0) {
            final String needsToPlay = card.getSVar("NeedsToPlay");
            CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);

            list = list.getValidCards(needsToPlay.split(","), card.getController(), card);
            if (list.isEmpty()) {
                return false;
            }
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
    public final Object clone() {
        try {
            return super.clone();
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Spell : clone() error, " + ex);
        }
    }
}
