package forge.quest.data.pet;

import forge.AllZone;
import forge.Card;
import forge.quest.data.bazaar.QuestStallManager;
import forge.quest.data.bazaar.QuestStallPurchasable;

public abstract class QuestPetAbstract implements QuestStallPurchasable{
    int level;
    private int maxLevel;
    private String name;
    private String description;

    public abstract Card getPetCard();

    public abstract int[] getAllUpgradePrices();
    public int getPrice(){
        return getAllUpgradePrices()[level];
    }

    public abstract String[] getAllUpgradeDescriptions();
    public String getUpgradeDescription(){
        return getAllUpgradeDescriptions()[level];
    }


    public abstract String[] getAllImageNames();
    public String getImageName(){
        return getAllImageNames()[level];
    }

    public abstract String[] getAllStats();
    public String getStats(){
        return getAllStats()[level];
    }
    public String getUpgradedStats(){
        return getAllStats()[level+1];
    }


    public int getLevel() {
        return level;
    }

    public void incrementLevel(){
        if (level < maxLevel){
            level++;
        }
    }

    public int getMaxLevel(){
        return maxLevel;
    }

    protected QuestPetAbstract(String name, String description, int maxLevel) {
        this.description = description;
        this.name = name;
        this.maxLevel = maxLevel;
    }

    public void setLevel(int level){
        this.level = level;
    }

    public String getPurchaseDescription() {
        return "<em>"+getDescription()+"</em><br>" + getUpgradeDescription()+
                "<br><br><u>Current stats:</u> " + getStats()+
                "<br><u>Upgraded stats:</u> " + getUpgradedStats();

    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public int compareTo(Object o) {
        return name.compareTo(o.toString());
    }

    public String getPurchaseName() {
        return name;
    }

    public String getStallName() {
        return QuestStallManager.PET_SHOP;
    }

    public boolean isAvailableForPurchase() {
        QuestPetAbstract pet = AllZone.QuestData.getPetManager().getPet(name);
        if (pet == null){
            return true;
        }
        return pet.level < pet.getMaxLevel();
    }

    public void onPurchase() {
        AllZone.QuestData.getPetManager().addPetLevel(name);
    }
}
