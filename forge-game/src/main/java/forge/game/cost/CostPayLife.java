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
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The Class CostPayLife.
 */
public class CostPayLife extends CostPart {
    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new cost pay life.
     *
     * @param amount
     *            the amount
     */
    public CostPayLife(final String amount, final String description) {
        super(amount, "card", description);
    }

    @Override
    public int paymentOrder() { return 7; }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Pay ");
        String desc = this.getTypeDescription();
        if (desc != null) {
            sb.append(desc);
        } else {
            sb.append(this.getAmount()).append(" life");
        }
        return sb.toString();
    }

    @Override
    public Integer getMaxAmountX(SpellAbility ability, Player payer) {
        if (!payer.canPayLife(1)) {
            return 0;
        }
        return payer.getLife();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer) {
        Integer amount = this.convertAmount();
        if (amount == null) { // try to calculate when it's defined.
            amount = AbilityUtils.calculateAmount(ability.getHostCard(), getAmount(), ability);
            // CR 107.1b
            if (getAmount().contains("/Half")) {
                amount = Math.max(amount, 0);
            }
        }

        if (amount != null && !payer.canPayLife(amount)) {
            return false;
        }

        if (!ability.isTrigger() && payer.hasKeyword("You can't pay life to cast spells or activate abilities.")) {
            return false;
        }

        return true;
    }

    @Override
    public boolean payAsDecided(Player ai, PaymentDecision decision, SpellAbility ability) {
        return ai.payLife(decision.c, null);
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
