package forge.gui.toolbox.itemmanager.filters;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardCMCFilter extends ValueRangeFilter<PaperCard> {
    public CardCMCFilter(ItemManager<PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardCMCFilter(itemManager);
    }

    @Override
    protected String getCaption() {
        return "CMC";
    }

    @Override
    public Predicate<PaperCard> buildPredicate() {
        Predicate<CardRules> predicate = getCardRulesFieldPredicate(CardRulesPredicates.LeafNumber.CardField.CMC);
        if (predicate == null) {
            return Predicates.alwaysTrue();
        }
        return Predicates.compose(predicate, PaperCard.FN_GET_RULES);
    }
}
