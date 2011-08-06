package forge.quest.data.item;

public class QuestItemSleight extends QuestItemAbstract{
    QuestItemSleight(){
        super("Sleight", QuestInventory.BOOKSTORE);

    }
    @Override
    public String getImageName() {
        return "BookIcon.png";
    }

    @Override
    public int getPrice() {
        return 2000;
    }

    @Override
    public String getPurchaseName() {
        return "Sleight of Hand Vol. I";
    }

    @Override
    public String getUpgradeDescription() {
        return "These volumes explain how to perform the most difficult of sleights.<br>"+
                "<u>Your first mulligan is <b>free</b></u>.";
    }
}
