package forge.itemmanager.filters;

import com.google.common.base.Predicate;
import forge.assets.FSkinFont;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;


public abstract class ItemFilter<T extends InventoryItem> {
    public static final float PADDING = Utils.scale(3);
    public static final FSkinFont DEFAULT_FONT = FSkinFont.get(11);

    protected final ItemManager<? super T> itemManager;
    private Widget widget;

    protected ItemFilter(ItemManager<? super T> itemManager0) {
        itemManager = itemManager0;
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

    public float getPreferredWidth(float maxWidth, float height) {
        return maxWidth; //use maximum width by default
    }

    protected abstract void buildWidget(Widget widget);
    protected abstract void doWidgetLayout(float width, float height);
    protected abstract Predicate<T> buildPredicate();

    public class Widget extends FContainer {
        private Widget() {
        }

        @Override
        protected void doLayout(float width, float height) {
            doWidgetLayout(width, height);
        }
    }
}
