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

/**
 * The Class CostTap.
 */
public class CostTap extends CostPart {

    /**
     * Instantiates a new cost tap.
     */
    public CostTap() {
    }

    public int paymentOrder() { return -1; }
    
    @Override
    public boolean isUndoable() { return true; }
    

    @Override
    public boolean isReusable() { return true; }

    @Override
    public boolean isRenewable() { return true; }
    
    @Override
    public final String toString() {
        return "{T}";
    }

    @Override
    public final void refund(final Card source) {
        source.setTapped(false);
    }


    @Override
    public final boolean canPay(final SpellAbility ability) {
        final Card source = ability.getHostCard();
        return source.isUntapped() && (!source.isSick() || source.hasKeyword("CARDNAME may activate abilities as though it has haste."));
    }

    @Override
    public boolean payAsDecided(Player ai, PaymentDecision decision, SpellAbility ability) {
        ability.getHostCard().tap();
        return true;
    }
    
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
