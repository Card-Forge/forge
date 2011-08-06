package forge.quest.data.item;

public class QuestItemLuckyCoin extends QuestItemAbstract{
    QuestItemLuckyCoin(){
        super("Lucky Coin",QuestInventory.BANKER);
    }

    @Override
    public String getUpgradeDescription() {
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
