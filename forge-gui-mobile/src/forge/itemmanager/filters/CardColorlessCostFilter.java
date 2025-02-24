package forge.itemmanager.filters;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.itemmanager.ItemManager;

import java.util.function.Predicate;


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
            return x -> true;
        }
        return PaperCardPredicates.fromRules(predicate);
    }
}
