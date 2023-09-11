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
import forge.game.ability.AbilityUtils;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The mana component of any spell or ability cost
 */
public class CostPartMana extends CostPart {
    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;
    // "Leftover"
    private final ManaCost cost;
    private boolean xCantBe0 = false;
    private boolean isExiledCreatureCost = false;
    private boolean isEnchantedCreatureCost = false;
    private boolean isCostPayAnyNumberOfTimes = false;

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
        this.isCostPayAnyNumberOfTimes = "NumTimes".equalsIgnoreCase(restriction);
    }

    // This version of the constructor allows to explicitly set exiledCreatureCost/enchantedCreatureCost, used only when copying costs
    public CostPartMana(final ManaCost cost, boolean exiledCreatureCost, boolean enchantedCreatureCost, boolean XCantBe0) {
        this.cost = cost;
        this.xCantBe0 = XCantBe0;
        this.isExiledCreatureCost = exiledCreatureCost;
        this.isEnchantedCreatureCost = enchantedCreatureCost;
    }

    /**
     * Gets the mana.
     *
     * @return the mana
     */
    public final ManaCost getMana() {
        return this.cost;
    }

    public final int getAmountOfX() {
        return this.cost.countX();
    }

    /**
     * @return the xCantBe0
     */
    public boolean canXbe0() {
        return !xCantBe0;
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
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        // For now, this will always return true. But this should probably be
        // checked at some point
        return true;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public ManaCost getManaCostFor(SpellAbility sa) {
        if (isExiledCreatureCost() && sa.getPaidList(CostExile.HashLKIListKey, true) != null && !sa.getPaidList(CostExile.HashLKIListKey, true).isEmpty()) {
            // back from the brink
            return sa.getPaidList(CostExile.HashLKIListKey, true).get(0).getManaCost();
        }
        if (isEnchantedCreatureCost() && sa.getHostCard().isEnchantingCard()) {
            // TODO human can still activate on TDFC backside
            return sa.getHostCard().getEnchantingCard().getManaCost();
        }
        if (isCostPayAnyNumberOfTimes) {
            int timesToPay = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getSVar("NumTimes"), sa);
            if (timesToPay == 0) {
                return null;
            }
            ManaCostBeingPaid totalMana = new ManaCostBeingPaid(getMana());
            for (int i = 1; i < timesToPay; i++) {
                totalMana.addManaCost(getMana());
            }
            return totalMana.toManaCost();
        }
        return getMana();
    }

    @Override
    public boolean payAsDecided(Player payer, PaymentDecision pd, SpellAbility sa, final boolean effect) {
        sa.clearManaPaid();

        ManaConversionMatrix old = new ManaConversionMatrix();
        old.restoreColorReplacements();
        old.applyCardMatrix(payer.getManaPool());

        // decision not used here, the whole payment is interactive!
        boolean result = payer.getController().payManaCost(this, sa, null, pd.matrix, effect);

        // restore old matrix during payment chains
        payer.getManaPool().restoreColorReplacements();
        payer.getManaPool().applyCardMatrix(old);

        return result;
    }

}
