package forge.quest.data.pet;

import forge.AllZone;
import forge.Card;
import forge.quest.data.bazaar.QuestStallManager;
import forge.quest.data.bazaar.QuestStallPurchasable;

/**
 * <p>
 * Abstract QuestPetAbstract class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class QuestPetAbstract implements QuestStallPurchasable {

    /** The level. */
    private int level;
    private final int maxLevel;
    // transient here ?
    private final String name;
    private final String description;

    /**
     * <p>
     * getPetCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public abstract Card getPetCard();

    /**
     * <p>
     * getAllUpgradePrices.
     * </p>
     * 
     * @return an array of int.
     */
    public abstract int[] getAllUpgradePrices();

    /**
     * <p>
     * getPrice.
     * </p>
     * 
     * @return a int.
     */
    @Override
    public final int getPrice() {
        return this.getAllUpgradePrices()[this.level];
    }

    /**
     * <p>
     * getAllUpgradeDescriptions.
     * </p>
     * 
     * @return an array of {@link java.lang.String} objects.
     */
    public abstract String[] getAllUpgradeDescriptions();

    /**
     * <p>
     * getUpgradeDescription.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getUpgradeDescription() {
        return this.getAllUpgradeDescriptions()[this.level];
    }

    /**
     * <p>
     * getAllImageNames.
     * </p>
     * 
     * @return an array of {@link java.lang.String} objects.
     */
    public abstract String[] getAllImageNames();

    /**
     * <p>
     * getImageName.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String getImageName() {
        return this.getAllImageNames()[this.level];
    }

    /**
     * <p>
     * getAllStats.
     * </p>
     * 
     * @return an array of {@link java.lang.String} objects.
     */
    public abstract String[] getAllStats();

    /**
     * <p>
     * getStats.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getStats() {
        return this.getAllStats()[this.level];
    }

    /**
     * <p>
     * getUpgradedStats.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getUpgradedStats() {
        return this.getAllStats()[this.level + 1];
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
     * incrementLevel.
     * </p>
     */
    public final void incrementLevel() {
        if (this.level < this.maxLevel) {
            this.level++;
        }
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
     * Constructor for QuestPetAbstract.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param description
     *            a {@link java.lang.String} object.
     * @param maxLevel
     *            a int.
     */
    protected QuestPetAbstract(final String name, final String description, final int maxLevel) {
        this.description = description;
        this.name = name;
        this.maxLevel = maxLevel;
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
     * getPurchaseDescription.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String getPurchaseDescription() {
        return "<em>" + this.getDescription() + "</em><br>" + this.getUpgradeDescription()
                + "<br><br><u>Current stats:</u> " + this.getStats() + "<br><u>Upgraded stats:</u> "
                + this.getUpgradedStats();

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

    /**
     * <p>
     * getStallName.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getStallName() {
        return QuestStallManager.PET_SHOP;
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
        final QuestPetAbstract pet = AllZone.getQuestData().getPetManager().getPet(this.name);
        if (pet == null) {
            return true;
        }
        return pet.level < pet.getMaxLevel();
    }

    /**
     * <p>
     * onPurchase.
     * </p>
     */
    @Override
    public void onPurchase() {
        AllZone.getQuestData().getPetManager().addPetLevel(this.name);
    }
}
