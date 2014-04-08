package forge.itemmanager.filters;

import com.google.common.base.Predicates;

import forge.deck.DeckProxy;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SItemManagerUtil.StatTypes;
import forge.toolbox.FLabel;
import forge.util.ItemPool;

import java.util.Map;

public abstract class DeckStatTypeFilter extends StatTypeFilter<DeckProxy> {
    public DeckStatTypeFilter(ItemManager<? super DeckProxy> itemManager0) {
        super(itemManager0);
    }

    @Override
    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        return false;
    }

    @Override
    public void afterFiltersApplied() {
        final ItemPool<? super DeckProxy> items = itemManager.getFilteredItems();

        for (Map.Entry<StatTypes, FLabel> btn : buttonMap.entrySet()) {
            if (btn.getKey().predicate != null) {
                int count = items.countAll(DeckProxy.createPredicate(Predicates.compose(btn.getKey().predicate, PaperCard.FN_GET_RULES)), DeckProxy.class);
                btn.getValue().setText(String.valueOf(count));
            }
        }
        getWidget().revalidate();
    }
}
