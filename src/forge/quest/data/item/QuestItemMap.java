package forge.quest.data.item;

import forge.quest.data.bazaar.QuestStallManager;

public class QuestItemMap extends QuestItemAbstract{
    QuestItemMap(){
        super("Map", QuestStallManager.GEAR);
    }

    @Override
    public String getPurchaseName() {
        return "Adventurer's Map";
    }

    @Override
    public String getPurchaseDescription() {
        return "These ancient charts should facilitate navigation during your travels significantly.<br>"+
                "<em>Effect: </em>Quest assignments become available more frequently.";
    }

    @Override
    public String getImageName() {
        return "MapIconLarge.png";
    }

    @Override
    public int getPrice() {
        return 2000;
    }
}
