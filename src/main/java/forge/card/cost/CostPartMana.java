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

import forge.Card;
import forge.FThreads;
import forge.card.ability.AbilityUtils;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputPayManaOfCostPayment;
import forge.control.input.InputPayManaX;
import forge.control.input.InputPayment;
import forge.game.GameState;
import forge.game.ai.ComputerUtilMana;
import forge.game.player.AIPlayer;

/**
 * The mana component of any spell or ability cost
 */
public class CostPartMana extends CostPart {
    // "Leftover"
    private final ManaCost cost;
    private ManaCost adjustedCost;
    private boolean xCantBe0 = false;

    /**
     * Gets the mana.
     * 
     * @return the mana
     */
    public final ManaCost getMana() {
        // Only used for Human to pay for non-X cost first
        return this.cost;
    }

    /**
     * Checks for no x mana cost.
     * 
     * @return true, if successful
     */
    public final boolean hasNoXManaCost() {
        return getAmountOfX() == 0;
    }

    /**
     * Gets the x mana.
     * 
     * @return the x mana
     */
    public final int getAmountOfX() {
        return this.cost.getShardCount(ManaCostShard.X);
    }

    /**
     * @return the xCantBe0
     */
    public boolean canXbe0() {
        return !xCantBe0;
    }

    /**
     * @param xCantBe00 the xCantBe0 to set
     */
    public void setxCantBe0(boolean xCantBe0) {
        this.xCantBe0 = xCantBe0; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the mana to pay.
     * 
     * @return the mana to pay
     */
    public final ManaCost getManaToPay() {
        // Only used for Human to pay for non-X cost first
        if (this.adjustedCost != null ) {
            return this.adjustedCost;
        }

        return this.cost;
    }
    
    @Override
    public boolean isReusable() { return true; }

    @Override
    public boolean isUndoable() { return true; }
    
    /**
     * Instantiates a new cost mana.
     * 
     * @param mana
     *            the mana
     * @param amount
     *            the amount
     * @param xCantBe0 TODO
     */
    public CostPartMana(final ManaCost cost, boolean xCantBe0) {
        this.cost = cost;
        this.setxCantBe0(xCantBe0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        return cost.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability) {
        // For now, this will always return true. But this should probably be
        // checked at some point
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#payAI(forge.card.cost.PaymentDecision, forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public void payAI(PaymentDecision decision, AIPlayer ai, SpellAbility ability, Card source) {
        ComputerUtilMana.payManaCost(ai, ability);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final GameState game) {
        final Card source = ability.getSourceCard();
        int manaToAdd = 0;
        if (!this.hasNoXManaCost()) {
            // if X cost is a defined value, other than xPaid
            if (!ability.getSVar("X").equals("Count$xPaid")) {
                // this currently only works for things about Targeted object
                manaToAdd += AbilityUtils.calculateAmount(source, "X", ability) * this.getAmountOfX();
            }
        }


        if (!"0".equals(this.getManaToPay()) || manaToAdd > 0) {
            InputPayment inpPayment = new InputPayManaOfCostPayment(game, this, ability, manaToAdd);
            FThreads.setInputAndWait(inpPayment);
            if(!inpPayment.isPaid())
                return false;
        } 
        if (this.getAmountOfX() > 0) {
            if( !ability.isAnnouncing("X") ) {
                source.setXManaCostPaid(0);
                InputPayment inpPayment = new InputPayManaX(ability, this.getAmountOfX(), this.canXbe0());
                FThreads.setInputAndWait(inpPayment);
                if(!inpPayment.isPaid())
                    return false;
            } else {
                int x = AbilityUtils.calculateAmount(source, "X", ability);
                source.setXManaCostPaid(x);
            }
        }
        return true;

    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(AIPlayer ai, SpellAbility ability, Card source) {
        return new PaymentDecision(0);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param manaCost
     */
    public void setAdjustedMana(ManaCost manaCost) {
        // this is set when static effects of LodeStone Golems or Thalias are applied
        adjustedCost = manaCost;
    }
}
