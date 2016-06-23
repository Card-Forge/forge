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
 * The Class CostUntap.
 */
public class CostUntap extends CostPart {

    /**
     * Instantiates a new cost untap.
     */
    public CostUntap() {
    }

    @Override
    public int paymentOrder() { return 20; }

    @Override
    public boolean isReusable() { return true; }

    @Override
    public boolean isUndoable() { return true; }
    
    @Override
    public boolean isRenewable() { return true; }

    
    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        return "{Q}";
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public final void refund(final Card source) {
        source.setTapped(true);
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
        final Card source = ability.getHostCard();
        return source.isTapped() && (!source.isSick() || source.hasKeyword("CARDNAME may activate abilities as though it has haste."));
    }

    @Override
    public boolean payAsDecided(Player ai, PaymentDecision decision, SpellAbility ability) {
        ability.getHostCard().untap();
        return true;
    }
    
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
