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
    public String getHashForLKIList() {
        return "Flipped";
    }
    @Override
    public String getHashForCardList() {
    	return "FlippedCards";
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

    @Override
    public final String toString() {
        return Cost.convertAmountTypeToWords(this.convertAmount(), this.getAmount(), "Coin");
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard) {
        final Player activator = ability.getActivatingPlayer();
        int i = FlipCoinEffect.getFilpMultiplier(activator);
        FlipCoinEffect.flipCoinCall(activator, ability, i);
        return targetCard;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
