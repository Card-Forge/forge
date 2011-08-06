package forge.quest.data.item;

import forge.AllZone;
import forge.quest.data.bazaar.QuestStallManager;

public class QuestItemZeppelin extends QuestItemAbstract{
    boolean zeppelinUsed = false;

    QuestItemZeppelin(){
        super("Zeppelin", QuestStallManager.GEAR);
    }

    @Override
    public String getPurchaseName() {
        return "Zeppelin";
    }

    @Override
    public String getPurchaseDescription() {
        return "This extremely comfortable airship allows for more efficient and safe travel<br>to faraway destinations. <br>"+
                "<em>Effect: </em>Quest assignments become available more frequently<br>" +
                "<em>Effect: </em>Adds +3 to max life during quest games.<br>" +
                "<em>Effect: </em>Allows travel to far places, allowing you to see a new set of opponents";
    }

    @Override
    public String getImageName() {
        return "ZeppelinIcon.png";
    }

    @Override
    public int getPrice() {
        return 5000;
    }

    @Override
    public boolean isAvailableForPurchase() {
        return super.isAvailableForPurchase() && AllZone.QuestData.getInventory().hasItem("Map");
    }

    public boolean hasBeenUsed() {
        return zeppelinUsed;
    }

    public void setZeppelinUsed(boolean used){
        this.zeppelinUsed = used;
    }
}
