package forge.card.spellability;

import java.util.ArrayList;

import forge.AllZone;
import forge.Card;
import forge.PlayerZone;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cost.CostPayment;

/**
 * <p>
 * SpellAbility_Requirements class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class SpellAbilityRequirements {
    private SpellAbility ability = null;
    private TargetSelection select = null;
    private CostPayment payment = null;
    private boolean isFree = false;
    private boolean skipStack = false;

    /**
     * <p>
     * Setter for the field <code>skipStack</code>.
     * </p>
     * 
     * @param bSkip
     *            a boolean.
     */
    public final void setSkipStack(final boolean bSkip) {
        this.skipStack = bSkip;
    }

    /**
     * <p>
     * setFree.
     * </p>
     * 
     * @param bFree
     *            a boolean.
     */
    public final void setFree(final boolean bFree) {
        this.isFree = bFree;
    }

    private PlayerZone fromZone = null;
    private boolean bCasting = false;

    /**
     * <p>
     * Constructor for SpellAbility_Requirements.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param ts
     *            a {@link forge.card.spellability.TargetSelection} object.
     * @param cp
     *            a {@link forge.card.cost.CostPayment} object.
     */
    public SpellAbilityRequirements(final SpellAbility sa, final TargetSelection ts, final CostPayment cp) {
        this.ability = sa;
        this.select = ts;
        this.payment = cp;
    }

    /**
     * <p>
     * fillRequirements.
     * </p>
     */
    public final void fillRequirements() {
        this.fillRequirements(false);
    }

    /**
     * <p>
     * fillRequirements.
     * </p>
     * 
     * @param skipTargeting
     *            a boolean.
     */
    public final void fillRequirements(final boolean skipTargeting) {
        if ((this.ability instanceof Spell) && !this.bCasting) {
            // remove from hand
            this.bCasting = true;
            if (!this.ability.getSourceCard().isCopiedSpell()) {
                final Card c = this.ability.getSourceCard();

                this.fromZone = AllZone.getZoneOf(c);
                this.ability.setSourceCard(AllZone.getGameAction().moveToStack(c));
            }
        }

        // freeze Stack. No abilities should go onto the stack while I'm filling
        // requirements.
        AllZone.getStack().freezeStack();

        // Skip to paying if parent ability doesn't target and has no
        // subAbilities.
        // (or trigger case where its already targeted)
        if (!skipTargeting && (this.select.doesTarget() || (this.ability.getSubAbility() != null))) {
            this.select.setRequirements(this);
            this.select.resetTargets();
            this.select.chooseTargets();
        } else {
            this.needPayment();
        }
    }

    /**
     * <p>
     * finishedTargeting.
     * </p>
     */
    public final void finishedTargeting() {
        if (this.select.isCanceled()) {
            // cancel ability during target choosing
            final Card c = this.ability.getSourceCard();
            if (this.bCasting && !c.isCopiedSpell()) { // and not a copy
                // add back to where it came from
                AllZone.getGameAction().moveTo(this.fromZone, c);
            }

            this.select.resetTargets();
            AllZone.getStack().clearFrozen();
            return;
        } else {
            this.needPayment();
        }
    }

    /**
     * <p>
     * needPayment.
     * </p>
     */
    public final void needPayment() {
        if (!this.isFree) {
            this.startPaying();
        } else {
            this.finishPaying();
        }
    }

    /**
     * <p>
     * startPaying.
     * </p>
     */
    public final void startPaying() {
        this.payment.setRequirements(this);
        this.payment.payCost();
    }

    /**
     * <p>
     * finishPaying.
     * </p>
     */
    public final void finishPaying() {
        if (this.isFree || this.payment.isAllPaid()) {
            if (this.skipStack) {
                AbilityFactory.resolve(this.ability, false);
            } else {
                this.addAbilityToStack();
            }

            this.select.resetTargets();
            AllZone.getGameAction().checkStateEffects();
        } else if (this.payment.isCanceled()) {
            final Card c = this.ability.getSourceCard();
            if (this.bCasting && !c.isCopiedSpell()) { // and not a copy
                // add back to Previous Zone
                AllZone.getGameAction().moveTo(this.fromZone, c);
            }

            if (this.select != null) {
                this.select.resetTargets();
            }

            this.ability.resetOnceResolved();
            this.payment.cancelPayment();
            AllZone.getStack().clearFrozen();
        }
    }

    /**
     * <p>
     * addAbilityToStack.
     * </p>
     */
    public final void addAbilityToStack() {
        // For older abilities that don't setStackDescription set it here
        if (this.ability.getStackDescription().equals("")) {
            final StringBuilder sb = new StringBuilder();
            sb.append(this.ability.getSourceCard().getName());
            if (this.ability.getTarget() != null) {
                final ArrayList<Object> targets = this.ability.getTarget().getTargets();
                if (targets.size() > 0) {
                    sb.append(" - Targeting ");
                    for (final Object o : targets) {
                        sb.append(o.toString()).append(" ");
                    }
                }
            }

            this.ability.setStackDescription(sb.toString());
        }

        AllZone.getHumanPlayer().getManaPool().clearPay(this.ability, false);
        AllZone.getStack().addAndUnfreeze(this.ability);
    }
}
