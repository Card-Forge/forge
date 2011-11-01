package forge.quest.data.item;

import forge.quest.data.bazaar.QuestStallManager;

/**
 * <p>
 * QuestItemEstates class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestItemEstates extends QuestItemAbstract {
    /**
     * <p>
     * Constructor for QuestItemEstates.
     * </p>
     */
    QuestItemEstates() {
        super("Estates", QuestStallManager.BANKER, 3);
    }

    /** {@inheritDoc} */
    @Override
    public final String getPurchaseDescription() {
        return String.format("Gives a bonus of <b>%d%%</b> to match winnings.<br>"
                + "Improves sell percentage by <b>%.2f%%</b>.", (10 + (this.getLevel() * 5)),
                (1 + (this.getLevel() * 0.75)));
    }

    /** {@inheritDoc} */
    @Override
    public final String getImageName() {
        return "GoldIconLarge.png";
    }

    /** {@inheritDoc} */
    @Override
    public final int getPrice() {
        if (this.getLevel() == 0) {
            return 500;
        } else if (this.getLevel() == 1) {
            return 750;
        } else {
            return 1000;
        }
    }

}
