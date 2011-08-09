package forge.quest.data.item;

import forge.AllZone;
import forge.quest.data.bazaar.QuestStallManager;

/**
 * <p>QuestItemZeppelin class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class QuestItemZeppelin extends QuestItemAbstract {
    boolean zeppelinUsed = false;

    /**
     * <p>Constructor for QuestItemZeppelin.</p>
     */
    QuestItemZeppelin() {
        super("Zeppelin", QuestStallManager.GEAR);
    }

    /** {@inheritDoc} */
    @Override
    public String getPurchaseName() {
        return "Zeppelin";
    }

    /** {@inheritDoc} */
    @Override
    public String getPurchaseDescription() {
        return "This extremely comfortable airship allows for more efficient and safe travel<br>to faraway destinations. <br>" +
                "<em>Effect: </em>Quest assignments become available more frequently<br>" +
                "<em>Effect: </em>Adds +3 to max life during quest games.<br>" +
                "<em>Effect: </em>Allows travel to far places, allowing you to see a new set of opponents";
    }

    /** {@inheritDoc} */
    @Override
    public String getImageName() {
        return "ZeppelinIcon.png";
    }

    /** {@inheritDoc} */
    @Override
    public int getPrice() {
        return 5000;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAvailableForPurchase() {
        return super.isAvailableForPurchase() && AllZone.getQuestData().getInventory().hasItem("Map");
    }

    /**
     * <p>hasBeenUsed.</p>
     *
     * @return a boolean.
     */
    public boolean hasBeenUsed() {
        return zeppelinUsed;
    }

    /**
     * <p>Setter for the field <code>zeppelinUsed</code>.</p>
     *
     * @param used a boolean.
     */
    public void setZeppelinUsed(boolean used) {
        this.zeppelinUsed = used;
    }
}
