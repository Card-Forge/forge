package forge.quest.data.item;

import forge.quest.data.bazaar.QuestStallManager;

public class QuestItemEstates extends QuestItemAbstract {
    QuestItemEstates() {
        super("Estates", QuestStallManager.BANKER, 3);
    }

    @Override
    public String getPurchaseDescription() {
        return String.format("Gives a bonus of <b>%d%%</b> to match winnings.<br>" +
                "Improves sell percentage by <b>%.2f%%</b>.", (10 + getLevel() * 5), (1 + getLevel() * 0.75));
    }

    @Override
    public String getImageName() {
        return "GoldIconLarge.png";
    }

    @Override
    public int getPrice() {
        if (getLevel() == 0) {
            return 500;
        }
        else if (getLevel() == 1) {
            return 750;
        }
        else {
            return 1000;
        }
    }

    
}
