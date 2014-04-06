package forge.itemmanager.views;

import forge.Forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerModel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class ItemView<T extends InventoryItem> {
    private static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_TEXT);

    protected final ItemManager<T> itemManager;
    protected final ItemManagerModel<T> model;
    private final FScrollPane scroller = new Scroller();;
    private final FLabel button;
    private final OptionsPanel pnlOptions = new OptionsPanel();

    private float heightBackup;
    private boolean isIncrementalSearchActive = false;

    protected ItemView(ItemManager<T> itemManager0, ItemManagerModel<T> model0) {
        itemManager = itemManager0;
        model = model0;
        button = new FLabel.Builder().selectable(true)
            .icon(getIcon()).iconScaleAuto(false).build();
    }

    private class Scroller extends FScrollPane {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            onResize();
            //scroll selection into view whenever view height changes
            if (visibleHeight != heightBackup) {
                heightBackup = visibleHeight;
                scrollSelectionIntoView();
            }
            return new ScrollBounds(visibleWidth, visibleHeight);
        }

        @Override
        public void drawOverlay(Graphics g) {
            g.drawLine(1, BORDER_COLOR, 0, 0, getWidth(), 0);
        }
    }

    private class OptionsPanel extends FScrollPane {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            // TODO Auto-generated method stub
            return new ScrollBounds(visibleWidth, visibleHeight);
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
    protected abstract void onResize();
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
    }

    protected void onSelectionChange() {
        final int index = getSelectedIndex();
        if (index != -1) {
            /*ListSelectionEvent event = new ListSelectionEvent(itemManager, index, index, false);
            for (ListSelectionListener listener : itemManager.getSelectionListeners()) {
                listener.valueChanged(event);
            }*/
        }
    }

    @Override
    public String toString() {
        return getCaption(); //return caption as string for display in combo box
    }

    public abstract FDisplayObject getComponent();
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
