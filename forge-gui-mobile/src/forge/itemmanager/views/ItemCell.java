package forge.itemmanager.views;

import com.google.common.base.Function;

import forge.item.InventoryItem;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemColumnConfig;
import forge.itemmanager.ItemColumnConfig.SortState;
import forge.itemmanager.ItemManagerConfig;

import java.util.Map;
import java.util.Map.Entry;

/**
 * A column object in a EditorTableModel in the card editor.
 * Requires a sorting function and a display function
 * (to extract information as appropriate for table row data).
 */
public class ItemCell {
    private final ItemColumn itemColumn;
    private ItemCellRenderer cellRenderer;

    public ItemCell(ItemColumn itemColumn0) {
        super();

        itemColumn = itemColumn0;
        ColumnDef def = itemColumn.getConfig().getDef();
        cellRenderer = ItemCellRenderer.getColumnDefRenderer(def);
    }

    public ItemColumn getItemColumn() {
        return itemColumn;
    }

    public String getShortName() {
        return itemColumn.getConfig().getShortName();
    }

    public String getLongName() {
        return itemColumn.getConfig().getLongName();
    }

    public int getIndex() {
        return itemColumn.getConfig().getIndex();
    }

    public void setIndex(final int index0) {
        itemColumn.getConfig().setIndex(index0);
    }

    public int getSortPriority() {
        return itemColumn.getConfig().getSortPriority();
    }

    public void setSortPriority(final int sortPriority0) {
        itemColumn.getConfig().setSortPriority(sortPriority0);
    }

    public SortState getSortState() {
        return itemColumn.getConfig().getSortState();
    }

    public void setSortState(final SortState state0) {
        itemColumn.getConfig().setSortState(state0);
    }

    public SortState getDefaultSortState() {
        return itemColumn.getConfig().getDefaultSortState();
    }

    public boolean isVisible() {
        return itemColumn.getConfig().isVisible();
    }

    public void setVisible(boolean visible0) {
        itemColumn.getConfig().setVisible(visible0);
    }

    public Function<Entry<InventoryItem, Integer>, Comparable<?>> getFnSort() {
        return itemColumn.getFnSort();
    }

    public Function<Entry<? extends InventoryItem, Integer>, Object> getFnDisplay() {
        return itemColumn.getFnDisplay();
    }

    public ItemCellRenderer getCellRenderer() {
        return cellRenderer;
    }
    public void setCellRenderer(ItemCellRenderer cellRenderer0) {
        cellRenderer = cellRenderer0;
    }

    @Override
    public String toString() {
        return itemColumn.toString();
    }

    public static void addColOverride(ItemManagerConfig config, Map<ColumnDef, ItemCell> colOverrides, ColumnDef colDef) {
        ItemColumnConfig colConfig = config.getCols().get(colDef);
        addColOverride(config, colOverrides, colDef, colConfig.getFnSort(), colConfig.getFnDisplay());
    }
    public static void addColOverride(ItemManagerConfig config, Map<ColumnDef, ItemCell> colOverrides, ColumnDef colDef,
            Function<Entry<InventoryItem, Integer>, Comparable<?>> fnSort0,
            Function<Entry<? extends InventoryItem, Integer>, Object> fnDisplay0) {
        colOverrides.put(colDef, new ItemCell(new ItemColumn(config.getCols().get(colDef), fnSort0, fnDisplay0)));
    }
}
