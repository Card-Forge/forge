/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.itemmanager.views;

import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerModel;
import forge.toolbox.FCheckBox;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FList;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.Map.Entry;


public final class ItemListView<T extends InventoryItem> extends ItemView<T> {
    static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor SEL_ACTIVE_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);
    private static final FSkinColor SEL_INACTIVE_COLOR = FSkinColor.get(Colors.CLR_INACTIVE);
    private static final FSkinColor HEADER_BACK_COLOR = BACK_COLOR.getContrastColor(-10);
    static final FSkinColor ALT_ROW_COLOR = BACK_COLOR.getContrastColor(-20);
    private static final FSkinColor GRID_COLOR = BACK_COLOR.getContrastColor(20);
    private static final FSkinFont ROW_FONT = FSkinFont.get(12);
    private static final int ROW_HEIGHT = 19;

    private final ItemTable table = new ItemTable();
    private final ItemTableModel tableModel;
    private boolean allowMultipleSelections;
    private List<Integer> selectedIndices = new ArrayList<Integer>();

    public ItemTableModel getTableModel() {
        return tableModel;
    }

    /**
     * ItemTable Constructor.
     * 
     * @param itemManager0
     * @param model0
     */
    public ItemListView(ItemManager<T> itemManager0, ItemManagerModel<T> model0) {
        super(itemManager0, model0);
        tableModel = new ItemTableModel(model0);
        setAllowMultipleSelections(false);
        getPnlOptions().setVisible(false); //hide options panel by default
    }

    @Override
    public void setup(ItemManagerConfig config, Map<ColumnDef, ItemColumn> colOverrides) {
        final Iterable<T> selectedItemsBefore = getSelectedItems();

        //ensure columns ordered properly
        final List<ItemColumn> columns = new LinkedList<ItemColumn>();
        for (ItemColumnConfig colConfig : config.getCols().values()) {
            if (colOverrides == null || !colOverrides.containsKey(colConfig.getDef())) {
                columns.add(new ItemColumn(colConfig));
            }
            else {
                columns.add(colOverrides.get(colConfig.getDef()));
            }
        }
        Collections.sort(columns, new Comparator<ItemColumn>() {
            @Override
            public int compare(ItemColumn arg0, ItemColumn arg1) {
                return Integer.compare(arg0.getIndex(), arg1.getIndex());
            }
        });

        //hide table header if only showing single string column
        boolean hideHeader = (config.getCols().size() == 1 && config.getCols().containsKey(ColumnDef.STRING));

        getPnlOptions().clear();

        if (config.getShowUniqueCardsOption()) {
            final FCheckBox chkBox = new FCheckBox("Unique Cards Only", itemManager.getWantUnique());
            chkBox.setFontSize(ROW_FONT.getSize());
            chkBox.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    boolean wantUnique = chkBox.isSelected();
                    if (itemManager.getWantUnique() == wantUnique) { return; }
                    itemManager.setWantUnique(wantUnique);
                    itemManager.refresh();

                    if (itemManager.getConfig() != null) {
                        itemManager.getConfig().setUniqueCardsOnly(wantUnique);
                    }
                }
            });
            getPnlOptions().add(chkBox);
        }

        int modelIndex = 0;
        for (final ItemColumn col : columns) {
            col.setIndex(modelIndex++);
            if (col.isVisible()) { table.columns.add(col); }

            if (!hideHeader) {
                final FCheckBox chkBox = new FCheckBox(StringUtils.isEmpty(col.getShortName()) ?
                        col.getLongName() : col.getShortName(), col.isVisible());
                chkBox.setFontSize(ROW_FONT.getSize());
                chkBox.setCommand(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        boolean visible = chkBox.isSelected();
                        if (col.isVisible() == visible) { return; }
                        col.setVisible(visible);

                        if (col.isVisible()) {
                            table.columns.add(col);

                            //move column into proper position
                            int oldIndex = table.getColumnCount() - 1;
                            int newIndex = col.getIndex();
                            for (int i = 0; i < col.getIndex(); i++) {
                                if (!columns.get(i).isVisible()) {
                                    newIndex--;
                                }
                            }
                            if (newIndex < oldIndex) {
                                table.columns.remove(oldIndex);
                                table.columns.add(newIndex, col);
                            }
                        }
                        else {
                            table.columns.remove(col);
                        }
                        ItemManagerConfig.save();
                    }
                });
                getPnlOptions().add(chkBox);
            }
        }

        tableModel.setup();
        refresh(selectedItemsBefore, 0, 0);
    }

    public ItemTable getTable() {
        return table;
    }

    @Override
    public FDisplayObject getComponent() {
        return table;
    }

    @Override
    protected FImage getIcon() {
        return FSkinImage.LIST;
    }

    @Override
    protected String getCaption() {
        return "List View";
    }

    @Override
    public void setAllowMultipleSelections(boolean allowMultipleSelections0) {
        allowMultipleSelections = allowMultipleSelections0;
    }

    @Override
    public int getSelectedIndex() {
        if (selectedIndices.isEmpty()) {
            return -1;
        }
        return selectedIndices.get(0);
    }

    @Override
    public Iterable<Integer> getSelectedIndices() {
        return selectedIndices;
    }

    @Override
    protected void onSetSelectedIndex(int index) {
        selectedIndices.clear();
        selectedIndices.add(index);
    }

    @Override
    protected void onSetSelectedIndices(Iterable<Integer> indices) {
        selectedIndices.clear();
        for (Integer index : indices) {
            selectedIndices.add(index);
        }
    }

    @Override
    public void scrollSelectionIntoView() {
        //table.scrollIntoView(table.getItemAt(getSelectedIndex()));
    }

    @Override
    public void selectAll() {
    }

    @Override
    public int getIndexOfItem(T item) {
        return tableModel.itemToRow(item);
    }

    @Override
    public T getItemAtIndex(int index) {
        Entry<T, Integer> itemEntry = tableModel.rowToItem(index);
        return itemEntry != null ? itemEntry.getKey() : null;
    }

    @Override
    public int getCount() {
        return table.getCount();
    }

    @Override
    public int getSelectionCount() {
        return selectedIndices.size();
    }

    @Override
    public int getIndexAtPoint(float x, float y) {
        return 0; //TODO
    }

    @Override
    protected void onResize() {
    }

    @Override
    protected void onRefresh() {
    }

    public final class ItemTable extends FList<T> {
        private List<ItemColumn> columns = new ArrayList<ItemColumn>();

        private ItemTable() {
        }

        public Iterable<ItemColumn> getColumns() {
            return columns;
        }

        public int getColumnCount() {
            return columns.size();
        }
    }

    public final class ItemTableModel {
        private final ItemManagerModel<T> model;

        /**
         * Instantiates a new table model.
         * 
         * @param table0 &emsp; {@link forge.gui.ItemManager.ItemTable<T>}
         * @param model0 &emsp; {@link forge.gui.ItemManager.ItemManagerModel<T>}
         */
        public ItemTableModel(final ItemManagerModel<T> model0) {
            model = model0;
        }

        public void setup() {
            final ItemColumn[] sortcols = new ItemColumn[table.getColumnCount()];

            // Assemble priority sort.
            for (ItemColumn col : table.getColumns()) {
                if (col.getSortPriority() > 0 && col.getSortPriority() <= sortcols.length) {
                    sortcols[col.getSortPriority() - 1] = col;
                }
            }

            model.getCascadeManager().reset();

            for (int i = sortcols.length - 1; i >= 0; i--) {
                ItemColumn col = sortcols[i];
                if (col != null) {
                    model.getCascadeManager().add(col, true);
                }
            }
        }

        public Entry<T, Integer> rowToItem(final int row) {
            final List<Entry<T, Integer>> orderedList = model.getOrderedList();
            return (row >= 0) && (row < orderedList.size()) ? orderedList.get(row) : null;
        }

        public int itemToRow(final T item) { //TODO: Consider optimizing this if used frequently
            final List<Entry<T, Integer>> orderedList = model.getOrderedList();
            for (int i = 0; i < orderedList.size(); i++) {
                if (orderedList.get(i).getKey() == item) {
                    return i;
                }
            }
            return -1;
        }
    }
}
