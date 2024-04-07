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
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * This is for the "FlipCoin" Cost
 */
public class CostFlipCoin extends CostPart {

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new cost FlipCoin.
     * 
     * @param amount
     *            the amount
     */
    public CostFlipCoin(final String amount) {
        this.setAmount(amount);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        return true;
    }

    @Override
    public int paymentOrder() {
        // In a world where costs are fully undoable, determining  random information should be done last.
        return 22;
    }

    @Override
    public final String toString() {
        return Cost.convertAmountTypeToWords(this.convertAmount(), this.getAmount(), "Coin");
    }

    @Override
    public boolean payAsDecided(Player payer, PaymentDecision pd, SpellAbility sa, final boolean effect) {
        int m = FlipCoinEffect.getFlipMultiplier(payer);
        for (int i = 0; i < pd.c; i++) {
            FlipCoinEffect.flipCoinCall(payer, sa, m);
        }
        return true;
    }

    @Override
    public boolean isReusable() { return true; }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
