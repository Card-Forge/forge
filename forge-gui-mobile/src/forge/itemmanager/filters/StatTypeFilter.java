package forge.itemmanager.filters;

import com.google.common.base.Predicates;

import forge.item.InventoryItem;
import forge.item.ItemPredicate;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
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
        final ToggleButton button = addToggleButton(widget, st.img);
        buttonMap.put(st, button);

        //hook so long-pressing a button toggles itself on and toggles off all other buttons
        button.setLongPressHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                lockFiltering = true;
                boolean foundSelected = false;
                for (Map.Entry<SItemManagerUtil.StatTypes, FLabel> btn : buttonMap.entrySet()) {
                    if (btn.getKey() != st) {
                        if (btn.getKey() == StatTypes.MULTICOLOR) {
                            switch (st) {
                            case WHITE:
                            case BLUE:
                            case BLACK:
                            case RED:
                            case GREEN:
                                //ensure multicolor filter selected after right-clicking a color filter
                                if (!btn.getValue().isSelected()) {
                                    btn.getValue().setSelected(true);
                                }
                                continue;
                            default:
                                break;
                            }
                        }
                        else if (btn.getKey() == StatTypes.DECK_MULTICOLOR) {
                            switch (st) {
                            case DECK_WHITE:
                            case DECK_BLUE:
                            case DECK_BLACK:
                            case DECK_RED:
                            case DECK_GREEN:
                                //ensure multicolor filter selected after right-clicking a color filter
                                if (!btn.getValue().isSelected()) {
                                    btn.getValue().setSelected(true);
                                }
                                continue;
                            default:
                                break;
                            }
                        }
                        if (btn.getValue().isSelected()) {
                            foundSelected = true;
                            btn.getValue().setSelected(false);
                        }
                    }
                }
                if (!button.isSelected()) {
                    button.setSelected(true);
                }
                else if (!foundSelected) {
                    //if statLabel only label in group selected, re-select all other labels in group
                    for (Map.Entry<SItemManagerUtil.StatTypes, FLabel> btn : buttonMap.entrySet()) {
                        if (btn.getKey() != st) {
                            if (!btn.getValue().isSelected()) {
                                btn.getValue().setSelected(true);
                            }
                        }
                    }
                }
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

        for (Map.Entry<SItemManagerUtil.StatTypes, FLabel> btn : buttonMap.entrySet()) {
            if (btn.getKey().predicate != null) {
                int count = items.countAll(Predicates.compose(btn.getKey().predicate, PaperCard.FN_GET_RULES), PaperCard.class);
                btn.getValue().setText(String.valueOf(count));
            }
        }
        getWidget().revalidate();
    }
}
