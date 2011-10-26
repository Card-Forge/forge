package forge.quest.data.item;

import forge.quest.data.bazaar.QuestStallManager;

/**
 * <p>
 * QuestItemSleight class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestItemSleight extends QuestItemAbstract {
    /**
     * <p>
     * Constructor for QuestItemSleight.
     * </p>
     */
    QuestItemSleight() {
        super("Sleight", QuestStallManager.BOOKSTORE);

    }

    /** {@inheritDoc} */
    @Override
    public final String getImageName() {
        return "BookIcon.png";
    }

    /** {@inheritDoc} */
    @Override
    public final int getPrice() {
        return 2000;
    }

    /** {@inheritDoc} */
    @Override
    public final String getPurchaseName() {
        return "Sleight of Hand Vol. I";
    }

    /** {@inheritDoc} */
    @Override
    public final String getPurchaseDescription() {
        return "These volumes explain how to perform the most difficult of sleights.<br>"
                + "<em>Effect: </em>Your first mulligan is <b>free</b>";
    }
}
