package forge.itemmanager.filters;

import com.google.common.base.Predicates;

import forge.assets.FSkin;
import forge.item.InventoryItem;
import forge.item.ItemPredicate;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.itemmanager.SItemManagerUtil;
import forge.itemmanager.SItemManagerUtil.StatTypes;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.ItemPool;

import java.util.HashMap;
import java.util.Map;

public abstract class StatTypeFilter<T extends InventoryItem> extends ToggleButtonsFilter<T> {
    protected final Map<SItemManagerUtil.StatTypes, FLabel> buttonMap;

    public StatTypeFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
        buttonMap = new HashMap<SItemManagerUtil.StatTypes, FLabel>();
    }

    protected void addToggleButton(Widget widget, final StatTypes st) {
        final ToggleButton button = addToggleButton(widget, FSkin.getImages().get(st.skinProp));
        buttonMap.put(st, button);

        //hook so pressing a selected button toggles off all other buttons while remaining toggled
        button.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (lockFiltering) { return; }

                if (!button.isSelected()) {
                    lockFiltering = true;
                    button.setSelected(true);
                    SFilterUtil.showOnlyStat(st, button, buttonMap);
                    lockFiltering = false;
                }
                applyChange();
            }
        });
    }

    @Override
    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        FLabel btnPackOrDeck = buttonMap.get(StatTypes.PACK_OR_DECK); //support special pack/deck case
        if (btnPackOrDeck != null && btnPackOrDeck.isSelected()) {
            return ItemPredicate.Presets.IS_PACK_OR_DECK.apply(item);
        }
        return false;
    }

    @Override
    public void afterFiltersApplied() {
        final ItemPool<? super T> items = itemManager.getFilteredItems();

        FLabel btnPackOrDeck = buttonMap.get(StatTypes.PACK_OR_DECK);
        if (btnPackOrDeck != null) { //support special pack/deck case
            int count = items.countAll(ItemPredicate.Presets.IS_PACK_OR_DECK, InventoryItem.class);
            btnPackOrDeck.setText(String.valueOf(count));
        }

        for (Map.Entry<SItemManagerUtil.StatTypes, FLabel> btn : buttonMap.entrySet()) {
            if (btn.getKey().predicate != null) {
                int count = items.countAll(Predicates.compose(btn.getKey().predicate, PaperCard.FN_GET_RULES), PaperCard.class);
                btn.getValue().setText(String.valueOf(count));
            }
        }
        getWidget().revalidate();
    }
}
