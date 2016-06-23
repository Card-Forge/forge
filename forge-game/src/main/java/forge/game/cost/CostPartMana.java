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

import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The mana component of any spell or ability cost
 */
public class CostPartMana extends CostPart {
    // "Leftover"
    private final ManaCost cost;
    private boolean xCantBe0 = false;
    private boolean isExiledCreatureCost = false;
    private boolean isEnchantedCreatureCost = false;
    private final String restriction;

    public int paymentOrder() { return shouldPayLast() ? 200 : 0; }

    public boolean shouldPayLast() {
        return isExiledCreatureCost;
    }
    /**
     * Instantiates a new cost mana.
     */
    public CostPartMana(final ManaCost cost, String restriction) {
        this.cost = cost;
        this.xCantBe0 = "XCantBe0".equals(restriction);
        this.isExiledCreatureCost = "Exiled".equalsIgnoreCase(restriction);
        this.isEnchantedCreatureCost = "EnchantedCost".equalsIgnoreCase(restriction);
        this.restriction = xCantBe0 || isExiledCreatureCost || isEnchantedCreatureCost? null : restriction;
    }

    /**
     * Gets the mana.
     * 
     * @return the mana
     */
    public final ManaCost getMana() {
        // Only used for Human to pay for non-X cost first
        return this.cost;
    }

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
     * Gets the mana to pay.
     * 
     * @return the mana to pay
     */
    public final ManaCost getManaToPay() {
        return cost;
    }
    
    /**
     * @return the isExiledCreatureCost
     */
    public boolean isExiledCreatureCost() {
        return isExiledCreatureCost;
    }
    
    public boolean isEnchantedCreatureCost() {
        return isEnchantedCreatureCost;
    }
    
    @Override
    public boolean isReusable() { return true; }

    @Override
    public boolean isUndoable() { return true; }
    

    @Override
    public final String toString() {
        return cost.toString();
    }


    @Override
    public final boolean canPay(final SpellAbility ability) {
        // For now, this will always return true. But this should probably be
        // checked at some point
        return true;
    }

    public String getRestiction() {
        return restriction;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public ManaCost getManaCostFor(SpellAbility sa) {
        if (isExiledCreatureCost() && sa.getPaidList(CostExile.HashLKIListKey)!= null && !sa.getPaidList(CostExile.HashLKIListKey).isEmpty()) {
            // back from the brink
            return sa.getPaidList(CostExile.HashLKIListKey).get(0).getManaCost();
        } if (isEnchantedCreatureCost() && sa.getHostCard().getEnchantingCard() != null) {
            return sa.getHostCard().getEnchantingCard().getManaCost();
        } else {
        	return getManaToPay();
        }
    }
    
    @Override
    public boolean payAsDecided(Player payer, PaymentDecision pd, SpellAbility sa) {
        // TODO Auto-generated method stub
        sa.clearManaPaid();

        // decision not used here, the whole payment is interactive!
        return payer.getController().payManaCost(this, sa, null, true); 
    }

}
