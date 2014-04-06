package forge.itemmanager.filters;

import com.google.common.base.Predicate;

import forge.Forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.toolbox.FCheckBox;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.utils.LayoutHelper;


public abstract class ItemFilter<T extends InventoryItem> {
    public final static float PANEL_HEIGHT = 28;
    private static final float REMOVE_BUTTON_SIZE = 17;
    private final static float PADDING = 3;
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);

    protected final ItemManager<? super T> itemManager;
    private FilterPanel panel;
    private Widget widget;
    private final FCheckBox chkEnable = new FCheckBox();
    private RemoveButton btnRemove;

    protected ItemFilter(ItemManager<? super T> itemManager0) {
        itemManager = itemManager0;
        chkEnable.setSelected(true); //enable by default
    }

    public FilterPanel getPanel() {
        if (panel == null) {
            panel = new FilterPanel();

            chkEnable.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    updateEnabled();
                    applyChange();
                }
            });
            panel.add(chkEnable);

            getWidget(); //initialize widget
            if (!isEnabled()) {
                updateEnabled();
            }
            panel.add(widget);

            btnRemove = new RemoveButton();
            panel.add(btnRemove);
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

    public void setNumber(int number) {
        chkEnable.setText("(" + number + ")");
    }

    public boolean isEnabled() {
        return chkEnable.isSelected();
    }

    public void setEnabled(boolean enabled0) {
        chkEnable.setSelected(enabled0);
    }

    public void updateEnabled() {
        boolean enabled = isEnabled();
        for (FDisplayObject child : widget.getChildren()) {
            child.setEnabled(enabled);
        }
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
            float x = PADDING;
            float y = PADDING;
            float w = width - 2 * PADDING;
            float h = height - 2 * PADDING;
            chkEnable.setBounds(x, y, 43, h);
            x += chkEnable.getWidth();
            widget.setBounds(x, y, w - REMOVE_BUTTON_SIZE - x, h);
            x += widget.getWidth();
            btnRemove.setBounds(x, y, REMOVE_BUTTON_SIZE, height);
        }

        public void drawOverlay(Graphics g) {
            float y = getHeight();
            g.drawLine(1, FORE_COLOR, 0, y, getWidth(), y);
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

    private class RemoveButton extends FLabel {
        private RemoveButton() {
            super(new FLabel.Builder()
                .command(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        itemManager.removeFilter(ItemFilter.this);
                    }
                }));
            setIcon(new RemoveIcon());
        }

        private class RemoveIcon implements FImage {
            @Override
            public float getWidth() {
                return REMOVE_BUTTON_SIZE;
            }

            @Override
            public float getHeight() {
                return REMOVE_BUTTON_SIZE;
            }

            @Override
            public void draw(forge.Forge.Graphics g, float x, float y, float w, float h) {
                float thickness = 2;
                float offset = 4;
                float x1 = offset;
                float y1 = offset;
                float x2 = w - offset - 1;
                float y2 = h - offset - 1;

                if (!RemoveButton.this.isPressed()) {
                    g.setAlphaComposite(0.6f);
                }

                g.drawLine(thickness, FORE_COLOR, x1, y1, x2, y2);
                g.drawLine(thickness, FORE_COLOR, x2, y1, x1, y2);
                
                if (!RemoveButton.this.isPressed()) {
                    g.resetAlphaComposite();
                }
            }
        }
    }
}
