package forge.quest.data.item;

import forge.AllZone;

public abstract class QuestItemAbstract {
    private int level = 0;
    private String name;
    private String shopName;
    private int maxLevel = 1;


    protected QuestItemAbstract(String name, String shopName) {
        this.name = name;
        this.shopName = shopName;
    }

    protected QuestItemAbstract(String name, String shopName, int maxLevel) {
        this.name = name;
        this.shopName = shopName;
        this.maxLevel = maxLevel;
    }

    /**
     * This is the name shared across all item levels e.g., "Estates"
     */
    public final String getName(){
        return name;
    }

    /**
     * This is the name used in purchasing the item e.g.,"Estates Training 1"
     */
    public String getPurchaseName(){
        return name;
    }

    public String getShopName() {
        return shopName;
    }


    /**
     * This method will be invoked when an item is bought in a shop.
     */
    public void onPurchase() {
    }

    /**
     * Returns if the item is available for purchase;
     * @return <code>true</code> if the item can be displayed in a store
     * <code>false</code> if the item should not be displayed in store since, for example, prerequisites are not met
     */
    public boolean isAvailable(){
        return AllZone.QuestData.getInventory().getItemLevel(name) < maxLevel;
    }

    public int getLevel(){
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
    
    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean isLeveledItem(){
        return maxLevel == 1;
    }

    public abstract String getUpgradeDescription();

    public abstract String getImageName();

    public abstract int getPrice();

}
