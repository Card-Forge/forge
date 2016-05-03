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
package forge.game.cost;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Cost_Payment class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CostPayment {
    private final Cost cost;
    private final SpellAbility ability;
    private final List<CostPart> paidCostParts = new ArrayList<CostPart>();

    /**
     * <p>
     * Getter for the field <code>cost</code>.
     * </p>
     * 
     * @return a {@link forge.game.cost.Cost} object.
     */
    public final Cost getCost() {
        return this.cost;
    }

    /**
     * <p>
     * Constructor for Cost_Payment.
     * </p>
     * 
     * @param cost
     *            a {@link forge.game.cost.Cost} object.
     * @param abil
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public CostPayment(final Cost cost, final SpellAbility abil) {
        this.cost = cost;
        this.ability = abil;
    }

    /**
     * <p>
     * canPayAdditionalCosts.
     * </p>
     * 
     * @param cost
     *            a {@link forge.game.cost.Cost} object.
     * @param ability
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean canPayAdditionalCosts(final Cost cost, final SpellAbility ability) {
        if (cost == null) {
            return true;
        }

        final Card card = ability.getHostCard();

        Player activator = ability.getActivatingPlayer();
        if (activator == null) {
            activator = card.getController();
        }

        for (final CostPart part : cost.getCostParts()) {
            if (!part.canPay(ability)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * isAllPaid.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isFullyPaid() {
        for (final CostPart part : this.cost.getCostParts()) {
            if (!this.paidCostParts.contains(part)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * cancelPayment.
     * </p>
     */
    public final void refundPayment() {
        Card sourceCard = this.ability.getHostCard();
        for (final CostPart part : this.paidCostParts) {
            if (part.isUndoable()) {
                part.refund(sourceCard);
            }
        }

        // Move this to CostMana
        this.ability.getActivatingPlayer().getManaPool().refundManaPaid(this.ability);
    }

    public boolean payCost(final CostDecisionMakerBase decisionMaker) {
    	final List<CostPart> costParts = this.getCost().getCostPartsWithZeroMana();
        for (final CostPart part : costParts) {
            // Wrap the cost and push onto the cost stack
            decisionMaker.getPlayer().getGame().costPaymentStack.push(new IndividualCostPaymentInstance(part));

            PaymentDecision pd = part.accept(decisionMaker);

            if (pd == null || !part.payAsDecided(decisionMaker.getPlayer(), pd, ability)) {
                decisionMaker.getPlayer().getGame().costPaymentStack.pop(); // cost is resolved
                return false;
            }
            this.paidCostParts.add(part);

            decisionMaker.getPlayer().getGame().costPaymentStack.pop(); // cost is resolved
        }

        // this clears lists used for undo. 
        for (final CostPart part1 : this.paidCostParts) {
            if (part1 instanceof CostPartWithList) {
                ((CostPartWithList) part1).resetLists();
            }
        }

        return true;
    }

    public final boolean payComputerCosts(final CostDecisionMakerBase decisionMaker) {
        // Just in case it wasn't set, but honestly it shouldn't have gotten
        // here without being set
        if (this.ability.getActivatingPlayer() == null) {
            this.ability.setActivatingPlayer(decisionMaker.getPlayer());
        }

        Map<CostPart, PaymentDecision> decisions = new HashMap<CostPart, PaymentDecision>();
        
        // Set all of the decisions before attempting to pay anything
        for (final CostPart part : this.cost.getCostParts()) {
            PaymentDecision decision = part.accept(decisionMaker);
            if (null == decision) return false;

            // wrap the payment and push onto the cost stack
            decisionMaker.getPlayer().getGame().costPaymentStack.push(new IndividualCostPaymentInstance(part));
            if (decisionMaker.paysRightAfterDecision() && !part.payAsDecided(decisionMaker.getPlayer(), decision, ability)) {
                decisionMaker.getPlayer().getGame().costPaymentStack.pop(); // cost is resolved
                return false;
            }

            decisionMaker.getPlayer().getGame().costPaymentStack.pop(); // cost is either paid or deferred
            decisions.put(part, decision);
        }

        for (final CostPart part : this.cost.getCostParts()) {
            // wrap the payment and push onto the cost stack
            decisionMaker.getPlayer().getGame().costPaymentStack.push(new IndividualCostPaymentInstance(part));

            if (!part.payAsDecided(decisionMaker.getPlayer(), decisions.get(part), this.ability)) {
                decisionMaker.getPlayer().getGame().costPaymentStack.pop(); // cost is resolved
                return false;
            }
            // abilities care what was used to pay for them
            if( part instanceof CostPartWithList ) {
                ((CostPartWithList) part).resetLists();
            }

            decisionMaker.getPlayer().getGame().costPaymentStack.pop(); // cost is resolved
        }
        return true;
    }
}
