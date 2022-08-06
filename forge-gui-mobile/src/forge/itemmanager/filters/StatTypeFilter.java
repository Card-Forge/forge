package forge.itemmanager.filters;

import forge.assets.FSkin;
import forge.item.InventoryItem;
import forge.item.ItemPredicate;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.itemmanager.SItemManagerUtil;
import forge.itemmanager.SItemManagerUtil.StatTypes;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;

import java.util.HashMap;
import java.util.Map;

public abstract class StatTypeFilter<T extends InventoryItem> extends ToggleButtonsFilter<T> {
    protected final Map<SItemManagerUtil.StatTypes, FLabel> buttonMap;

    public StatTypeFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
        buttonMap = new HashMap<>();
    }

    protected void addToggleButton(Widget widget, final StatTypes st) {
        final ToggleButton button = addToggleButton(widget, FSkin.getImages().get(st.skinProp));
        buttonMap.put(st, button);

        //hook so long-pressing a button toggles itself on and toggles off all other buttons
        button.setLongPressHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                lockFiltering = true;
                SFilterUtil.showOnlyStat(st, button, buttonMap);
                lockFiltering = false;
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
}
