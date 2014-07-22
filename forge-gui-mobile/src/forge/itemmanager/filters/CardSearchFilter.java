package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;


public class CardSearchFilter extends TextSearchFilter<PaperCard> {
    public CardSearchFilter(ItemManager<? super PaperCard> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        CardSearchFilter copy = new CardSearchFilter(itemManager);
        copy.getWidget(); //initialize widget
        copy.txtSearch.setText(txtSearch.getText());
        return copy;
    }

    @Override
    protected Predicate<PaperCard> buildPredicate() {
        return SFilterUtil.buildTextFilter(
                txtSearch.getText(),
                false,
                true,
                true,
                true,
                false); //TODO: Support enabling searching in cost
    }

    @Override
    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        //fallback to regular item text filter if item not PaperCard
        return SFilterUtil.buildItemTextFilter(txtSearch.getText()).apply(item);
    }
}
