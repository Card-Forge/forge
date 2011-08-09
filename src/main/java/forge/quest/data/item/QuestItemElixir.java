package forge.quest.data.item;

import forge.AllZone;
import forge.quest.data.bazaar.QuestStallManager;

/**
 * This item has special coding because of the
 *
 * @author Forge
 * @version $Id$
 */
public class QuestItemElixir extends QuestItemAbstract {

    /**
     * <p>Constructor for QuestItemElixir.</p>
     */
    QuestItemElixir() {
        super("Elixir of Life", QuestStallManager.ALCHEMIST, 15);
    }

    /** {@inheritDoc} */
    @Override
    public String getPurchaseDescription() {
        return "Gives +1 to maximum life<br>Current Life: " + AllZone.getQuestData().getLife();
    }

    /** {@inheritDoc} */
    @Override
    public String getImageName() {
        return "ElixirIcon.png";
    }

    /** {@inheritDoc} */
    @Override
    public int getPrice() {
        if (getLevel() < 5) {
            return 250;
        } else if (getLevel() < 10) {
            return 500;
        } else {
            return 750;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onPurchase() {
        AllZone.getQuestData().addLife(1);
    }

}
