package forge.itemmanager.filters;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;


public class CardColorlessCostFilter extends ValueRangeFilter<PaperCard> {
    public CardColorlessCostFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardColorlessCostFilter(itemManager);
    }

    @Override
    protected String getCaption() {
        return "Generic Cost";
    }

    @Override
    protected Predicate<PaperCard> buildPredicate() {
        Predicate<CardRules> predicate = getCardRulesFieldPredicate(CardRulesPredicates.LeafNumber.CardField.GENERIC_COST);
        if (predicate == null) {
            return Predicates.alwaysTrue();
        }
        return Predicates.compose(predicate, PaperCard.FN_GET_RULES);
    }
}
