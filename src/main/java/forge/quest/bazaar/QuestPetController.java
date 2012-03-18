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
package forge.quest.bazaar;

import javax.swing.ImageIcon;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import forge.Card;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestAssets;

/**
 * <p>
 * Abstract QuestPetAbstract class.
 * </p>
 * It's not good to store in a single class pets properties and bazaar sellable - such is a tradeoff for speed of development  
 * @author Forge
 * @version $Id$
 */
public class QuestPetController implements IQuestBazaarItem {

    /** The level. */
    @XStreamAsAttribute()
    private int maxLevel;
    
    private final List<QuestPetStats> levels = new ArrayList<QuestPetStats>();

    @XStreamAsAttribute()
    private final String name;
    
    @XStreamAlias(value="desc")
    private final String description;
    @XStreamAsAttribute()
    private final String saveFileKey;
    @XStreamAsAttribute()    
    private int slot; 
    
    protected int getPetLevel(QuestAssets qA) {
        int level = qA.getPetLevel(saveFileKey);
        return level < 0 ? 0 : level > maxLevel ? maxLevel : level; 
    }
    
    /**
     * <p>
     * getPetCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    
    
    public Card getPetCard(QuestAssets qA) {
        return this.levels.get(getPetLevel(qA)).getCard();
    }

    /**
     * <p>
     * getPrice.
     * </p>
     * 
     * @return a int.
     */
    @Override
    public final int getBuyingPrice(QuestAssets qA) {
        int level = getPetLevel(qA);
        // we'll buy next level 
        return level >= maxLevel ? -1 /* cannot buy */ : this.levels.get(level + 1).getCost();
    }
    

    /** {@inheritDoc} */
    @Override
    public final int getSellingPrice(QuestAssets qA) {
        return 0;
    }

    /**
     * <p>
     * getUpgradeDescription.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getUpgradeDescription(QuestAssets qA) {
        return this.levels.get(getPetLevel(qA)).getNextLevel();
    }

    /**
     * <p>
     * getIcon.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final ImageIcon getIcon(QuestAssets qA) {
        String path = ForgeProps.getFile(NewConstants.IMAGE_TOKEN).getAbsolutePath() + File.separator;
        int level = getPetLevel(qA);
        return new ImageIcon( path + levels.get(level < maxLevel ? level + 1 : level ).getPicture() + ".jpg");
    }

    /**
     * <p>
     * getStats.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getStats(QuestAssets qA) {
        return this.levels.get(getPetLevel(qA)).getStats();
    }

    /**
     * <p>
     * getUpgradedStats.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getUpgradedStats(QuestAssets qA) {
        int level = getPetLevel(qA);
        return level >= maxLevel ? "N/A" : this.levels.get(level+1).getStats();
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
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String getPurchaseDescription(QuestAssets qA) {
        return this.getDescription()
                + "\n\nCurrent stats: " + this.getStats(qA) + "\nUpgraded stats: "
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


//    @Override
//    public String getStallName() {
//        return QuestStallManager.PET_SHOP;
//    }

    /**
     * <p>
     * isAvailableForPurchase.
     * </p>
     * 
     * @return a boolean.
     */
    @Override
    public boolean isAvailableForPurchase(QuestAssets qA) {
        return getPetLevel(qA) < getMaxLevel();
    }

    /**
     * <p>
     * onPurchase.
     * </p>
     */
    @Override
    public void onPurchase(QuestAssets qA) {
        qA.setPetLevel(saveFileKey, getPetLevel(qA) + 1);
    }

    public String getSaveFileKey() {
        return saveFileKey;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public int getSlot() {
        return slot;
    }
}
