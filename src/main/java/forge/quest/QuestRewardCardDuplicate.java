package forge.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import forge.Singletons;
import forge.item.CardPrinted;
import forge.item.ItemPool;

/** 
 * Allows the player to choose a duplicate copy of a currently owned card.
 *
 */
public class QuestRewardCardDuplicate implements IQuestRewardCard {

    private final String description;

    /**
     * 
     * The constructor. No parameters.
     */
    public QuestRewardCardDuplicate() {
        description = "a duplicate card";
    }

    /**
     * The name.
     * 
     * @return the name
     */
    @Override
    public String getName() {
        return description;
    }

    /**
     * This class is a dynamic list of cards, hence no images.
     * 
     * @return an empty string
     */
    @Override
    public String getImageKey() {
        return "";
    }

    /**
     * The item type.
     * 
     * @return item type
     */
    @Override
    public String getItemType() {
        return "duplicate card";
    }

    /**
     * Produces a list of options to choose from, in this case,
     * the player's current cards.
     * 
     * @return a List<CardPrinted> or null if could not create a list.
     */
    public final List<CardPrinted> getChoices() {
        final ItemPool<CardPrinted> playerCards = Singletons.getModel().getQuest().getAssets().getCardPool();
        if (!playerCards.isEmpty()) { // Maybe a redundant check since it's hard to win a duel without any cards...

            List<CardPrinted> cardChoices = new ArrayList<CardPrinted>();
            for (final Map.Entry<CardPrinted, Integer> card : playerCards) {
                cardChoices.add(card.getKey());
            }
            Collections.sort(cardChoices);

            return Collections.unmodifiableList(cardChoices);
        }
        return null;
    }
}
