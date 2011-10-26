package forge.quest.data.item;

import forge.quest.data.bazaar.QuestStallManager;

/**
 * <p>
 * QuestItemMap class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestItemMap extends QuestItemAbstract {
    /**
     * <p>
     * Constructor for QuestItemMap.
     * </p>
     */
    QuestItemMap() {
        super("Map", QuestStallManager.GEAR);
    }

    /** {@inheritDoc} */
    @Override
    public final String getPurchaseName() {
        return "Adventurer's Map";
    }

    /** {@inheritDoc} */
    @Override
    public final String getPurchaseDescription() {
        return "These ancient charts should facilitate navigation during your travels significantly.<br>"
                + "<em>Effect: </em>Quest assignments become available more frequently.";
    }

    /** {@inheritDoc} */
    @Override
    public final String getImageName() {
        return "MapIconLarge.png";
    }

    /** {@inheritDoc} */
    @Override
    public final int getPrice() {
        return 2000;
    }
}
