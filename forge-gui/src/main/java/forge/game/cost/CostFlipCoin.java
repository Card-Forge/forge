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

import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.FlipCoinEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * This is for the "FlipCoin" Cost
 */
public class CostFlipCoin extends CostPartWithList {

    /**
     * Instantiates a new cost FlipCoin.
     * 
     * @param amount
     *            the amount
     */
    public CostFlipCoin(final String amount) {
        this.setAmount(amount);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForList() {
        return "Flipped";
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
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final Player activator) {
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        final Card source = ability.getSourceCard();

        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = Cost.chooseXValue(source, ability, this.getList().size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        for (int i = 0; i < c; i++) {
            executePayment(ability, source);
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        return Cost.convertAmountTypeToWords(this.convertAmount(), this.getAmount(), "Coin");
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected void doPayment(SpellAbility ability, Card targetCard) {
        final Player activator = ability.getActivatingPlayer();
        int i = FlipCoinEffect.getFilpMultiplier(activator);
        FlipCoinEffect.flipCoinCall(activator, ability, i);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPart#decideAIPayment(forge.game.player.AIPlayer, forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    public PaymentDecision decideAIPayment(Player ai, SpellAbility ability, Card source) {
        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(this.getAmount());
            // Generalize this
            if (sVar.equals("XChoice")) {
                return null;
            }
            c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }
        return new PaymentDecision(c);
    }
    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payAI(final PaymentDecision decision, final Player ai, SpellAbility ability, Card source) {
        int n = FlipCoinEffect.getFilpMultiplier(ai);
        for (int i = 0; i < decision.c; i++) {
            FlipCoinEffect.flipCoinCall(ai, ability, n);
        }
        return true;
    }
}
