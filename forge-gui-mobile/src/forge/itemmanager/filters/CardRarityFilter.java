package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.card.CardRarity;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;

public class CardRarityFilter extends ComboBoxFilter<PaperCard, CardRarity> {
    public CardRarityFilter(ItemManager<? super PaperCard> itemManager0) {
        super("Any Rarity", CardRarity.FILTER_OPTIONS, itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        CardRarityFilter copy = new CardRarityFilter(itemManager);
        copy.filterValue = filterValue;
        return copy;
    }

    @Override
    protected String getDisplayText(CardRarity value) {
        return CardRarity.FN_GET_LONG_NAME.apply(value);
    }

    @Override
    protected Predicate<PaperCard> buildPredicate() {
        return new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard input) {
                if (filterValue == null) {
                    return true;
                }
                return input.getRarity() == filterValue;
            }
        };
    }
}
