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
import java.util.List;
import java.util.Map;
import forge.Card;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.AIPlayer;
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
    private final Cost cost;
    private final SpellAbility ability;
    private final ArrayList<CostPart> paidCostParts = new ArrayList<CostPart>();

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
            if (!part.canPay(ability)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * payCost.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean payCost(final GameState game) {
        for (final CostPart part : this.cost.getCostParts()) {
            if ( false == part.payHuman(this.ability, game) ) {
                return false;
            }
            
            // abilities care what was used to pay for them
            if( part instanceof CostPartWithList )
                ((CostPartWithList) part).reportPaidCardsTo(ability);
            
            this.paidCostParts.add(part);
        }

        // this clears lists used for undo. 
        for (final CostPart part1 : this.paidCostParts) {
            if (part1 instanceof CostPartWithList) {
                ((CostPartWithList) part1).resetList();
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
        Card sourceCard = this.ability.getSourceCard();
        for (final CostPart part : this.paidCostParts) {
            if (part.isUndoable()) {
                part.refund(sourceCard);
            }
        }

        // Move this to CostMana
        this.ability.getActivatingPlayer().getManaPool().refundManaPaid(this.ability, false);
    }

    /**
     * <p>
     * payComputerCosts.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean payComputerCosts(final AIPlayer ai, final GameState game) {
        // canPayAdditionalCosts now Player Agnostic

        // Just in case it wasn't set, but honestly it shouldn't have gotten
        // here without being set
        this.ability.setActivatingPlayer(ai);

        final Card source = this.ability.getSourceCard();
        final List<CostPart> parts = this.cost.getCostParts();

        if (this.getCost().getCostMana() == null) {
            parts.add(new CostPartMana("0", 0, false));
        }

        Map<Class<? extends CostPart>, PaymentDecision> decisions = new HashMap<Class<? extends CostPart>, PaymentDecision>();
        
        // Set all of the decisions before attempting to pay anything
        for (final CostPart part : parts) {
            PaymentDecision decision = part.decideAIPayment(ai, this.ability, source);
            if ( null == decision ) return false;
            decisions.put(part.getClass(), decision);
        }

        for (final CostPart part : parts) {
            part.payAI(decisions.get(part.getClass()), ai, this.ability, this.ability.getSourceCard());
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
