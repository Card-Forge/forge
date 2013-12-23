package forge.gui.toolbox.itemmanager.views;

import java.awt.Container;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JViewport;

import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.InventoryItem;

public abstract class ItemView<T extends InventoryItem> {
    private final ItemManager<T> itemManager;

    protected ItemView(ItemManager<T> itemManager0) {
        this.itemManager = itemManager0;
    }

    public ItemManager<T> getItemManager() {
        return this.itemManager;
    }

    public final T getSelectedItem() {
        int index = getSelectedIndex();
        return index >= 0 ? getItemAtIndex(index) : null;
    }

    public final Iterable<T> getSelectedItems() {
        List<T> items = new ArrayList<T>();
        for (Integer i : getSelectedIndices()) {
            items.add(getItemAtIndex(i));
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
        if (getCount() == 0) { return false; }

        List<Integer> indices = new ArrayList<Integer>();
        for (T item : items) {
            int index = getIndexOfItem(item);
            if (index != -1) {
                indices.add(index);
            }
        }
        if (indices.size() > 0) {
            setSelectedIndices(indices, scrollIntoView);
            return true;
        }
        return false;
    }

    public void setSelectedIndex(int index) {
        setSelectedIndex(index, true);
    }
    public void setSelectedIndex(int index, boolean scrollIntoView) {
        onSetSelectedIndex(index);
        if (scrollIntoView) {
            scrollSelectionIntoView();
        }
    }

    public void setSelectedIndices(Iterable<Integer> indices) {
        setSelectedIndices(indices, true);
    }
    public void setSelectedIndices(Iterable<Integer> indices, boolean scrollIntoView) {
        onSetSelectedIndices(indices);
        if (scrollIntoView) {
            scrollSelectionIntoView();
        }
    }

    public void scrollSelectionIntoView() {
        Container parent = getComponent().getParent();
        if (parent instanceof JViewport) {
            onScrollSelectionIntoView((JViewport)parent);
        }
    }

    public void focus() {
        this.getComponent().requestFocusInWindow();
    }

    public boolean hasFocus() {
        return this.getComponent().hasFocus();
    }

    @Override
    public String toString() {
        return this.getCaption(); //return caption as string for display in combo box
    }

    public abstract JComponent getComponent();
    public abstract void setAllowMultipleSelections(boolean allowMultipleSelections);
    public abstract T getItemAtIndex(int index);
    public abstract int getIndexOfItem(T item);
    public abstract int getSelectedIndex();
    public abstract Iterable<Integer> getSelectedIndices();
    public abstract void selectAll();
    public abstract int getCount();
    public abstract int getSelectionCount();
    public abstract int getIndexAtPoint(Point p);
    protected abstract String getCaption();
    protected abstract void onSetSelectedIndex(int index);
    protected abstract void onSetSelectedIndices(Iterable<Integer> indices);
    protected abstract void onScrollSelectionIntoView(JViewport viewport);
}
