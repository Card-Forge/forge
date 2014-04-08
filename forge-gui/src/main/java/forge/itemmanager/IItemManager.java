package forge.itemmanager;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import forge.item.InventoryItem;
import forge.util.ItemPool;

public interface IItemManager<T extends InventoryItem> {
    ItemManagerConfig getConfig();
    void setup(ItemManagerConfig config0);
    Class<T> getGenericType();
    String getCaption();
    void setCaption(String caption);
    ItemPool<T> getPool();
    void setPool(final Iterable<T> items);
    void setPool(final ItemPool<T> poolView, boolean infinite);
    void setPool(final ItemPool<T> pool0);
    int getItemCount();
    int getSelectionCount();
    T getSelectedItem();
    Collection<T> getSelectedItems();
    ItemPool<T> getSelectedItemPool();
    boolean setSelectedItem(T item);
    boolean setSelectedItems(Iterable<T> items);
    T stringToItem(String str);
    boolean setSelectedString(String str);
    boolean setSelectedStrings(Iterable<String> strings);
    boolean selectItemEntrys(Iterable<Entry<T, Integer>> itemEntrys);
    void selectAll();
    int getSelectedIndex();
    Iterable<Integer> getSelectedIndices();
    void setSelectedIndex(int index);
    void setSelectedIndices(Integer[] indices);
    void setSelectedIndices(Iterable<Integer> indices);
    void addItem(final T item, int qty);
    void addItems(Iterable<Entry<T, Integer>> itemsToAdd);
    void removeItem(final T item, int qty);
    void removeItems(Iterable<Map.Entry<T, Integer>> itemsToRemove);
    void removeAllItems();
    void scrollSelectionIntoView();
    int getItemCount(final T item);
    ItemPool<T> getFilteredItems();
    boolean applyFilters();
}
