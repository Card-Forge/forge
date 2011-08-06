package forge.quest.data.item;

import forge.AllZone;
import forge.quest.data.bazaar.QuestStallManager;

/**
 * This item has special coding because of the
 */
public class QuestItemElixir extends QuestItemAbstract{

    QuestItemElixir(){
        super("Elixir of Life", QuestStallManager.ALCHEMIST, 15);
    }

    @Override
    public String getPurchaseDescription() {
        return "Gives +1 to maximum life<br>Current Life: "+AllZone.QuestData.getLife();
    }

    @Override
    public String getImageName() {
        return "ElixirIcon.png";
    }

    @Override
    public int getPrice() {
        if (getLevel() < 5){
            return 250;
        } else if (getLevel() < 10){
            return 500;
        } else{
            return 750;
        }
    }

    @Override
    public void onPurchase() {
        AllZone.QuestData.addLife(1);
    }

}
