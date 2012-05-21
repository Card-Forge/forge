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
package forge.card.cost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import forge.AllZone;
import forge.Card;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRequirements;
import forge.game.player.Player;

/**
 * <p>
 * Cost_Payment class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CostPayment {
    private Cost cost = null;
    private SpellAbility ability = null;
    private Card card = null;
    private SpellAbilityRequirements req = null;
    private boolean bCancel = false;
    private final Map<CostPart, Boolean> paidCostParts = new HashMap<CostPart, Boolean>();

    /**
     * <p>
     * Getter for the field <code>cost</code>.
     * </p>
     * 
     * @return a {@link forge.card.cost.Cost} object.
     */
    public final Cost getCost() {
        return this.cost;
    }

    /**
     * <p>
     * Getter for the field <code>ability</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getAbility() {
        return this.ability;
    }

    /**
     * <p>
     * Getter for the field <code>card</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getCard() {
        return this.card;
    }

    /**
     * <p>
     * setRequirements.
     * </p>
     * 
     * @param reqs
     *            a {@link forge.card.spellability.SpellAbilityRequirements}
     *            object.
     */
    public final void setRequirements(final SpellAbilityRequirements reqs) {
        this.req = reqs;
    }

    /**
     * Gets the requirements.
     * 
     * @return the requirements
     */
    public final SpellAbilityRequirements getRequirements() {
        return this.req;
    }

    /**
     * <p>
     * setCancel.
     * </p>
     * 
     * @param cancel
     *            a boolean.
     */
    public final void setCancel(final boolean cancel) {
        this.bCancel = cancel;
    }

    /**
     * <p>
     * isCanceled.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isCanceled() {
        return this.bCancel;
    }

    /**
     * <p>
     * Constructor for Cost_Payment.
     * </p>
     * 
     * @param cost
     *            a {@link forge.card.cost.Cost} object.
     * @param abil
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public CostPayment(final Cost cost, final SpellAbility abil) {
        this.cost = cost;
        this.ability = abil;
        this.card = abil.getSourceCard();

        for (final CostPart part : cost.getCostParts()) {
            this.paidCostParts.put(part, false);
        }
    }

    /**
     * <p>
     * canPayAdditionalCosts.
     * </p>
     * 
     * @param cost
     *            a {@link forge.card.cost.Cost} object.
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean canPayAdditionalCosts(final Cost cost, final SpellAbility ability) {
        if (cost == null) {
            return true;
        }

        Player activator = ability.getActivatingPlayer();
        final Card card = ability.getSourceCard();

        if (activator == null) {
            activator = card.getController();
        }

        for (final CostPart part : cost.getCostParts()) {
            if (!part.canPay(ability, card, activator, cost)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Sets the paid mana part.
     * 
     * @param part
     *            the part
     * @param bPaid
     *            the b paid
     */
    public final void setPaidManaPart(final CostPart part, final boolean bPaid) {
        this.paidCostParts.put(part, bPaid);
    }

    /**
     * Paid cost.
     * 
     * @param part
     *            the part
     */
    public final void paidCost(final CostPart part) {
        this.setPaidManaPart(part, true);
        this.payCost();
    }

    /**
     * Cancel cost.
     */
    public final void cancelCost() {
        this.setCancel(true);
        this.req.finishPaying();
    }

    /**
     * <p>
     * payCost.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean payCost() {
        // Nothing actually ever checks this return value, is it needed?
        if (this.bCancel) {
            this.req.finishPaying();
            return false;
        }

        for (final CostPart part : this.cost.getCostParts()) {
            // This portion of the cost is already paid for, keep moving
            if (this.paidCostParts.get(part)) {
                continue;
            }

            if (!part.payHuman(this.ability, this.card, this)) {
                return false;
            }
        }

        this.resetUndoList();
        this.req.finishPaying();
        return true;
    }

    /**
     * <p>
     * isAllPaid.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isAllPaid() {
        for (final CostPart part : this.paidCostParts.keySet()) {
            if (!this.paidCostParts.get(part)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * resetUndoList.
     * </p>
     */
    public final void resetUndoList() {
        for (final CostPart part : this.paidCostParts.keySet()) {
            if (part instanceof CostPartWithList) {
                ((CostPartWithList) part).resetList();
            }
        }
    }

    /**
     * <p>
     * cancelPayment.
     * </p>
     */
    public final void cancelPayment() {
        for (final CostPart part : this.paidCostParts.keySet()) {
            if (this.paidCostParts.get(part) && part.isUndoable()) {
                part.refund(this.card);
            }
        }

        // Move this to CostMana
        AllZone.getHumanPlayer().getManaPool().refundManaPaid(this.ability, false);
    }

    /**
     * <p>
     * payComputerCosts.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean payComputerCosts() {
        // canPayAdditionalCosts now Player Agnostic

        // Just in case it wasn't set, but honestly it shouldn't have gotten
        // here without being set
        final Player activator = AllZone.getComputerPlayer();
        this.ability.setActivatingPlayer(activator);

        final Card source = this.ability.getSourceCard();
        final ArrayList<CostPart> parts = this.cost.getCostParts();

        // Set all of the decisions before attempting to pay anything
        for (final CostPart part : parts) {
            if (!part.decideAIPayment(this.ability, source, this)) {
                return false;
            }
        }

        for (final CostPart part : parts) {
            part.payAI(this.ability, this.ability.getSourceCard(), this);
        }
        return true;
    }

    /**
     * <p>
     * changeCost.
     * </p>
     */
    public final void changeCost() {
        this.cost.changeCost(this.ability);
    }
}
