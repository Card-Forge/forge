package forge.itemmanager.views;

import forge.Forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.item.InventoryItem;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerModel;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

public abstract class ItemView<T extends InventoryItem> {
    private static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_TEXT);

    protected final ItemManager<T> itemManager;
    protected final ItemManagerModel<T> model;
    private final Scroller scroller = new Scroller();
    private final FLabel button;
    private final OptionsPanel pnlOptions = new OptionsPanel();

    private float heightBackup;
    private boolean isIncrementalSearchActive = false;

    protected ItemView(ItemManager<T> itemManager0, ItemManagerModel<T> model0) {
        itemManager = itemManager0;
        model = model0;
        button = new FLabel.Builder().selectable(true).align(HAlignment.CENTER)
                .iconScaleFactor(0.9f).icon(getIcon()).build();
    }

    private class Scroller extends FScrollPane {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            onResize(visibleWidth, visibleHeight);
            //scroll selection into view whenever view height changes
            if (visibleHeight != heightBackup) {
                heightBackup = visibleHeight;
                scrollSelectionIntoView();
            }
            return new ScrollBounds(visibleWidth, ItemView.this.getScrollHeight());
        }

        @Override
        public boolean tap(float x, float y, int count) {
            return ItemView.this.tap(x, y, count);
        }

        @Override
        public boolean zoom(float x, float y, float amount) {
            return ItemView.this.zoom(x, y, amount);
        }

        @Override
        public void drawOverlay(Graphics g) {
            g.drawRect(1.5f, BORDER_COLOR, 0, 0, getWidth(), getHeight());
        }
    }

    protected boolean tap(float x, float y, int count) {
        return false;
    }
    protected boolean zoom(float x, float y, float amount) {
        return false;
    }
    protected abstract float getScrollHeight();
    protected abstract float layoutOptionsPanel(float visibleWidth, float height);

    private class OptionsPanel extends FScrollPane {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            return new ScrollBounds(layoutOptionsPanel(visibleWidth, visibleHeight), visibleHeight);
        }

        @Override
        public void drawOverlay(Graphics g) {
            g.drawLine(1, BORDER_COLOR, 0, 0, getWidth(), 0);
        }
    }

    public void initialize(final int index) {
        button.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (button.isSelected()) {
                    itemManager.setViewIndex(index);
                }
                else {
                    button.setSelected(true); //prevent toggling off button
                }
            }
        });
    }

    public FLabel getButton() {
        return button;
    }

    public FScrollPane getScroller() {
        return scroller;
    }

    public FScrollPane getPnlOptions() {
        return pnlOptions;
    }

    public float getScrollValue() {
        return scroller.getScrollTop();
    }

    public void setScrollValue(float value) {
        scroller.setScrollTop(value);
    }

    public boolean isIncrementalSearchActive() {
        return isIncrementalSearchActive;
    }

    public void refresh(final Iterable<T> itemsToSelect, final int backupIndexToSelect, final float scrollValueToRestore) {
        model.refreshSort();
        onRefresh();
        fixSelection(itemsToSelect, backupIndexToSelect, scrollValueToRestore);
    }
    protected abstract void onResize(float visibleWidth, float visibleHeight);
    protected abstract void onRefresh();
    protected void fixSelection(final Iterable<T> itemsToSelect, final int backupIndexToSelect, final float scrollValueToRestore) {
        if (itemsToSelect == null) {
            setSelectedIndex(0, false); //select first item if no items to select
            setScrollValue(0); //ensure scrolled to top
        }
        else {
            if (!setSelectedItems(itemsToSelect)) {
                setSelectedIndex(backupIndexToSelect);
            }
        }
    }

    public final T getSelectedItem() {
        return getItemAtIndex(getSelectedIndex());
    }

    public final Collection<T> getSelectedItems() {
        List<T> items = new ArrayList<T>();
        for (Integer i : getSelectedIndices()) {
            T item = getItemAtIndex(i);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    public final boolean setSelectedItem(T item) {
        return setSelectedItem(item, true);
    }
    public final boolean setSelectedItem(T item, boolean scrollIntoView) {
        int index = getIndexOfItem(item);
        if (index != -1) {
            setSelectedIndex(index, scrollIntoView);
            return true;
        }
        return false;
    }

    public final boolean setSelectedItems(Iterable<T> items) {
        return setSelectedItems(items, true);
    }
    public final boolean setSelectedItems(Iterable<T> items, boolean scrollIntoView) {
        List<Integer> indices = new ArrayList<Integer>();
        for (T item : items) {
            int index = getIndexOfItem(item);
            if (index != -1) {
                indices.add(index);
            }
        }
        if (indices.size() > 0) {
            onSetSelectedIndices(indices);
            if (scrollIntoView) {
                scrollSelectionIntoView();
            }
            onSelectionChange();
            return true;
        }
        return false;
    }

    public void setSelectedIndex(int index) {
        setSelectedIndex(index, true);
    }
    public void setSelectedIndex(int index, boolean scrollIntoView) {
        int count = getCount();
        if (count == 0) { return; }

        if (index < 0) {
            index = 0;
        }
        else if (index >= count) {
            index = count - 1;
        }

        onSetSelectedIndex(index);
        if (scrollIntoView) {
            scrollSelectionIntoView();
        }

        onSelectionChange();
    }

    public void setSelectedIndices(Iterable<Integer> indices) {
        setSelectedIndices(indices, true);
    }
    public void setSelectedIndices(Iterable<Integer> indices, boolean scrollIntoView) {
        int count = getCount();
        if (count == 0) { return; }

        List<Integer> indexList = new ArrayList<Integer>();
        for (Integer index : indices) {
            if (index >= 0 && index < count) {
                indexList.add(index);
            }
        }

        if (indexList.isEmpty()) { //if no index in range, set selected index based on first index
            for (Integer index : indices) {
                setSelectedIndex(index);
                return;
            }
            return;
        }

        onSetSelectedIndices(indexList);
        if (scrollIntoView) {
            scrollSelectionIntoView();
        }

        onSelectionChange();
    }

    protected void onSelectionChange() {
        final int index = getSelectedIndex();
        if (index != -1) {
            if (itemManager.getSelectionChangedHandler() != null) {
                itemManager.getSelectionChangedHandler().handleEvent(new FEvent(itemManager, FEventType.CHANGE));
            }
        }
    }

    @Override
    public String toString() {
        return getCaption(); //return caption as string for display in combo box
    }

    public abstract void setup(ItemManagerConfig config, Map<ColumnDef, ItemColumn> colOverrides);
    public abstract void setAllowMultipleSelections(boolean allowMultipleSelections);
    public abstract T getItemAtIndex(int index);
    public abstract int getIndexOfItem(T item);
    public abstract int getSelectedIndex();
    public abstract Iterable<Integer> getSelectedIndices();
    public abstract void selectAll();
    public abstract int getCount();
    public abstract int getSelectionCount();
    public abstract int getIndexAtPoint(float x, float y);
    public abstract void scrollSelectionIntoView();
    protected abstract FImage getIcon();
    protected abstract String getCaption();
    protected abstract void onSetSelectedIndex(int index);
    protected abstract void onSetSelectedIndices(Iterable<Integer> indices);
}
