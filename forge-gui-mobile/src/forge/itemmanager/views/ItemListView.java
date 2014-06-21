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

import forge.Graphics;
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
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FList;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.Map.Entry;


public final class ItemListView<T extends InventoryItem> extends ItemView<T> {
    private static final FSkinColor ROW_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
    private static final FSkinColor ALT_ROW_COLOR = ROW_COLOR.getContrastColor(-20);
    private static final FSkinColor SEL_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);

    private final ItemList list = new ItemList();
    private final ItemListModel listModel;
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
        getPnlOptions().setVisible(false); //hide options panel by default
        getScroller().add(list);
    }

    @Override
    public void setup(ItemManagerConfig config, Map<ColumnDef, ItemColumn> colOverrides) {
        //ensure cols ordered properly
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
            chkBox.setFont(list.getFont());
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

        list.cols.clear();

        int modelIndex = 0;
        for (final ItemColumn col : cols) {
            col.setIndex(modelIndex++);
            if (col.isVisible()) { list.cols.add(col); }

            final FCheckBox chkBox = new FCheckBox(StringUtils.isEmpty(col.getShortName()) ?
                    col.getLongName() : col.getShortName(), col.isVisible());
            chkBox.setFont(list.getFont());
            chkBox.setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    boolean visible = chkBox.isSelected();
                    if (col.isVisible() == visible) { return; }
                    col.setVisible(visible);

                    if (col.isVisible()) {
                        list.cols.add(col);

                        //move col into proper position
                        int oldIndex = list.getCellCount() - 1;
                        int newIndex = col.getIndex();
                        for (int i = 0; i < col.getIndex(); i++) {
                            if (!cols.get(i).isVisible()) {
                                newIndex--;
                            }
                        }
                        if (newIndex < oldIndex) {
                            list.cols.remove(oldIndex);
                            list.cols.add(newIndex, col);
                        }
                    }
                    else {
                        list.cols.remove(col);
                    }
                    ItemManagerConfig.save();
                }
            });
            getPnlOptions().add(chkBox);
        }

        listModel.setup();
        refresh(null, 0, 0);
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
        list.scrollIntoView(getSelectedIndex());
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
    protected void onResize(float visibleWidth, float visibleHeight) {
        list.setSize(visibleWidth, visibleHeight);
    }

    @Override
    protected void onRefresh() {
        list.setListData(model.getOrderedList());
    }

    @Override
    protected float getScrollHeight() {
        return getScroller().getHeight();
    }

    @Override
    protected float layoutOptionsPanel(float visibleWidth, float height) {
        return visibleWidth;
    }

    public final class ItemList extends FList<Entry<T, Integer>> {
        private final ItemManager<T>.ItemRenderer renderer;
        private List<ItemColumn> cols = new ArrayList<ItemColumn>();

        private ItemList() {
            renderer = itemManager.getListItemRenderer();
            setListItemRenderer(new ListItemRenderer<Map.Entry<T,Integer>>() {
                private Integer prevTapIndex = -1;

                @Override
                public float getItemHeight() {
                    return renderer.getItemHeight();
                }

                @Override
                public boolean tap(Integer index, Entry<T, Integer> value, float x, float y, int count) {
                    if (maxSelections > 1) {
                        if (selectedIndices.contains(index)) {
                            //allow removing selection if it won't fall below min
                            //or if max selected (since you need to be able to deselect an item before selecting a new item)
                            if (selectedIndices.size() > minSelections || selectedIndices.size() == maxSelections) {
                                selectedIndices.remove(index);
                                onSelectionChange();
                            }
                        }
                        else if (selectedIndices.size() < maxSelections) {
                            selectedIndices.add(index);
                            Collections.sort(selectedIndices); //ensure selected indices are sorted
                            onSelectionChange();
                        }
                    }
                    else {
                        setSelectedIndex(index);
                    }
                    if (renderer.tap(value, x, y, count)) {
                        prevTapIndex = index;
                        return true; //don't activate if renderer handles tap
                    }
                    if (count == 2 && index == prevTapIndex) {
                        itemManager.activateSelectedItems();
                    }
                    prevTapIndex = index;
                    return true;
                }

                @Override
                public void drawValue(Graphics g, Integer index, Entry<T, Integer> value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
                    if (maxSelections > 1) {
                        if (pressed) { //if multi-select mode, draw SEL_COLOR when pressed
                            g.fillRect(SEL_COLOR, x - FList.PADDING, y - FList.PADDING, w + 2 * FList.PADDING, h + 2 * FList.PADDING);
                        }
                        //draw checkbox, with it checked based on whether item is selected
                        float checkBoxSize = h / 2;
                        float padding = checkBoxSize / 2;
                        w -= checkBoxSize + padding;
                        FCheckBox.drawCheckBox(g, selectedIndices.contains(index), x + w, y + padding, checkBoxSize, checkBoxSize);
                        w -= padding;
                    }
                    renderer.drawValue(g, value, font, foreColor, pressed, x + 1, y, w - 2, h); //x + 1 and w - 2 to account for left and right borders
                }
            });
            setFont(FSkinFont.get(14));
        }

        public Iterable<ItemColumn> getCells() {
            return cols;
        }

        public int getCellCount() {
            return cols.size();
        }

        @Override
        protected void drawBackground(Graphics g) {
            //draw no background by default
        }

        @Override
        protected FSkinColor getItemFillColor(int index) {
            if (maxSelections == 1 && selectedIndices.contains(index)) {
                return SEL_COLOR; //don't show SEL_COLOR if in multi-select mode
            }
            if (index % 2 == 1) {
                return ALT_ROW_COLOR;
            }
            return ROW_COLOR;
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
            final ItemColumn[] sortcols = new ItemColumn[list.getCellCount()];

            // Assemble priority sort.
            for (ItemColumn col : list.getCells()) {
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
