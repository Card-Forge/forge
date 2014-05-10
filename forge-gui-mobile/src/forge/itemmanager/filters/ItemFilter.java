package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FTextField;
import forge.util.LayoutHelper;
import forge.util.Utils;


public abstract class ItemFilter<T extends InventoryItem> {
    public static final float PADDING = Utils.scaleY(3);
    public static final int DEFAULT_FONT_SIZE = 11;
    public static final float PANEL_HEIGHT = FTextField.getDefaultHeight(DEFAULT_FONT_SIZE) + PADDING;

    protected final ItemManager<? super T> itemManager;
    private FilterPanel panel;
    private Widget widget;

    protected ItemFilter(ItemManager<? super T> itemManager0) {
        itemManager = itemManager0;
    }

    public FilterPanel getPanel() {
        if (panel == null) {
            panel = new FilterPanel();
            panel.add(getWidget());
        }
        return panel;
    }

    public Widget getWidget() {
        if (widget == null) {
            widget = new Widget();
            buildWidget(widget);
        }
        return widget;
    }

    public void refreshWidget() {
        if (widget == null) { return; }
        widget.clear();
        buildWidget(widget);
    }

    public FDisplayObject getMainComponent() {
        return getWidget();
    }

    protected void applyChange() {
        itemManager.applyFilters();
    }

    public final <U extends InventoryItem> Predicate<U> buildPredicate(Class<U> genericType) {
        final Predicate<T> predicate = buildPredicate();
        return new Predicate<U>() {
            @SuppressWarnings("unchecked")
            @Override
            public boolean apply(U item) {
                try {
                    return predicate.apply((T)item);
                }
                catch (Exception ex) {
                    return showUnsupportedItem(item); //if can't cast U to T, filter item out unless derived class can handle it
                }
            }
        };
    }

    protected <U extends InventoryItem> boolean showUnsupportedItem(U item) {
        return false; //don't show unsupported items by default
    }

    public abstract ItemFilter<T> createCopy();
    public abstract boolean isEmpty();
    public abstract void reset();
    public void afterFiltersApplied() {
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    public abstract boolean merge(ItemFilter<?> filter);

    protected abstract void buildWidget(Widget widget);
    protected abstract void doWidgetLayout(LayoutHelper helper);
    protected abstract Predicate<T> buildPredicate();

    public class FilterPanel extends FContainer {
        private FilterPanel() {
        }

        @Override
        protected void doLayout(float width, float height) {
            widget.setBounds(0, PADDING, width, height - PADDING);
        }
    }

    public class Widget extends FContainer {
        private Widget() {
        }

        @Override
        protected void doLayout(float width, float height) {
            LayoutHelper helper = new LayoutHelper(this);
            doWidgetLayout(helper);
        }
    }
}
