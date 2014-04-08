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
public class CardToughnessFilter extends ValueRangeFilter<PaperCard> {
    public CardToughnessFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardToughnessFilter(itemManager);
    }

    @Override
    protected String getCaption() {
        return "Toughness";
    }

    @Override
    protected Predicate<PaperCard> buildPredicate() {
        Predicate<CardRules> predicate = getCardRulesFieldPredicate(CardRulesPredicates.LeafNumber.CardField.TOUGHNESS);
        if (predicate == null) {
            return Predicates.alwaysTrue();
        }
        predicate = Predicates.and(predicate, CardRulesPredicates.Presets.IS_CREATURE);
        return Predicates.compose(predicate, PaperCard.FN_GET_RULES);
    }
}
