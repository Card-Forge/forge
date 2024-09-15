package forge.gamemodes.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import forge.item.PaperCard;
import forge.model.FModel;

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
        List<PaperCard> cardChoices = new ArrayList<>();
        FModel.getMagicDb().getCommonCards().streamAllCards().filter(predicates)
                .sorted().forEach(cardChoices::add); //TODO: Once java is at 10+, can use Collectors.toUnmodifiableList
        return Collections.unmodifiableList(cardChoices);
    }

}
