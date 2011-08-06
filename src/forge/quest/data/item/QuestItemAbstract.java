package forge.quest.data.item;

import forge.AllZone;
import forge.quest.data.bazaar.QuestStallPurchasable;

public abstract class QuestItemAbstract implements QuestStallPurchasable{
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

    public String getStallName() {
        return shopName;
    }


    /**
     * This method will be invoked when an item is bought in a shop.
     */
    public void onPurchase() {
        int currentLevel = AllZone.QuestData.getInventory().getItemLevel(name);
        AllZone.QuestData.getInventory().setItemLevel(name, currentLevel +1);
    }


    public boolean isAvailableForPurchase(){
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

    public abstract String getPurchaseDescription();

    public abstract String getImageName();

    public abstract int getPrice();

    public int compareTo(Object o) {
        QuestStallPurchasable q = (QuestStallPurchasable) o;
        return this.getPurchaseName().compareTo(q.getPurchaseName());
    }
}
