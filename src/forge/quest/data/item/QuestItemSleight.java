package forge.quest.data.item;

import forge.quest.data.bazaar.QuestStallManager;

public class QuestItemSleight extends QuestItemAbstract{
    QuestItemSleight(){
        super("Sleight", QuestStallManager.BOOKSTORE);

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
    public String getPurchaseDescription() {
        return "These volumes explain how to perform the most difficult of sleights.<br>"+
                "<em>Effect: </em>Your first mulligan is <b>free</b>";
    }
}
