package forge.itemmanager.filters;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JPanel;

import com.google.common.base.Predicates;

import forge.gui.UiCommand;
import forge.item.InventoryItem;
import forge.item.ItemPredicate;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.itemmanager.SItemManagerUtil.StatTypes;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.util.ItemPool;
import forge.util.Localizer;

public abstract class StatTypeFilter<T extends InventoryItem> extends ToggleButtonsFilter<T> {
    protected final Map<StatTypes, FLabel> buttonMap;

    public StatTypeFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
        buttonMap = new HashMap<>();
    }

    @SuppressWarnings("serial")
    protected void addToggleButton(JPanel widget, final StatTypes st) {
        final Localizer localizer = Localizer.getInstance();
        StringBuilder tooltip = new StringBuilder();
        tooltip.append(st.label);
        tooltip.append(" (").append(localizer.getMessage("lblclicktotoogle")).append(" ");
        if (st.label.length() > 1 && !Character.isUpperCase(st.label.charAt(1))) {
            tooltip.append(st.label.substring(0, 1).toLowerCase());
            tooltip.append(st.label.substring(1));
        }
        else {
            tooltip.append(st.label);
        }
        tooltip.append(")");

        final FLabel button = addToggleButton(widget, FSkin.getImage(st.skinProp, 18, 18), tooltip.toString());
        buttonMap.put(st, button);

        //hook so right-clicking a button toggles itself on and toggles off all other buttons
        button.setRightClickCommand(new UiCommand() {
            @Override
            public void run() {
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

    @Override
    public void afterFiltersApplied() {
        final ItemPool<? super T> items = itemManager.getFilteredItems();

        FLabel btnPackOrDeck = buttonMap.get(StatTypes.PACK_OR_DECK);
        if (btnPackOrDeck != null) { //support special pack/deck case
            int count = items.countAll(ItemPredicate.Presets.IS_PACK_OR_DECK, InventoryItem.class);
            btnPackOrDeck.setText(String.valueOf(count));
        }

        Iterator<StatTypes> buttonMapStatsIterator = buttonMap.keySet().iterator();
        while (buttonMapStatsIterator.hasNext()){
            StatTypes statTypes = buttonMapStatsIterator.next();
            if (statTypes.predicate != null){
                int count = items.countAll(Predicates.compose(statTypes.predicate, PaperCard.FN_GET_RULES), PaperCard.class);
                buttonMap.get(statTypes).setText(String.valueOf(count));
            }
        }
        getWidget().revalidate();
    }
}
