package forge.itemmanager.filters;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.itemmanager.ItemManager;

import java.util.function.Predicate;


public class CardCMCFilter extends ValueRangeFilter<PaperCard> {
    public CardCMCFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardCMCFilter(itemManager);
    }

    @Override
    protected String getCaption() {
        return "Mana Value";
    }

    @Override
    protected Predicate<PaperCard> buildPredicate() {
        Predicate<CardRules> predicate = getCardRulesFieldPredicate(CardRulesPredicates.LeafNumber.CardField.CMC);
        if (predicate == null) {
            return x -> true;
        }
        return PaperCardPredicates.fromRules(predicate);
    }
}
