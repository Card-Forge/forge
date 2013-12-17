package forge.gui.toolbox.itemmanager.filters;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import com.google.common.base.Predicates;
import forge.Command;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.gui.toolbox.itemmanager.SItemManagerUtil.StatTypes;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.util.ItemPoolView;

public abstract class StatTypeFilter<T extends InventoryItem> extends ToggleButtonsFilter<T> {
    protected final Map<SItemManagerUtil.StatTypes, FLabel> buttonMap;

    public StatTypeFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
        buttonMap = new HashMap<SItemManagerUtil.StatTypes, FLabel>();
    }

    @SuppressWarnings("serial")
    protected void addToggleButton(JPanel widget, final StatTypes st) {
        final FLabel button = addToggleButton(widget, st.toLabelString(), st.img);
        buttonMap.put(st, button);

        //hook so right-clicking a button toggles itself on and toggles off all other buttons
        button.setRightClickCommand(new Command() {
            @Override
            public void run() {
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
                                if (!btn.getValue().getSelected()) {
                                    btn.getValue().setSelected(true);
                                }
                                continue;
                            default:
                                break;
                            }
                        }
                        if (btn.getValue().getSelected()) {
                            foundSelected = true;
                            btn.getValue().setSelected(false);
                        }
                    }
                }
                if (!button.getSelected()) {
                    button.setSelected(true);
                }
                else if (!foundSelected) {
                    //if statLabel only label in group selected, re-select all other labels in group
                    for (Map.Entry<SItemManagerUtil.StatTypes, FLabel> btn : buttonMap.entrySet()) {
                        if (btn.getKey() != st) {
                            if (!btn.getValue().getSelected()) {
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
    public void afterFiltersApplied() {
        final ItemPoolView<? super T> items = itemManager.getFilteredItems();
        for (Map.Entry<SItemManagerUtil.StatTypes, FLabel> btn : buttonMap.entrySet()) {
            int count = items.countAll(Predicates.compose(btn.getKey().predicate, PaperCard.FN_GET_RULES), PaperCard.class);
            btn.getValue().setText(String.valueOf(count));
        }
        getWidget().revalidate();
    }
}
