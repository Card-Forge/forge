package forge.quest.data.item;

public class QuestItemZeppelin extends QuestItemAbstract{
    QuestItemZeppelin(){
        super("Zeppelin",QuestInventory.GEAR);
    }

    @Override
    public String getPurchaseName() {
        return "Zeppelin";
    }

    @Override
    public String getUpgradeDescription() {
        return "This extremely comfortable airship allows for more efficient and safe travel<br>to faraway destinations. <br>"+
                "<u>Quest assignments become available more frequently<br>Adds +3 to max life during quest games</u>.";
    }

    @Override
    public String getImageName() {
        return "ZeppelinIcon.png";
    }

    @Override
    public int getPrice() {
        return 2000;
    }
}
