package forge.card.spellability;

import forge.AllZone;
import forge.Card;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;

import java.util.ArrayList;

/**
 * <p>SpellAbility_Requirements class.</p>
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
     * <p>Setter for the field <code>skipStack</code>.</p>
     *
     * @param bSkip a boolean.
     */
    public void setSkipStack(boolean bSkip) {
        skipStack = bSkip;
    }

    /**
     * <p>setFree.</p>
     *
     * @param bFree a boolean.
     */
    public void setFree(boolean bFree) {
        isFree = bFree;
    }

    private PlayerZone fromZone = null;
    private boolean bCasting = false;

    /**
     * <p>Constructor for SpellAbility_Requirements.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param ts a {@link forge.card.spellability.Target_Selection} object.
     * @param cp a {@link forge.card.spellability.Cost_Payment} object.
     */
    public SpellAbility_Requirements(SpellAbility sa, Target_Selection ts, Cost_Payment cp) {
        ability = sa;
        select = ts;
        payment = cp;
    }

    /**
     * <p>fillRequirements.</p>
     */
    public void fillRequirements() {
        fillRequirements(false);
    }

    /**
     * <p>fillRequirements.</p>
     *
     * @param skipTargeting a boolean.
     */
    public void fillRequirements(boolean skipTargeting) {
        if (ability instanceof Spell && !bCasting) {
            // remove from hand
            bCasting = true;
            if (!ability.getSourceCard().isCopiedSpell()) {
                Card c = ability.getSourceCard();

                fromZone = AllZone.getZone(c);
                AllZone.getGameAction().moveToStack(c);
            }
        }

        // freeze Stack. No abilities should go onto the stack while I'm filling requirements.
        AllZone.getStack().freezeStack();

        // Skip to paying if parent ability doesn't target and has no subAbilities. (or trigger case where its already targeted)
        if (!skipTargeting && (select.doesTarget() || ability.getSubAbility() != null)) {
            select.setRequirements(this);
            select.resetTargets();
            select.chooseTargets();
        } else
            needPayment();
    }

    /**
     * <p>finishedTargeting.</p>
     */
    public void finishedTargeting() {
        if (select.isCanceled()) {
            // cancel ability during target choosing
            Card c = ability.getSourceCard();
            if (bCasting && !c.isCopiedSpell()) {    // and not a copy
                // add back to where it came from
                AllZone.getGameAction().moveTo(fromZone, c);
            }

            select.resetTargets();
            AllZone.getStack().clearFrozen();
            return;
        } else
            needPayment();
    }

    /**
     * <p>needPayment.</p>
     */
    public void needPayment() {
        if (!isFree)
            startPaying();
        else
            finishPaying();
    }

    /**
     * <p>startPaying.</p>
     */
    public void startPaying() {
        payment.setRequirements(this);
        payment.payCost();
    }

    /**
     * <p>finishPaying.</p>
     */
    public void finishPaying() {
        if (isFree || payment.isAllPaid()) {
            if (skipStack)
                AbilityFactory.resolve(ability, false);
            else
                addAbilityToStack();

            select.resetTargets();
            AllZone.getGameAction().checkStateEffects();
        } else if (payment.isCanceled()) {
            Card c = ability.getSourceCard();
            if (bCasting && !c.isCopiedSpell()) {    // and not a copy
                // add back to Previous Zone
                AllZone.getGameAction().moveTo(fromZone, c);
            }

            if (select != null)
                select.resetTargets();

            payment.cancelPayment();
            AllZone.getStack().clearFrozen();
        }
    }

    /**
     * <p>addAbilityToStack.</p>
     */
    public void addAbilityToStack() {
        // For older abilities that don't setStackDescription set it here
        if (ability.getStackDescription().equals("")) {
            StringBuilder sb = new StringBuilder();
            sb.append(ability.getSourceCard().getName());
            if (ability.getTarget() != null) {
                ArrayList<Object> targets = ability.getTarget().getTargets();
                if (targets.size() > 0) {
                    sb.append(" - Targeting ");
                    for (Object o : targets)
                        sb.append(o.toString()).append(" ");
                }
            }

            ability.setStackDescription(sb.toString());
        }

        AllZone.getManaPool().clearPay(ability, false);
        AllZone.getStack().addAndUnfreeze(ability);
    }
}
