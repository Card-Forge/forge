package forge.quest.data.item;

import forge.AllZone;
import forge.quest.data.bazaar.QuestStallManager;

public class QuestItemZeppelin extends QuestItemAbstract{
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
                "<em>Effect: </em>Quest assignments become available more frequently<br><em>Effect: </em>Adds +3 to max life during quest games.";
    }

    @Override
    public String getImageName() {
        return "ZeppelinIcon.png";
    }

    @Override
    public int getPrice() {
        return 2000;
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable() && AllZone.QuestData.getInventory().hasItem("Map");
    }
}
