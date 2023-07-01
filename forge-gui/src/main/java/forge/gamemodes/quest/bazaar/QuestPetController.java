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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.data.QuestAssets;
import forge.gamemodes.quest.data.QuestPreferences;
import forge.gui.GuiBase;
import forge.item.PaperToken;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.model.FModel;

/**
 * <p>
 * Abstract QuestPetAbstract class.
 * </p>
 * It's not good to store in a single class pets properties and bazaar sellable
 * - such is a tradeoff for speed of development
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestPetController implements IQuestBazaarItem {

    /** The level. */
    @XStreamAsAttribute()
    private final int maxLevel;

    private final List<QuestPetStats> levels = new ArrayList<>();

    @XStreamAsAttribute()
    private final String name;

    @XStreamAlias(value = "desc")
    private final String description;
    @XStreamAsAttribute()
    private final String saveFileKey;
    @XStreamAsAttribute()
    private int slot;

    /**
     * 
     * TODO: Write javadoc for this method.
     * @param qA quest assets
     * @return int
     */
    protected int getPetLevel(final QuestAssets qA) {
        final int level = qA.getPetLevel(this.saveFileKey);
        return level < 0 ? 0 : level > this.maxLevel ? this.maxLevel : level;
    }

    /**
     * <p>
     * getPetCard.
     * </p>
     * @param qA quest assets
     * @return a {@link forge.game.card.Card} object.
     */

    public PaperToken getPetCard(final QuestAssets qA) {
        return this.levels.get(this.getPetLevel(qA)).getCard();
    }

    /**
     * <p>
     * getPrice.
     * </p>
     * @param qA quest assets
     * @return a int.
     */
    @Override
    public final int getBuyingPrice(final QuestAssets qA) {
        final int level = this.getPetLevel(qA);
        // we'll buy next level
        return level >= this.maxLevel ? -1 /* cannot buy */ : this.levels.get(level + 1).getCost();
    }

    /** {@inheritDoc} */
    @Override
    public final int getSellingPrice(final QuestAssets qA) {
        return 0;
    }

    /**
     * <p>
     * getUpgradeDescription.
     * </p>
     * @param qA quest assets
     * @return a {@link java.lang.String} object.
     */
    public final String getUpgradeDescription(final QuestAssets qA) {
        return this.levels.get(this.getPetLevel(qA)).getNextLevel();
    }

    /**
     * <p>
     * getIcon.
     * </p>
     * @param qA quest assets
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final ISkinImage getIcon(final QuestAssets qA) {
        final String path = ForgeConstants.CACHE_TOKEN_PICS_DIR;
        final int level = this.getPetLevel(qA);
        try {
            return GuiBase.getInterface().getUnskinnedIcon(path + this.levels.get(level < this.maxLevel ? level + 1 : level).getPicture() + ".jpg");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            return GuiBase.getInterface().getUnskinnedIcon(ForgeConstants.NO_CARD_FILE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return GuiBase.getInterface().getSkinIcon(FSkinProp.ICO_UNKNOWN);
    }

    /**
     * <p>
     * getStats.
     * </p>
     * @param qA quest assets
     * @return a {@link java.lang.String} object.
     */
    public final String getStats(final QuestAssets qA) {
        return this.levels.get(this.getPetLevel(qA)).getStats();
    }

    /**
     * <p>
     * getUpgradedStats.
     * </p>
     * @param qA quest assets
     * @return a {@link java.lang.String} object.
     */
    public final String getUpgradedStats(final QuestAssets qA) {
        final int level = this.getPetLevel(qA);
        return level >= this.maxLevel ? "N/A" : this.levels.get(level + 1).getStats();
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

    // Never to be called, instances will be read from xml
    private QuestPetController() {
        this.description = null;
        this.name = null;
        this.maxLevel = 0;
        this.saveFileKey = null;
    }

    /**
     * <p>
     * getPurchaseDescription.
     * </p>
     * @param qA quest assets
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String getPurchaseDescription(final QuestAssets qA) {
        return this.getDescription() + "\n\nCurrent stats: " + this.getStats(qA) + "\nUpgraded stats: "
                + this.getUpgradedStats(qA);

    }

    /**
     * <p>
     * Getter for the field <code>description</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getDescription() {
        return this.description;
    }

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public final int compareTo(final Object o) {
        return this.name.compareTo(o.toString());
    }

    /**
     * <p>
     * getPurchaseName.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String getPurchaseName() {
        return this.name;
    }

    // @Override
    // public String getStallName() {
    // return QuestStallManager.PET_SHOP;
    // }

    /**
     * <p>
     * isAvailableForPurchase.
     * </p>
     *
     * @param qA quest assets
     * @param qCtrl
     * @return a boolean.
     */
    @Override
    public boolean isAvailableForPurchase(final QuestAssets qA, QuestController qCtrl) {
        return this.getPetLevel(qA) < this.getMaxLevel() &&
                (qCtrl == null || qCtrl.getLevel() >= this.getPetLevel(qA) ||
                FModel.getQuestPreferences().getPrefInt(QuestPreferences.QPref.ITEM_LEVEL_RESTRICTION) == 0);
    }

    /**
     * <p>
     * onPurchase.
     * </p>
     * @param qA quest assets
     */
    @Override
    public void onPurchase(final QuestAssets qA) {
        qA.setPetLevel(this.saveFileKey, this.getPetLevel(qA) + 1);
    }

    /**
     * 
     * TODO: Write javadoc for this method.
     * @return String
     */
    public String getSaveFileKey() {
        return this.saveFileKey;
    }

    /**
     * TODO: Write javadoc for this method.
     * 
     * @return int
     */
    public int getSlot() {
        return this.slot;
    }
}
