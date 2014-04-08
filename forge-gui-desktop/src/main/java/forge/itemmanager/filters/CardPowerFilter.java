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
            return Predicates.alwaysTrue();
        }
        predicate = Predicates.and(predicate, CardRulesPredicates.Presets.IS_CREATURE);
        return Predicates.compose(predicate, PaperCard.FN_GET_RULES);
    }
}
