package forge.quest.data.item;

import forge.quest.data.bazaar.QuestStallManager;

public class QuestItemLuckyCoin extends QuestItemAbstract{
    QuestItemLuckyCoin(){
        super("Lucky Coin", QuestStallManager.BANKER);
    }

    @Override
    public String getPurchaseDescription() {
        return "This coin is believed to give good luck to its owner.<br>"+
                "Improves the chance of getting a random <br>rare after each match by <b>15%</b>.";
    }

    @Override
    public String getImageName() {
        return "CoinIcon.png";
    }

    @Override
    public int getPrice() {
        return 2000;
    }
    

}
