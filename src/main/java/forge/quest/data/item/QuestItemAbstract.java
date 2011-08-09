package forge.quest.data.item;

import forge.AllZone;
import forge.quest.data.bazaar.QuestStallPurchasable;

/**
 * <p>Abstract QuestItemAbstract class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public abstract class QuestItemAbstract implements QuestStallPurchasable {
    private int level = 0;
    private String name;
    private String shopName;
    private int maxLevel = 1;


    /**
     * <p>Constructor for QuestItemAbstract.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param shopName a {@link java.lang.String} object.
     */
    protected QuestItemAbstract(String name, String shopName) {
        this.name = name;
        this.shopName = shopName;
    }

    /**
     * <p>Constructor for QuestItemAbstract.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param shopName a {@link java.lang.String} object.
     * @param maxLevel a int.
     */
    protected QuestItemAbstract(String name, String shopName, int maxLevel) {
        this.name = name;
        this.shopName = shopName;
        this.maxLevel = maxLevel;
    }

    /**
     * This is the name shared across all item levels e.g., "Estates"
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getName() {
        return name;
    }

    /**
     * This is the name used in purchasing the item e.g.,"Estates Training 1"
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPurchaseName() {
        return name;
    }

    /**
     * <p>getStallName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getStallName() {
        return shopName;
    }


    /**
     * This method will be invoked when an item is bought in a shop.
     */
    public void onPurchase() {
        int currentLevel = AllZone.getQuestData().getInventory().getItemLevel(name);
        AllZone.getQuestData().getInventory().setItemLevel(name, currentLevel + 1);
    }


    /**
     * <p>isAvailableForPurchase.</p>
     *
     * @return a boolean.
     */
    public boolean isAvailableForPurchase() {
        return AllZone.getQuestData().getInventory().getItemLevel(name) < maxLevel;
    }

    /**
     * <p>Getter for the field <code>level</code>.</p>
     *
     * @return a int.
     */
    public int getLevel() {
        return level;
    }

    /**
     * <p>Setter for the field <code>level</code>.</p>
     *
     * @param level a int.
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * <p>Getter for the field <code>maxLevel</code>.</p>
     *
     * @return a int.
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * <p>isLeveledItem.</p>
     *
     * @return a boolean.
     */
    public boolean isLeveledItem() {
        return maxLevel == 1;
    }

    /**
     * <p>getPurchaseDescription.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public abstract String getPurchaseDescription();

    /**
     * <p>getImageName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public abstract String getImageName();

    /**
     * <p>getPrice.</p>
     *
     * @return a int.
     */
    public abstract int getPrice();

    /** {@inheritDoc} */
    public int compareTo(Object o) {
        QuestStallPurchasable q = (QuestStallPurchasable) o;
        return this.getPurchaseName().compareTo(q.getPurchaseName());
    }
}
