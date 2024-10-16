package forge.itemmanager.filters;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import forge.gui.GuiBase;
import forge.gui.UiCommand;
import forge.item.InventoryItem;
import forge.item.ItemPredicate;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
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

        int imageSize = Math.round(18 * GuiBase.getInterface().getScreenScale());
        final FLabel button = addToggleButton(widget, FSkin.getImage(st.skinProp, imageSize, imageSize), tooltip.toString());
        buttonMap.put(st, button);

        //hook so right-clicking a button toggles itself on and toggles off all other buttons
        button.setRightClickCommand((UiCommand) () -> {
            lockFiltering = true;
            SFilterUtil.showOnlyStat(st, button, buttonMap);
            lockFiltering = false;
            applyChange();
        });
    }

    @Override
    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        FLabel btnPackOrDeck = buttonMap.get(StatTypes.PACK_OR_DECK); //support special pack/deck case
        if (btnPackOrDeck != null && btnPackOrDeck.isSelected()) {
            return ItemPredicate.IS_PACK_OR_DECK.test(item);
        }
        return false;
    }

    @Override
    public void afterFiltersApplied() {
        final ItemPool<? super T> items = itemManager.getFilteredItems();

        FLabel btnPackOrDeck = buttonMap.get(StatTypes.PACK_OR_DECK);
        if (btnPackOrDeck != null) { //support special pack/deck case
            int count = items.countAll(ItemPredicate.IS_PACK_OR_DECK, InventoryItem.class);
            btnPackOrDeck.setText(String.valueOf(count));
        }

        for (StatTypes statTypes : buttonMap.keySet()) {
            if (statTypes.predicate != null) {
                int count = items.countAll(PaperCardPredicates.fromRules(statTypes.predicate), PaperCard.class);
                buttonMap.get(statTypes).setText(String.valueOf(count));
            }
        }

        getWidget().revalidate();
    }
}
