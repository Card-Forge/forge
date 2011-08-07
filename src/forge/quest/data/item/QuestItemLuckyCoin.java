package forge.quest.data.item;

import forge.quest.data.bazaar.QuestStallManager;

/**
 * <p>QuestItemLuckyCoin class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class QuestItemLuckyCoin extends QuestItemAbstract {
    /**
     * <p>Constructor for QuestItemLuckyCoin.</p>
     */
    QuestItemLuckyCoin() {
        super("Lucky Coin", QuestStallManager.BANKER);
    }

    /** {@inheritDoc} */
    @Override
    public String getPurchaseDescription() {
        return "This coin is believed to give good luck to its owner.<br>" +
                "Improves the chance of getting a random <br>rare after each match by <b>15%</b>.";
    }

    /** {@inheritDoc} */
    @Override
    public String getImageName() {
        return "CoinIcon.png";
    }

    /** {@inheritDoc} */
    @Override
    public int getPrice() {
        return 2000;
    }


}
