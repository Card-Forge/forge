package forge.card.spellability;

import java.util.ArrayList;

import forge.AllZone;
import forge.Card;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cost.Cost_Payment;

/**
 * <p>
 * SpellAbility_Requirements class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class SpellAbility_Requirements {
    private SpellAbility ability = null;
    private Target_Selection select = null;
    private Cost_Payment payment = null;
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
        skipStack = bSkip;
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
        isFree = bFree;
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
     *            a {@link forge.card.spellability.Target_Selection} object.
     * @param cp
     *            a {@link forge.card.cost.Cost_Payment} object.
     */
    public SpellAbility_Requirements(final SpellAbility sa, final Target_Selection ts, final Cost_Payment cp) {
        ability = sa;
        select = ts;
        payment = cp;
    }

    /**
     * <p>
     * fillRequirements.
     * </p>
     */
    public final void fillRequirements() {
        fillRequirements(false);
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
        if (ability instanceof Spell && !bCasting) {
            // remove from hand
            bCasting = true;
            if (!ability.getSourceCard().isCopiedSpell()) {
                Card c = ability.getSourceCard();

                fromZone = AllZone.getZoneOf(c);
                AllZone.getGameAction().moveToStack(c);
            }
        }

        // freeze Stack. No abilities should go onto the stack while I'm filling
        // requirements.
        AllZone.getStack().freezeStack();

        // Skip to paying if parent ability doesn't target and has no
        // subAbilities.
        // (or trigger case where its already targeted)
        if (!skipTargeting && (select.doesTarget() || ability.getSubAbility() != null)) {
            select.setRequirements(this);
            select.resetTargets();
            select.chooseTargets();
        } else {
            needPayment();
        }
    }

    /**
     * <p>
     * finishedTargeting.
     * </p>
     */
    public final void finishedTargeting() {
        if (select.isCanceled()) {
            // cancel ability during target choosing
            Card c = ability.getSourceCard();
            if (bCasting && !c.isCopiedSpell()) { // and not a copy
                // add back to where it came from
                AllZone.getGameAction().moveTo(fromZone, c);
            }

            select.resetTargets();
            AllZone.getStack().clearFrozen();
            return;
        } else {
            needPayment();
        }
    }

    /**
     * <p>
     * needPayment.
     * </p>
     */
    public final void needPayment() {
        if (!isFree) {
            startPaying();
        } else {
            finishPaying();
        }
    }

    /**
     * <p>
     * startPaying.
     * </p>
     */
    public final void startPaying() {
        payment.setRequirements(this);
        payment.payCost();
    }

    /**
     * <p>
     * finishPaying.
     * </p>
     */
    public final void finishPaying() {
        if (isFree || payment.isAllPaid()) {
            if (skipStack) {
                AbilityFactory.resolve(ability, false);
            } else {
                addAbilityToStack();
            }

            select.resetTargets();
            AllZone.getGameAction().checkStateEffects();
        } else if (payment.isCanceled()) {
            Card c = ability.getSourceCard();
            if (bCasting && !c.isCopiedSpell()) { // and not a copy
                // add back to Previous Zone
                AllZone.getGameAction().moveTo(fromZone, c);
            }

            if (select != null) {
                select.resetTargets();
            }

            ability.resetOnceResolved();
            payment.cancelPayment();
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
        if (ability.getStackDescription().equals("")) {
            StringBuilder sb = new StringBuilder();
            sb.append(ability.getSourceCard().getName());
            if (ability.getTarget() != null) {
                ArrayList<Object> targets = ability.getTarget().getTargets();
                if (targets.size() > 0) {
                    sb.append(" - Targeting ");
                    for (Object o : targets) {
                        sb.append(o.toString()).append(" ");
                    }
                }
            }

            ability.setStackDescription(sb.toString());
        }

        AllZone.getHumanPlayer().getManaPool().clearPay(ability, false);
        AllZone.getStack().addAndUnfreeze(ability);
    }
}
