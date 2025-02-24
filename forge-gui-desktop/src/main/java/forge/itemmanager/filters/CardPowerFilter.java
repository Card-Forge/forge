package forge.itemmanager.filters;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.itemmanager.ItemManager;

import java.util.function.Predicate;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardPowerFilter extends ValueRangeFilter<PaperCard> {
    public CardPowerFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardPowerFilter(itemManager);
    }

    @Override
    protected String getCaption() {
        return "Power";
    }

    @Override
    protected Predicate<PaperCard> buildPredicate() {
        Predicate<CardRules> predicate = getCardRulesFieldPredicate(CardRulesPredicates.LeafNumber.CardField.POWER);
        if (predicate == null) {
            return x -> true;
        }
        predicate = predicate.and(CardRulesPredicates.IS_CREATURE);
        return PaperCardPredicates.fromRules(predicate);
    }
}
