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
package forge.gamemodes.quest.bazaar;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.data.QuestAssets;
import forge.gui.GuiBase;
import forge.localinstance.assets.FSkinProp;
import forge.localinstance.assets.ISkinImage;

/**
 * <p>
 * Abstract QuestItemAbstract class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestItemBasic implements IQuestBazaarItem {

    @XStreamAsAttribute
    private QuestItemType itemType;

    /**
     * Gets the item type.
     *
     * @return the item type
     */
    public final QuestItemType getItemType() {
        return itemType;
    }

    @XStreamAsAttribute
    private int maxLevel = 1;

    @XStreamAsAttribute
    private String purchaseName = null;

    private String description = "Read from XML";

    @XStreamAsAttribute
    private int basePrice = 1000;

    /**
     * Gets the base price.
     *
     * @return the base price
     */
    protected final int getBasePrice() {
        return basePrice;
    }

    @XStreamAsAttribute
    private final FSkinProp icon = null;

    protected QuestItemBasic() { //needed for XML serialization
    }

    /**
     * <p>
     * Constructor for QuestItemAbstract.
     * </p>
     *
     * @param type0 the type0
     */
    protected QuestItemBasic(final QuestItemType type0) {
        itemType = type0;
    }

    /**
     * This is the name shared across all item levels e.g., "Estates".
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getName() {
        return itemType.getKey();
    }

    /**
     * This is the name used in purchasing the item e.g.,"Estates Training 1".
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getPurchaseName() {
        return StringUtils.isBlank(purchaseName) ? getName() : purchaseName;
    }

    /**
     * This method will be invoked when an item is bought in a shop.
     *
     * @param qA the q a
     */
    @Override
    public void onPurchase(final QuestAssets qA) {
        final int currentLevel = qA.getItemLevel(itemType);
        qA.setItemLevel(itemType, currentLevel + 1);
    }

    /**
     * <p>
     * isAvailableForPurchase.
     * </p>
     *
     *
     * @param qA the q a
     * @param qCtrl
     * @return a boolean.
     */
    @Override
    public boolean isAvailableForPurchase(final QuestAssets qA, QuestController qCtrl) {
        return qA.getItemLevel(itemType) < maxLevel;
    }

    /**
     * <p>
     * Getter for the field <code>maxLevel</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getMaxLevel() {
        return maxLevel;
    }

    /**
     * <p>
     * isLeveledItem.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isLeveledItem() {
        return maxLevel == 1;
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
        return description;
    }

    /**
     * <p>
     * getIcon.
     * </p>
     * 
     */
    @Override
    public ISkinImage getIcon(final QuestAssets qA) {
        return GuiBase.getInterface().getSkinIcon(icon);
    }

    /**
     * Gets the buying price.
     *
     * @param qA the q a
     * @return a int.
     */
    @Override
    public int getBuyingPrice(final QuestAssets qA) {
        return basePrice;
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
        final IQuestBazaarItem q = (IQuestBazaarItem) o;
        return getPurchaseName().compareTo(q.getPurchaseName());
    }
}
