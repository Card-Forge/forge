package forge.itemmanager.filters;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardCMCRangeFilter extends ValueRangeFilter<PaperCard> {
    public CardCMCRangeFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardCMCRangeFilter(itemManager);
    }

    @Override
    protected String getCaption() {
        return "Mana Value";
    }

    @Override
    protected Predicate<PaperCard> buildPredicate() {
        Predicate<CardRules> predicate = getCardRulesFieldPredicate(CardRulesPredicates.LeafNumber.CardField.CMC);
        if (predicate == null) {
            return Predicates.alwaysTrue();
        }
        return Predicates.compose(predicate, PaperCard.FN_GET_RULES);
    }
}
