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
import forge.game.card.Card;
import forge.game.card.CardDamageMap;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The Class CostDamage.
 */
public class CostDamage extends CostPart {

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;

    public CostDamage(final String amount) {
        this.setAmount(amount);
    }

    @Override
    public int paymentOrder() { return 8; }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Deal ").append(this.getAmount()).append(" damage to you");
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
    public final boolean canPay(final SpellAbility ability, final Player payer) {
        return true;
    }
    
    @Override
    public boolean payAsDecided(Player payer, PaymentDecision decision, SpellAbility sa) {
        final Card source = sa.getHostCard();
        CardDamageMap damageMap = new CardDamageMap();
        CardDamageMap preventMap = new CardDamageMap();
        GameEntityCounterTable table = new GameEntityCounterTable();

        damageMap.put(source, payer, decision.c);
        source.getGame().getAction().dealDamage(false, damageMap, preventMap, table, sa);

        return decision.c > 0;
    }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
