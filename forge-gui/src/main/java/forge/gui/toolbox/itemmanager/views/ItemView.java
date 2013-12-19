package forge.gui.toolbox.itemmanager.views;

import java.awt.Point;
import java.util.List;

import javax.swing.JComponent;

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

    public void setSelectedItem(T item) {
        int index = this.getIndexOfItem(item);
        if (index != -1) {
            setSelectedIndex(index);
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
    public abstract int[] getSelectedIndices();
    public abstract T getSelectedItem();
    public abstract List<T> getSelectedItems();
    public abstract void setSelectedIndex(int index);
    public abstract void selectAll();
    public abstract int getCount();
    public abstract int getIndexAtPoint(Point p);
    protected abstract String getCaption();
}
