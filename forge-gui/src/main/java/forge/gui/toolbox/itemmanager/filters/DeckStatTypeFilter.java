package forge.gui.toolbox.itemmanager.filters;

import java.util.Map;

import com.google.common.base.Predicates;

import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.item.DeckBox;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.util.ItemPoolView;

public abstract class DeckStatTypeFilter extends StatTypeFilter<DeckBox> {
    public DeckStatTypeFilter(ItemManager<? super DeckBox> itemManager0) {
        super(itemManager0);
    }

    @Override
    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        return false;
    }

    @Override
    public void afterFiltersApplied() {
        final ItemPoolView<? super DeckBox> items = itemManager.getFilteredItems();

        for (Map.Entry<SItemManagerUtil.StatTypes, FLabel> btn : buttonMap.entrySet()) {
            if (btn.getKey().predicate != null) {
                int count = items.countAll(DeckBox.createPredicate(Predicates.compose(btn.getKey().predicate, PaperCard.FN_GET_RULES)), DeckBox.class);
                btn.getValue().setText(String.valueOf(count));
            }
        }
        getWidget().revalidate();
    }
}
