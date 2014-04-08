package forge.itemmanager;

import java.util.Map.Entry;

import com.google.common.base.Function;

import forge.item.InventoryItem;

public class ItemColumnConfig {
    public static enum SortState {
        NONE,
        ASC,
        DESC
    }

    private final ColumnDef def;
    private SortState sortState = SortState.NONE;
    private int preferredWidth;
    private int sortPriority = 0;
    private boolean visible = true;
    private int index = 0;
    private ItemColumnConfig defaults;

    public ItemColumnConfig(ColumnDef def0) {
        this.def = def0;
        this.preferredWidth = def0.preferredWidth;
    }
    private ItemColumnConfig(ItemColumnConfig from) {
        this.def = from.def;
        this.sortState = from.sortState;
        this.preferredWidth = from.preferredWidth;
        this.sortPriority = from.sortPriority;
        this.visible = from.visible;
        this.index = from.index;
    }

    public ColumnDef getDef() {
        return this.def;
    }

    public String getShortName() {
        return this.def.shortName;
    }

    public String getLongName() {
        return this.def.longName;
    }

    public int getPreferredWidth() {
        return this.preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth0) {
        this.preferredWidth = preferredWidth0;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(final int index0) {
        this.index = index0;
    }

    public int getSortPriority() {
        return sortPriority;
    }

    public void setSortPriority(final int sortPriority0) {
        int oldSortPriority = this.sortPriority;
        this.sortPriority = sortPriority0;
        if (sortPriority0 == 0) {
            this.sortState = SortState.NONE;
        }
        else if (oldSortPriority == 0) {
            this.sortState = def.sortState;
        }
    }

    public SortState getSortState() {
        return this.sortState;
    }

    public void setSortState(final SortState state0) {
        this.sortState = state0;
    }

    public SortState getDefaultSortState() {
        return this.def.sortState;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible0) {
        this.visible = visible0;
    }

    public Function<Entry<InventoryItem, Integer>, Comparable<?>> getFnSort() {
        return this.def.fnSort;
    }

    public Function<Entry<? extends InventoryItem, Integer>, Object> getFnDisplay() {
        return this.def.fnDisplay;
    }

    @Override
    public String toString() {
        return this.getLongName();
    }

    public void establishDefaults() {
        this.defaults = new ItemColumnConfig(this);
    }

    public ItemColumnConfig getDefaults() {
        return this.defaults;
    }
}
