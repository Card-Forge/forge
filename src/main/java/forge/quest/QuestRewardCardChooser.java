package forge.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.Singletons;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;

/** 
 * Resolves a card chooser InventoryItem into a CardPrinted.
 * The initial version includes "duplicate", other type may be added later.
 *
 */
public class QuestRewardCardChooser extends QuestRewardCard implements InventoryItem  {

    /**
     * Possible types for this object.
     */
    public enum poolType {
        /** The player's own cardpool (duplicate card). */
        playerCards,
        /** Filtered by a predicate that will be parsed. */
        predicateFilter
    }

    private poolType type;
    private final String description;
    private final Predicate<CardPrinted> predicates;

    /**
     * The constructor.
     * The parameter indicates the more specific type.
     * @param setType String, the type of the choosable card.
     * @param creationParameters String, used to build the predicates and description for the predicateFilter type
     */
    public QuestRewardCardChooser(final poolType setType, final String[] creationParameters) {
        type = setType;
        if (type == poolType.playerCards) {
            description = "a duplicate card";
            predicates = null;
        } else {
            description = buildDescription(creationParameters);
            predicates = buildPredicates(creationParameters);
        }
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
        switch (type) {
            case playerCards:
                return "duplicate card";
            case predicateFilter: default:
                return "chosen card";
        }
    }

    /**
     * Get type as enum.
     * @return enum, item type.
     */
    public poolType getType() {
        return type;
    }

    /**
     * Produces a list of options to choose from.
     * 
     * @return a List<CardPrinted> or null if could not create a list.
     */
    @Override
    public final List<CardPrinted> getChoices() {
        if (type == poolType.playerCards) {
            final ItemPool<CardPrinted> playerCards = Singletons.getModel().getQuest().getAssets().getCardPool();
            if (!playerCards.isEmpty()) { // Maybe a redundant check since it's hard to win a duel without any cards...

                List<CardPrinted> cardChoices = new ArrayList<CardPrinted>();
                for (final Map.Entry<CardPrinted, Integer> card : playerCards) {
                    cardChoices.add(card.getKey());
                }
                Collections.sort(cardChoices);

                return Collections.unmodifiableList(cardChoices);
            }

        } else if (type == poolType.predicateFilter) {
            List<CardPrinted> cardChoices = new ArrayList<CardPrinted>();

            for (final CardPrinted card : Iterables.filter(CardDb.instance().getAllCards(), predicates)) {
                cardChoices.add(card);
            }
            Collections.sort(cardChoices);

            return Collections.unmodifiableList(cardChoices);
        } else {
            throw new RuntimeException("Unknown QuestRewardCardType: " + type);
        }
        return null;
    }
}
