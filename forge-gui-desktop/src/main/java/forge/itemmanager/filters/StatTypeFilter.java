package forge.itemmanager.filters;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

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
import forge.toolbox.FUiScale;
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

        int imageSize = Math.round(18 * FUiScale.of(widget));
        final FLabel button = addToggleButton(widget, FSkin.getImage(st.skinProp, imageSize, imageSize), tooltip.toString());
        buttonMap.put(st, button);
        FUiScale.addScaleChangeListener(new IconScaleListener(widget, button, st, imageSize));

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

    /** Rebuilds a construction-time icon when the output scale changes, without retaining the UI. */
    private static final class IconScaleListener implements Runnable {
        private final WeakReference<JPanel> widgetRef;
        private final WeakReference<FLabel> buttonRef;
        private final StatTypes stat;
        private int size;

        private IconScaleListener(final JPanel widget, final FLabel button, final StatTypes stat, final int size) {
            widgetRef = new WeakReference<>(widget);
            buttonRef = new WeakReference<>(button);
            this.stat = stat;
            this.size = size;
        }

        @Override
        public void run() {
            JPanel widget = widgetRef.get();
            FLabel button = buttonRef.get();
            if (widget == null || button == null) {
                FUiScale.removeScaleChangeListener(this);
                return;
            }
            int imageSize = Math.round(18 * FUiScale.of(widget));
            if (imageSize == size) { return; }
            size = imageSize;
            button.setIcon(FSkin.getImage(stat.skinProp, imageSize, imageSize));
            button.revalidate();
            button.repaint();
        }
    }
}
