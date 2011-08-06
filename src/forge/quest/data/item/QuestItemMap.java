package forge.quest.data.item;

public class QuestItemMap extends QuestItemAbstract{
    QuestItemMap(){
        super("Map",QuestInventory.GEAR);
    }

    @Override
    public String getPurchaseName() {
        return "Adventurer's Map";
    }

    @Override
    public String getUpgradeDescription() {
        return "These ancient charts should facilitate navigation during your travels significantly.<br>"+
                "<u>Quest assignments become available more frequently</u>.";
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
