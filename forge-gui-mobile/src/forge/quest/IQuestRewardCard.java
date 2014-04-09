package forge.quest;

import forge.item.InventoryItem;
import forge.item.PaperCard;

import java.util.List;

/** 
 * Various card rewards that may be awarded during the Quest.
 * Classes that implement this interface should be able to build
 * and return a list of card choices for the player to choose
 * from.
 */
public interface IQuestRewardCard extends InventoryItem {

    /**
     * Returns an unmodifiable list of card choices.
     * @return List<CardPrinted>, an umodifiable list of cards.
     */
    List<PaperCard> getChoices();

}
