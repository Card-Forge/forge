package forge.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.ItemPool;

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
     * The item type.
     *
     * @return item type
     */
    @Override
    public String getItemType() {
        return "duplicate card";
    }

    @Override
    public String toString() {
        return description;
    }

    /**
     * Produces a list of options to choose from, in this case,
     * the player's current cards.
     *
     * @return a List<CardPrinted> or null if could not create a list.
     */
    @Override
    public final List<PaperCard> getChoices() {
        final ItemPool<PaperCard> playerCards = FModel.getQuest().getAssets().getCardPool();
        if (!playerCards.isEmpty()) { // Maybe a redundant check since it's hard to win a duel without any cards...

            final List<PaperCard> cardChoices = new ArrayList<PaperCard>();
            for (final Map.Entry<PaperCard, Integer> card : playerCards) {
                cardChoices.add(card.getKey());
            }
            Collections.sort(cardChoices);

            return Collections.unmodifiableList(cardChoices);
        }
        return null;
    }
}
