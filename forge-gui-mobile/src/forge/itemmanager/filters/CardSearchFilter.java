package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;


public class CardSearchFilter extends TextSearchFilter<PaperCard> {
    private final boolean inName, inType, inText, inCost;

    public CardSearchFilter(ItemManager<? super PaperCard> itemManager0) {
        this(itemManager0, true, true, true, false);
    }

    public CardSearchFilter(ItemManager<? super PaperCard> itemManager0, boolean inName0, boolean inType0, boolean inText0, boolean inCost0) {
        super(itemManager0);
        inName = inName0;
        inType = inType0;
        inText = inText0;
        inCost = inCost0;
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        CardSearchFilter copy = new CardSearchFilter(itemManager, inName, inType, inText, inCost);
        copy.getWidget(); //initialize widget
        copy.txtSearch.setText(txtSearch.getText());
        return copy;
    }

    @Override
    protected Predicate<PaperCard> buildPredicate() {
        return SFilterUtil.buildTextFilter(
                txtSearch.getText(),
                false,
                inName,
                inType,
                inText,
                inCost);
    }

    @Override
    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        //fallback to regular item text filter if item not PaperCard
        return SFilterUtil.buildItemTextFilter(txtSearch.getText()).apply(item);
    }
}
