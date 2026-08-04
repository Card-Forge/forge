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

import forge.game.GameEntityCounterTable;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The Class CostDamage.
 */
public class CostPutCounterYou extends CostPart {

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;

    private CounterType type;

    public CostPutCounterYou(final String amount, CounterType type) {
        this.setAmount(amount);
        this.type = type;
    }

    @Override
    public int paymentOrder() { return 8; }

    public CounterType getCounter() {
        return type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Get ").append(Cost.convertAmountTypeToWords(convertAmount(), getAmount(), type.getName() + " counter"));
        return sb.toString();
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
        return payer.canReceiveCounters(type);
    }

    @Override
    public boolean payAsDecided(Player payer, PaymentDecision decision, SpellAbility sa, final boolean effect) {
        GameEntityCounterTable table = new GameEntityCounterTable();
        table.put(payer, payer, type, decision.c);
        table.replaceCounterEffect(payer.getGame(), sa);
        return true;
    }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
