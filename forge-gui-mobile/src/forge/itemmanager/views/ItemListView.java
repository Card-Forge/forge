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

import forge.Forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.item.InventoryItem;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemColumnConfig;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerModel;
import forge.toolbox.FCheckBox;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FList;
import forge.util.Utils;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.math.Vector2;

import java.util.*;
import java.util.Map.Entry;


public final class ItemListView<T extends InventoryItem> extends ItemView<T> {
    private static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
    private static final FSkinColor ALT_ROW_COLOR = BACK_COLOR.getContrastColor(-20);
    private static final FSkinColor SEL_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);
    private static final float ROW_HEIGHT = Utils.AVG_FINGER_HEIGHT + 12;
    private static final float ROW_PADDING = 5;

    private final ItemList list = new ItemList();
    private final ItemListModel listModel;
    private boolean allowMultipleSelections;
    private List<Integer> selectedIndices = new ArrayList<Integer>();

    public ItemListModel getListModel() {
        return listModel;
    }

    /**
     * ItemListView Constructor.
     * 
     * @param itemManager0
     * @param model0
     */
    public ItemListView(ItemManager<T> itemManager0, ItemManagerModel<T> model0) {
        super(itemManager0, model0);
        listModel = new ItemListModel(model0);
        setAllowMultipleSelections(false);
        getPnlOptions().setVisible(false); //hide options panel by default
    }

    @Override
    public void setup(ItemManagerConfig config, Map<ColumnDef, ItemColumn> colOverrides) {
        final Iterable<T> selectedItemsBefore = getSelectedItems();

        //ensure cells ordered properly
        final List<ItemColumn> cols = new LinkedList<ItemColumn>();
        for (ItemColumnConfig colConfig : config.getCols().values()) {
            if (colOverrides == null || !colOverrides.containsKey(colConfig.getDef())) {
                cols.add(new ItemColumn(colConfig));
            }
            else {
                cols.add(colOverrides.get(colConfig.getDef()));
            }
        }
        Collections.sort(cols, new Comparator<ItemColumn>() {
            @Override
            public int compare(ItemColumn arg0, ItemColumn arg1) {
                return Integer.compare(arg0.getConfig().getIndex(), arg1.getConfig().getIndex());
            }
        });

        getPnlOptions().clear();

        if (config.getShowUniqueCardsOption()) {
            final FCheckBox chkBox = new FCheckBox("Unique Cards Only", itemManager.getWantUnique());
            chkBox.setFontSize(list.getFontSize());
            chkBox.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    boolean wantUnique = chkBox.isSelected();
                    if (itemManager.getWantUnique() == wantUnique) { return; }
                    itemManager.setWantUnique(wantUnique);
                    itemManager.refresh();

                    if (itemManager.getConfig() != null) {
                        itemManager.setWantUnique(wantUnique);
                    }
                }
            });
            getPnlOptions().add(chkBox);
        }

        list.cells.clear();

        int modelIndex = 0;
        for (final ItemColumn col : cols) {
            final ItemCell cell = new ItemCell(col);
            col.setIndex(modelIndex++);
            if (col.isVisible()) { list.cells.add(cell); }

            final FCheckBox chkBox = new FCheckBox(StringUtils.isEmpty(col.getShortName()) ?
                    col.getLongName() : col.getShortName(), col.isVisible());
            chkBox.setFontSize(list.getFontSize());
            chkBox.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    boolean visible = chkBox.isSelected();
                    if (col.isVisible() == visible) { return; }
                    col.setVisible(visible);

                    if (col.isVisible()) {
                        list.cells.add(cell);

                        //move cell into proper position
                        int oldIndex = list.getCellCount() - 1;
                        int newIndex = col.getIndex();
                        for (int i = 0; i < col.getIndex(); i++) {
                            if (!cols.get(i).isVisible()) {
                                newIndex--;
                            }
                        }
                        if (newIndex < oldIndex) {
                            list.cells.remove(oldIndex);
                            list.cells.add(newIndex, cell);
                        }
                    }
                    else {
                        list.cells.remove(cell);
                    }
                    ItemManagerConfig.save();
                }
            });
            getPnlOptions().add(chkBox);
        }

        listModel.setup();
        refresh(selectedItemsBefore, 0, 0);
    }

    @Override
    public FDisplayObject getComponent() {
        return list;
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
        list.scrollIntoView(list.getItemAt(getSelectedIndex()));
    }

    @Override
    public void selectAll() {
        selectedIndices.clear();
        for (Integer i = 0; i < getCount(); i++) {
            selectedIndices.add(i);
        }
        onSelectionChange();
    }

    @Override
    public int getIndexOfItem(T item) {
        return listModel.itemToRow(item);
    }

    @Override
    public T getItemAtIndex(int index) {
        Entry<T, Integer> itemEntry = listModel.rowToItem(index);
        return itemEntry != null ? itemEntry.getKey() : null;
    }

    @Override
    public int getCount() {
        return list.getCount();
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
        list.setListData(model.getOrderedList());
    }

    public final class ItemList extends FList<Entry<T, Integer>> {
        private List<ItemCell> cells = new ArrayList<ItemCell>();

        private ItemList() {
            setListItemRenderer(new ListItemRenderer<Map.Entry<T,Integer>>() {
                @Override
                public float getItemHeight() {
                    return ROW_HEIGHT;
                }

                @Override
                public boolean tap(Entry<T, Integer> value, float x, float y, int count) {
                    int index = list.getIndexOf(value);
                    if (allowMultipleSelections) {
                        if (selectedIndices.contains(index)) {
                            selectedIndices.remove(index);
                        }
                        else {
                            selectedIndices.add(index);
                        }
                        onSelectionChange();
                    }
                    else {
                        setSelectedIndex(index);
                    }
                    if (count == 2) {
                        itemManager.activateSelectedItems();
                    }
                    return true;
                }

                @Override
                public void drawValue(Graphics g, Entry<T, Integer> value, FSkinFont font, FSkinColor foreColor, float width, float height) {
                    Vector2 loc = new Vector2(ROW_PADDING, ROW_PADDING);
                    for (ItemCell cell : cells) {
                        cell.getCellRenderer().draw(g, cell.getFnDisplay().apply(value), font, foreColor, loc, width, height);
                    }
                }
            });
            setFontSize(12);
        }

        public Iterable<ItemCell> getCells() {
            return cells;
        }

        public int getCellCount() {
            return cells.size();
        }

        @Override
        protected void drawBackground(Graphics g) {
            g.fillRect(BACK_COLOR, 0, 0, getWidth(), getHeight());
        }

        @Override
        protected FSkinColor getItemFillColor(ListItem item) {
            int index = Math.round(item.getTop() / ROW_HEIGHT); //more efficient indexing strategy
            if (selectedIndices.contains(index)) {
                return SEL_COLOR;
            }
            if (index % 2 == 1) {
                return ALT_ROW_COLOR;
            }
            return null;
        }

        @Override
        protected boolean drawLineSeparators() {
            return false;
        }
    }

    public final class ItemListModel {
        private final ItemManagerModel<T> model;

        /**
         * Instantiates a new list model.
         * 
         * @param model0 &emsp; {@link forge.gui.ItemManager.ItemManagerModel<T>}
         */
        public ItemListModel(final ItemManagerModel<T> model0) {
            model = model0;
        }

        public void setup() {
            final ItemCell[] sortcells = new ItemCell[list.getCellCount()];

            // Assemble priority sort.
            for (ItemCell cell : list.getCells()) {
                if (cell.getSortPriority() > 0 && cell.getSortPriority() <= sortcells.length) {
                    sortcells[cell.getSortPriority() - 1] = cell;
                }
            }

            model.getCascadeManager().reset();

            for (int i = sortcells.length - 1; i >= 0; i--) {
                ItemCell cell = sortcells[i];
                if (cell != null) {
                    model.getCascadeManager().add(cell.getItemColumn(), true);
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
