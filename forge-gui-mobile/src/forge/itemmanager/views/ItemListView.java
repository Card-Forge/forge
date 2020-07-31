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
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerModel;
import forge.toolbox.FCheckBox;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FList;

import com.badlogic.gdx.math.Rectangle;
import forge.util.Localizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public final class ItemListView<T extends InventoryItem> extends ItemView<T> {
    private static final FSkinColor ROW_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
    private static final FSkinColor ALT_ROW_COLOR = ROW_COLOR.getContrastColor(-20);
    private static final FSkinColor SEL_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);

    private final ItemList list = new ItemList();
    private final ItemListModel listModel;
    private List<Integer> selectedIndices = new ArrayList<>();

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
        list.compactModeHandler.setCompactMode(config.getCompactListView());
        refresh(null, 0, 0);
    }

    @Override
    public FImage getIcon() {
        return FSkinImage.LIST;
    }

    @Override
    public String getCaption() {
        return Localizer.getInstance().getMessage("lblListView");
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
    public float getScrollValue() {
        return list.getScrollTop();
    }

    @Override
    public void setScrollValue(float value) {
        list.setScrollTop(value);
    }

    @Override
    public void scrollSelectionIntoView() {
        list.scrollIntoView(getSelectedIndex());
    }

    @Override
    public Rectangle getSelectionBounds() {
        if (selectedIndices.isEmpty()) { return null; }

        return new Rectangle(list.screenPos.x, list.screenPos.y + list.getItemStartPosition(getSelectedIndex()), list.getWidth(), list.getListItemRenderer().getItemHeight());
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
    protected void layoutOptionsPanel(float width, float height) {
    }

    public final class ItemList extends FList<Entry<T, Integer>> {
        private final ItemManager<T>.ItemRenderer renderer;
        private final CompactModeHandler compactModeHandler;

        private ItemList() {
            compactModeHandler = new CompactModeHandler();
            renderer = itemManager.getListItemRenderer(compactModeHandler);
            setListItemRenderer(new ListItemRenderer<Map.Entry<T,Integer>>() {
                private int prevTapIndex = -1;

                @Override
                public float getItemHeight() {
                    return renderer.getItemHeight();
                }

                @Override
                public boolean tap(Integer index, Entry<T, Integer> value, float x, float y, int count) {
                    if (maxSelections > 1) { //if multi-select
                        //don't toggle checkbox if renderer handles tap
                        if (!renderer.tap(index, value, x, y, count)) {
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
                    }
                    else { //if single-select
                        setSelectedIndex(index);

                        //don't activate if renderer handles tap
                        if (!renderer.tap(index, value, x, y, count)) {
                            if (count == 1) {
                                itemManager.showMenu(false);
                            }
                            else if (count == 2 && index == prevTapIndex) {
                                itemManager.activateSelectedItems();
                            }
                        }
                    }

                    prevTapIndex = index;
                    return true;
                }

                @Override
                public boolean showMenu(Integer index, Entry<T, Integer> value, FDisplayObject owner, float x, float y) {
                    return renderer.longPress(index, value, x, y);
                }

                @Override
                public void drawValue(Graphics g, Integer index, Entry<T, Integer> value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                    if (maxSelections > 1) {
                        if (pressed) { //if multi-select mode, draw SEL_COLOR when pressed
                            g.fillRect(SEL_COLOR, x - FList.PADDING, y - FList.PADDING, w + 2 * FList.PADDING, h + 2 * FList.PADDING);
                        }
                        //draw checkbox, with it checked based on whether item is selected
                        float checkBoxSize = h * 0.4f;
                        float padding = checkBoxSize / 2;
                        w -= checkBoxSize + padding;
                        FCheckBox.drawCheckBox(g, selectedIndices.contains(index), x + w, y + (h - checkBoxSize) / 2, checkBoxSize, checkBoxSize);
                        w -= padding;
                    }
                    renderer.drawValue(g, value, font, foreColor, backColor, pressed, x + 1, y, w - 2, h); //x + 1 and w - 2 to account for left and right borders
                }
            });
            setFont(FSkinFont.get(14));
        }

        @Override
        protected void drawBackground(Graphics g) {
            //draw no background by default
        }

        @Override
        public boolean press(float x, float y) {
            if (renderer.allowPressEffect(this, x, y)) {
                return super.press(x, y);
            }
            return true;
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

        @Override
        public boolean zoom(float x, float y, float amount) {
            if (compactModeHandler.update(amount)) {
                revalidate(); //update scroll bounds
                scrollSelectionIntoView(); //ensure selection remains in view

                //update compact mode configuration
                itemManager.getConfig().setCompactListView(compactModeHandler.isCompactMode());
            }
            return true;
        }
    }

    public final class ItemListModel {
        private final ItemManagerModel<T> model;

        public ItemListModel(final ItemManagerModel<T> model0) {
            model = model0;
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
