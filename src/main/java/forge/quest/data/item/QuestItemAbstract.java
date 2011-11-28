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
package forge.quest.data.item;

import forge.AllZone;
import forge.quest.data.bazaar.QuestStallPurchasable;

/**
 * <p>
 * Abstract QuestItemAbstract class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class QuestItemAbstract implements QuestStallPurchasable {
    private int level = 0;
    private final String name;
    private final String shopName;
    private int maxLevel = 1;

    /**
     * <p>
     * Constructor for QuestItemAbstract.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param shopName
     *            a {@link java.lang.String} object.
     */
    protected QuestItemAbstract(final String name, final String shopName) {
        this.name = name;
        this.shopName = shopName;
    }

    /**
     * <p>
     * Constructor for QuestItemAbstract.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param shopName
     *            a {@link java.lang.String} object.
     * @param maxLevel
     *            a int.
     */
    protected QuestItemAbstract(final String name, final String shopName, final int maxLevel) {
        this.name = name;
        this.shopName = shopName;
        this.maxLevel = maxLevel;
    }

    /**
     * This is the name shared across all item levels e.g., "Estates".
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getName() {
        return this.name;
    }

    /**
     * This is the name used in purchasing the item e.g.,"Estates Training 1".
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getPurchaseName() {
        return this.name;
    }

    /**
     * <p>
     * getStallName.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String getStallName() {
        return this.shopName;
    }

    /**
     * This method will be invoked when an item is bought in a shop.
     */
    @Override
    public void onPurchase() {
        final int currentLevel = AllZone.getQuestData().getInventory().getItemLevel(this.name);
        AllZone.getQuestData().getInventory().setItemLevel(this.name, currentLevel + 1);
    }

    /**
     * <p>
     * isAvailableForPurchase.
     * </p>
     * 
     * @return a boolean.
     */
    @Override
    public boolean isAvailableForPurchase() {
        return AllZone.getQuestData().getInventory().getItemLevel(this.name) < this.maxLevel;
    }

    /**
     * <p>
     * Getter for the field <code>level</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getLevel() {
        return this.level;
    }

    /**
     * <p>
     * Setter for the field <code>level</code>.
     * </p>
     * 
     * @param level
     *            a int.
     */
    public final void setLevel(final int level) {
        this.level = level;
    }

    /**
     * <p>
     * Getter for the field <code>maxLevel</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getMaxLevel() {
        return this.maxLevel;
    }

    /**
     * <p>
     * isLeveledItem.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isLeveledItem() {
        return this.maxLevel == 1;
    }

    /**
     * <p>
     * getPurchaseDescription.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public abstract String getPurchaseDescription();

    /**
     * <p>
     * getImageName.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public abstract String getImageName();

    /**
     * <p>
     * getPrice.
     * </p>
     * 
     * @return a int.
     */
    @Override
    public abstract int getPrice();

    /** {@inheritDoc} */
    @Override
    public final int compareTo(final Object o) {
        final QuestStallPurchasable q = (QuestStallPurchasable) o;
        return this.getPurchaseName().compareTo(q.getPurchaseName());
    }
}
