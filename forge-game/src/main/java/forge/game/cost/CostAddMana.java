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

import java.util.ArrayList;
import java.util.List;

import forge.card.mana.ManaAtom;
import org.apache.commons.lang3.StringUtils;

import forge.game.card.Card;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The Class CostAddMana.
 */
public class CostAddMana extends CostPart {
    /**
     * CostCostAddMana.
     * @param amount
     */
    public CostAddMana(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    public int paymentOrder() { return 5; }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        final Integer i = this.convertAmount();
        sb.append("Add ").append(StringUtils.repeat("{" + this.getType() + "}", i)).append(" to your mana pool");
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
    public final boolean canPay(final SpellAbility ability) {
        return true;
    }

    @Override
    public boolean payAsDecided(Player ai, PaymentDecision decision, SpellAbility sa) {
        Card source = sa.getHostCard();
        
        /*ColorSet cid = null;
        if (ai.getGame().getRules().hasCommander()) {
            cid = ai.getCommander().getRules().getColorIdentity();
        }*/
        List<Mana> manaProduced = new ArrayList<Mana>();
        final String type = this.getType();
        for (int n = 0; n < decision.c; n++) {
            if (StringUtils.isNumeric(type)) {
                for (int i = Integer.parseInt(type); i > 0; i--) {
                    manaProduced.add(new Mana((byte)ManaAtom.COLORLESS, source, null));
                }
            } else {
                byte attemptedMana = ManaAtom.fromName(type);
                // Commander rules removed mana generation to avoid colorless abusese
                /*
                if (cid != null) {
                    if (!cid.hasAnyColor(attemptedMana)) {
                        attemptedMana = (byte)ManaAtom.COLORLESS;
                    }
                }*/
                manaProduced.add(new Mana(attemptedMana, source, null));
            }
        }
        ai.getManaPool().add(manaProduced);
        return true;
    }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
