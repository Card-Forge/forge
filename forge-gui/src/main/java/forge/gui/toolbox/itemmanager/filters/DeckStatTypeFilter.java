package forge.gui.toolbox.itemmanager.filters;

import java.util.Map;

import com.google.common.base.Predicates;

import forge.deck.Deck;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.util.ItemPoolView;

public abstract class DeckStatTypeFilter extends StatTypeFilter<Deck> {
    public DeckStatTypeFilter(ItemManager<? super Deck> itemManager0) {
        super(itemManager0);
    }

    @Override
    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        return false;
    }

    @Override
    public void afterFiltersApplied() {
        final ItemPoolView<? super Deck> items = itemManager.getFilteredItems();

        for (Map.Entry<SItemManagerUtil.StatTypes, FLabel> btn : buttonMap.entrySet()) {
            if (btn.getKey().predicate != null) {
                int count = items.countAll(Deck.createPredicate(Predicates.compose(btn.getKey().predicate, PaperCard.FN_GET_RULES)), Deck.class);
                btn.getValue().setText(String.valueOf(count));
            }
        }
        getWidget().revalidate();
    }
}
