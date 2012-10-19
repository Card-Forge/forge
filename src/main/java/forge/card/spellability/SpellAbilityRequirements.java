/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.spellability;

import java.util.ArrayList;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cost.CostPayment;
import forge.game.zone.Zone;

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
    private Integer zonePosition = null;

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

    private Zone fromZone = null;
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

                this.fromZone = Singletons.getModel().getGame().getZoneOf(c);
                this.zonePosition = this.fromZone.getPosition(c);
                this.ability.setSourceCard(Singletons.getModel().getGame().getAction().moveToStack(c));
            }
        }

        // freeze Stack. No abilities should go onto the stack while I'm filling
        // requirements.
        Singletons.getModel().getGame().getStack().freezeStack();

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
                Singletons.getModel().getGame().getAction().moveTo(this.fromZone, c, this.zonePosition);
            }

            this.select.resetTargets();
            Singletons.getModel().getGame().getStack().removeFromFrozenStack(this.ability);
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
        this.payment.changeCost();
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
            Singletons.getModel().getGame().getAction().checkStateEffects();
        } else if (this.payment.isCanceled()) {
            final Card c = this.ability.getSourceCard();
            if (this.bCasting && !c.isCopiedSpell()) { // and not a copy
                // add back to Previous Zone
                Singletons.getModel().getGame().getAction().moveTo(this.fromZone, c, this.zonePosition);
            }

            if (this.select != null) {
                this.select.resetTargets();
            }

            this.ability.resetOnceResolved();
            this.payment.cancelPayment();
            Singletons.getModel().getGame().getStack().clearFrozen();
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

        this.ability.getActivatingPlayer().getManaPool().clearManaPaid(this.ability, false);
        Singletons.getModel().getGame().getStack().addAndUnfreeze(this.ability);
    }
}
