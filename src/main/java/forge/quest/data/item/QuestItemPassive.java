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

import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.QuestIcons;
import forge.quest.data.QuestAssets;

/**
 * <p>
 * Abstract QuestItemAbstract class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestItemPassive implements IQuestStallPurchasable {

    @XStreamAsAttribute
    private final QuestItemType itemType;

    /**
     * Gets the item type.
     *
     * @return the item type
     */
    public final QuestItemType getItemType() {
        return this.itemType;
    }

    @XStreamAsAttribute
    private final int maxLevel = 1;

    @XStreamAsAttribute
    private final String purchaseName = "Read this field from XML";
    private final String description = "Read from XML";

    @XStreamAsAttribute
    private final int basePrice = 1000;

    /**
     * Gets the base price.
     *
     * @return the base price
     */
    protected final int getBasePrice() {
        return this.basePrice;
    }

    @XStreamAsAttribute
    private final QuestIcons icon = null;

    /**
     * <p>
     * Constructor for QuestItemAbstract.
     * </p>
     *
     * @param type0 the type0
     */
    protected QuestItemPassive(final QuestItemType type0) {
        this.itemType = type0;
    }

    /**
     * This is the name shared across all item levels e.g., "Estates".
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getName() {
        return this.itemType.getKey();
    }

    /**
     * This is the name used in purchasing the item e.g.,"Estates Training 1".
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getPurchaseName() {
        return StringUtils.isBlank(this.purchaseName) ? this.getName() : this.purchaseName;
    }

    /**
     * This method will be invoked when an item is bought in a shop.
     *
     * @param qA the q a
     */
    @Override
    public void onPurchase(final QuestAssets qA) {
        final int currentLevel = qA.getItemLevel(this.itemType);
        qA.setItemLevel(this.itemType, currentLevel + 1);
    }

    /**
     * <p>
     * isAvailableForPurchase.
     * </p>
     *
     * @param qA the q a
     * @return a boolean.
     */
    @Override
    public boolean isAvailableForPurchase(final QuestAssets qA) {
        return qA.getItemLevel(this.itemType) < this.maxLevel;
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
     * @param qA the q a
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getPurchaseDescription(final QuestAssets qA) {
        return this.description;
    }

    /**
     * <p>
     * getImageName.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public ImageIcon getIcon() {
        return FSkin.getIcon(this.icon);
    }

    /**
     * Gets the buying price.
     *
     * @param qA the q a
     * @return a int.
     */
    @Override
    public int getBuyingPrice(final QuestAssets qA) {
        return this.basePrice;
    }

    /**
     * Gets the selling price.
     *
     * @param qA the q a
     * @return a int.
     */
    @Override
    public int getSellingPrice(final QuestAssets qA) {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public final int compareTo(final Object o) {
        final IQuestStallPurchasable q = (IQuestStallPurchasable) o;
        return this.getPurchaseName().compareTo(q.getPurchaseName());
    }
}
