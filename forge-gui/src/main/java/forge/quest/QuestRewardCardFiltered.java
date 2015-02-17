package forge.quest;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.item.PaperCard;
import forge.model.FModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 
 * Allows the player to choose a card from a predicate-filtered list of cards.
 *
 */
public class QuestRewardCardFiltered extends QuestRewardCard {
    private final String description;
    private final Predicate<PaperCard> predicates;

    /**
     * The constructor.
     * @param creationParameters String, used to build the predicates and description for the predicateFilter type
     */
    public QuestRewardCardFiltered(final String[] creationParameters) {
         description = buildDescription(creationParameters);
         predicates = buildPredicates(creationParameters);

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

    @Override
    public String toString() {
        return description;
    }

    /**
     * The item type.
     * 
     * @return item type
     */
    @Override
    public String getItemType() {
        return "chosen card";
    }

    /**
     * Produces a list of options to choose from.
     * 
     * @return a List<CardPrinted> or null if could not create a list.
     */
    @Override
    public final List<PaperCard> getChoices() {
        List<PaperCard> cardChoices = new ArrayList<PaperCard>();
        for (final PaperCard card : Iterables.filter(FModel.getMagicDb().getCommonCards().getAllCards(), predicates)) {
            cardChoices.add(card);
        }
        Collections.sort(cardChoices);
        return Collections.unmodifiableList(cardChoices);
    }

}
