package forge.itemmanager.filters;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.assets.FSkinFont;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.itemmanager.SFilterUtil;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FTextField;
import forge.util.LayoutHelper;


public class TextSearchFilter<T extends InventoryItem> extends ItemFilter<T> {
    private static final FSkinFont FONT = FSkinFont.get(12);
    protected FTextField txtSearch;

    public TextSearchFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
    }

    @Override
    public ItemFilter<T> createCopy() {
        TextSearchFilter<T> copy = new TextSearchFilter<T>(itemManager);
        copy.getWidget(); //initialize widget
        copy.txtSearch.setText(this.txtSearch.getText());
        return copy;
    }

    @Override
    public boolean isEmpty() {
        return txtSearch.isEmpty();
    }

    @Override
    public void reset() {
        txtSearch.setText("");
    }

    @Override
    public FDisplayObject getMainComponent() {
        return txtSearch;
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    public boolean merge(ItemFilter<?> filter) {
        return false;
    }

    @Override
    protected void buildWidget(Widget widget) {
        txtSearch = new FTextField();
        txtSearch.setFont(FONT);
        txtSearch.setGhostText("Search");
        widget.add(txtSearch);

        txtSearch.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                applyChange();
            }
        });
    }

    @Override
    protected void doWidgetLayout(LayoutHelper helper) {
        helper.fillLine(txtSearch, helper.getParentHeight());
    }

    @Override
    protected Predicate<T> buildPredicate() {
        String text = txtSearch.getText();
        if (text.trim().isEmpty()) {
            return Predicates.alwaysTrue();
        }
        return SFilterUtil.buildItemTextFilter(text);
    }
}
