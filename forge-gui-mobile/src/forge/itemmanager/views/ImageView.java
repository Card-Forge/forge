package forge.itemmanager.views;

import forge.Forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinImage;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.ImageCache;
import forge.deck.DeckProxy;
import forge.item.InventoryItem;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.GroupDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerModel;
import forge.itemmanager.filters.ItemFilter;
import forge.toolbox.FCardPanel;
import forge.toolbox.FComboBox;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.util.Utils;

import java.util.*;
import java.util.Map.Entry;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class ImageView<T extends InventoryItem> extends ItemView<T> {
    private static final float PADDING = Utils.scaleMin(5);
    private static final float PILE_SPACING_Y = 0.1f;
    private static final FSkinColor GROUP_HEADER_FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor OPTION_LABEL_COLOR = GROUP_HEADER_FORE_COLOR.alphaColor(0.7f);
    private static final FSkinColor GROUP_HEADER_LINE_COLOR = GROUP_HEADER_FORE_COLOR.alphaColor(0.5f);
    private static final FSkinFont GROUP_HEADER_FONT = FSkinFont.get(12);
    private static final float GROUP_HEADER_HEIGHT = Utils.scaleY(19);
    private static final float GROUP_HEADER_GLYPH_WIDTH = Utils.scaleX(6);
    private static final int MIN_COLUMN_COUNT = 1;
    private static final int MAX_COLUMN_COUNT = 10;

    private static final GroupDef[] CARD_GROUPBY_OPTIONS = { GroupDef.CREATURE_SPELL_LAND, GroupDef.CARD_TYPE, GroupDef.COLOR, GroupDef.COLOR_IDENTITY, GroupDef.CARD_RARITY };
    private static final GroupDef[] DECK_GROUPBY_OPTIONS = { GroupDef.COLOR, GroupDef.COLOR_IDENTITY };
    private static final ColumnDef[] CARD_PILEBY_OPTIONS = { ColumnDef.CMC, ColumnDef.COLOR, ColumnDef.NAME, ColumnDef.COST, ColumnDef.TYPE, ColumnDef.RARITY, ColumnDef.SET };
    private static final ColumnDef[] DECK_PILEBY_OPTIONS = { ColumnDef.DECK_COLOR, ColumnDef.DECK_FOLDER, ColumnDef.NAME, ColumnDef.DECK_FORMAT, ColumnDef.DECK_EDITION };

    private final CardViewDisplay display;
    private final List<Integer> selectedIndices = new ArrayList<Integer>();
    private int columnCount = 4;
    private boolean allowMultipleSelections;
    private ColumnDef pileBy = null;
    private GroupDef groupBy = null;
    private boolean lockHoveredItem = false;
    private boolean lockInput = false;
    private Vector2 hoverPoint;
    private Vector2 hoverScrollPos;
    private ItemInfo hoveredItem;
    private ItemInfo focalItem;
    private final ArrayList<ItemInfo> orderedItems = new ArrayList<ItemInfo>();
    private final ArrayList<Group> groups = new ArrayList<Group>();

    private class ExpandCollapseButton extends FLabel {
        private boolean isAllCollapsed;

        private ExpandCollapseButton() {
            super(new FLabel.ButtonBuilder());
            setCommand(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    if (groupBy == null || model.getItems().isEmpty()) { return; }

                    boolean collapsed = !isAllCollapsed;
                    for (Group group : groups) {
                        group.isCollapsed = collapsed;
                    }

                    updateIsAllCollapsed();
                    clearSelection(); //must clear selection since indices and visible items will be changing
                    updateLayout(false);
                }
            });
        }

        private void updateIsAllCollapsed() {
            boolean isAllCollapsed0 = true;
            for (Group group : groups) {
                if (!group.isCollapsed) {
                    isAllCollapsed0 = false;
                    break;
                }
            }
            isAllCollapsed = isAllCollapsed0;
        }

        @Override
        protected void drawContent(Graphics g, float w, float h, final boolean pressed) {
            float squareSize = w / 2 - 2;
            float offset = 2;
            float x = (w - squareSize) / 2 - offset;
            float y = (h - squareSize) / 2 - offset;
            if (!pressed) {
                x--;
                y--;
            }

            for (int i = 0; i < 2; i++) {
                g.drawLine(1, GROUP_HEADER_FORE_COLOR, x, y, x + squareSize, y);
                g.drawLine(1, GROUP_HEADER_FORE_COLOR, x + squareSize, y, x + squareSize, y + offset);
                g.drawLine(1, GROUP_HEADER_FORE_COLOR, x, y, x, y + squareSize);
                g.drawLine(1, GROUP_HEADER_FORE_COLOR, x, y + squareSize, x + offset, y + squareSize);
                x += offset;
                y += offset;
            }
            g.drawRect(1, GROUP_HEADER_FORE_COLOR, x, y, squareSize, squareSize);
            g.drawLine(1, GROUP_HEADER_FORE_COLOR, x + offset + 1, y + squareSize / 2, x + squareSize - 2 * offset + 1, y + squareSize / 2);
            if (isAllCollapsed) {
                g.drawLine(1, GROUP_HEADER_FORE_COLOR, x + squareSize / 2, y + offset + 1, x + squareSize / 2, y + squareSize - 2 * offset + 1);
            }
        }
    }
    private final ExpandCollapseButton btnExpandCollapseAll = new ExpandCollapseButton();
    private final FLabel lblGroupBy = new FLabel.Builder().text("Group:").fontSize(12).textColor(OPTION_LABEL_COLOR).build();
    private final FComboBox<Object> cbGroupByOptions = new FComboBox<Object>();
    private final FLabel lblPileBy = new FLabel.Builder().text("Pile:").fontSize(12).textColor(OPTION_LABEL_COLOR).build();
    private final FComboBox<Object> cbPileByOptions = new FComboBox<Object>();

    public ImageView(ItemManager<T> itemManager0, ItemManagerModel<T> model0) {
        super(itemManager0, model0);
        //setup options
        boolean isDeckManager = itemManager0.getGenericType().equals(DeckProxy.class);
        GroupDef[] groupByOptions = isDeckManager ? DECK_GROUPBY_OPTIONS : CARD_GROUPBY_OPTIONS;
        ColumnDef[] pileByOptions = isDeckManager ? DECK_PILEBY_OPTIONS : CARD_PILEBY_OPTIONS;
        cbGroupByOptions.addItem("(none)");
        cbPileByOptions.addItem("(none)");
        for (GroupDef option : groupByOptions) {
            cbGroupByOptions.addItem(option);
        }
        for (ColumnDef option : pileByOptions) {
            cbPileByOptions.addItem(option);
        }
        cbGroupByOptions.setSelectedIndex(0);
        cbPileByOptions.setSelectedIndex(0);

        cbGroupByOptions.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (cbGroupByOptions.getSelectedIndex() > 0) {
                    setGroupBy((GroupDef) cbGroupByOptions.getSelectedItem());
                }
                else {
                    setGroupBy(null);
                }
            }
        });
        cbPileByOptions.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (cbPileByOptions.getSelectedIndex() > 0) {
                    setPileBy((ColumnDef) cbPileByOptions.getSelectedItem());
                }
                else {
                    setPileBy(null);
                }
            }
        });

        cbGroupByOptions.setFontSize(12);
        cbPileByOptions.setFontSize(12);
        getPnlOptions().add(btnExpandCollapseAll);
        getPnlOptions().add(lblGroupBy);
        getPnlOptions().add(cbGroupByOptions);
        getPnlOptions().add(lblPileBy);
        getPnlOptions().add(cbPileByOptions);

        //setup display
        display = getScroller().add(new CardViewDisplay());
        /*display.addMouseListener(new FMouseAdapter() {
            @Override
            public void onLeftMouseDown(MouseEvent e) {
                if (lockInput) { return; }

                if (!selectItem(e)) {
                    //if didn't click on item, see if clicked on group header
                    if (groupBy != null) {
                        Point point = e.getPoint();
                        for (Group group : groups) {
                            if (group.getBounds().contains(point)) {
                                if (!group.items.isEmpty() && point.y < group.getTop() + GROUP_HEADER_HEIGHT) {
                                    group.isCollapsed = !group.isCollapsed;
                                    btnExpandCollapseAll.updateIsAllCollapsed();
                                    clearSelection(); //must clear selection since indices and visible items will be changing
                                    updateLayout(false);
                                }
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onLeftDoubleClick(MouseEvent e) {
                if (lockInput) { return; }

                ItemInfo item = getItemAtPoint(e.getPoint());
                if (item != null && item.selected) {
                    itemManager.activateSelectedItems();
                }
            }

            @Override
            public void onMiddleMouseDown(MouseEvent e) {
                if (lockInput) { return; }

                ItemInfo item = getItemAtPoint(e.getPoint());
                if (item != null && item.item instanceof IPaperCard) {
                    setLockHoveredItem(true); //lock hoveredItem while zoomer open
                    Card card = Card.getCardForUi((IPaperCard) item.item);
                    CardZoomer.SINGLETON_INSTANCE.doMouseButtonZoom(card);
                }
            }

            @Override
            public void onMiddleMouseUp(MouseEvent e) {
                if (lockInput) { return; }

                CardZoomer.SINGLETON_INSTANCE.closeZoomer();
                setLockHoveredItem(false);
            }

            @Override
            public void onRightClick(MouseEvent e) {
                if (lockInput) { return; }

                if (selectItem(e)) {
                    setLockHoveredItem(true); //lock hoveredItem while context menu open
                    itemManager.showContextMenu(e, new Runnable() {
                        @Override
                        public void run() {
                            setLockHoveredItem(false);
                        }
                    });
                }
            }

            private boolean selectItem(MouseEvent e) {
                focus();

                ItemInfo item = getItemAtPoint(e.getPoint());
                if (item == null) {
                    if (!e.isControlDown() && !e.isShiftDown()) {
                        clearSelection();
                        onSelectionChange();
                    }
                    return false;
                }

                if (item.selected) {
                    //toggle selection off item if Control down and left mouse down, otherwise do nothing
                    if (e.getButton() != 1) {
                        return true;
                    }
                    if (e.isControlDown()) {
                        item.selected = false;
                        selectedIndices.remove((Object)item.index);
                        onSelectionChange();
                        item.scrollIntoView();
                        return true;
                    }
                }
                if (!allowMultipleSelections || (!e.isControlDown() && !e.isShiftDown())) {
                    clearSelection();
                }
                selectedIndices.add(0, item.index);
                item.selected = true;
                onSelectionChange();
                item.scrollIntoView();
                return true;
            }

            @Override
            public void onMouseExit(MouseEvent e) {
                if (updateHoveredItem(null, null)) {
                    display.repaintSelf();
                }
            }
        });
        display.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                FScrollPane scroller = getScroller();
                Point hoverScrollPos = new Point(scroller.getHorizontalScrollBar().getValue(), scroller.getVerticalScrollBar().getValue());
                if (updateHoveredItem(e.getPoint(), hoverScrollPos)) {
                    display.repaintSelf();
                }
            }
        });*/
        groups.add(new Group("")); //add default group
    }

    @Override
    public void setup(ItemManagerConfig config, Map<ColumnDef, ItemColumn> colOverrides) {
        setGroupBy(config.getGroupBy(), true);
        setPileBy(config.getPileBy(), true);
        setColumnCount(config.getImageColumnCount(), true);
    }

    public GroupDef getGroupBy() {
        return groupBy;
    }
    public void setGroupBy(GroupDef groupBy0) {
        setGroupBy(groupBy0, false);
    }
    private void setGroupBy(GroupDef groupBy0, boolean forSetup) {
        if (groupBy == groupBy0) { return; }
        groupBy = groupBy0;

        if (groupBy == null) {
            cbGroupByOptions.setSelectedIndex(0);
        }
        else {
            cbGroupByOptions.setSelectedItem(groupBy);
        }

        groups.clear();

        if (groupBy == null) {
            groups.add(new Group(""));
            btnExpandCollapseAll.updateIsAllCollapsed();
        }
        else {
            for (String groupName : groupBy.getGroups()) {
                groups.add(new Group(groupName));
            }

            //collapse all groups by default if all previous groups were collapsed
            if (btnExpandCollapseAll.isAllCollapsed) {
                for (Group group : groups) {
                    group.isCollapsed = true;
                }
            }
        }

        if (!forSetup) {
            if (itemManager.getConfig() != null) {
                itemManager.getConfig().setGroupBy(groupBy);
            }
            refresh(null, -1, 0);
        }
    }

    public ColumnDef getPileBy() {
        return pileBy;
    }
    public void setPileBy(ColumnDef pileBy0) {
        setPileBy(pileBy0, false);
    }
    private void setPileBy(ColumnDef pileBy0, boolean forSetup) {
        if (pileBy == pileBy0) { return; }
        pileBy = pileBy0;

        if (pileBy == null) {
            cbPileByOptions.setSelectedIndex(0);
        }
        else {
            cbPileByOptions.setSelectedItem(pileBy);
        }

        if (!forSetup) {
            if (itemManager.getConfig() != null) {
                itemManager.getConfig().setPileBy(pileBy);
            }
            refresh(null, -1, 0);
        }
    }

    @Override
    protected void fixSelection(final Iterable<T> itemsToSelect, final int backupIndexToSelect, final float scrollValueToRestore) {
        if (itemsToSelect == null) {
            clearSelection(); //just clear selection if no items to select
            setScrollValue(scrollValueToRestore); //ensure scroll value restored
        }
        else {
            if (!setSelectedItems(itemsToSelect)) {
                setSelectedIndex(backupIndexToSelect);
            }
        }
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount0) {
        setColumnCount(columnCount0, false);
    }
    private void setColumnCount(int columnCount0, boolean forSetup) {
        if (columnCount0 < MIN_COLUMN_COUNT) {
            columnCount0 = MIN_COLUMN_COUNT;
        }
        else if (columnCount0 > MAX_COLUMN_COUNT) {
            columnCount0 = MAX_COLUMN_COUNT;
        }
        if (columnCount == columnCount0) { return; }
        columnCount = columnCount0;

        if (!forSetup) {
            if (itemManager.getConfig() != null) {
                itemManager.getConfig().setImageColumnCount(columnCount);
            }

            //determine item to retain scroll position of following column count change
            ItemInfo focalItem0 = getFocalItem();
            if (focalItem0 == null) {
                updateLayout(false);
                return;
            }
    
            float offsetTop = focalItem0.getTop() - getScrollValue();
            updateLayout(false);
            setScrollValue(focalItem0.getTop() - offsetTop);
            focalItem = focalItem0; //cache focal item so consecutive column count changes use the same item
        }
    }

    private ItemInfo getFocalItem() {
        if (focalItem != null) { //use cached focalItem if one
            return focalItem;
        }

        if (hoveredItem != null) {
            return hoveredItem;
        }

        //if not item hovered, use first fully visible item as focal point
        final float visibleTop = getScrollValue();
        for (Group group : groups) {
            if (group.getBottom() < visibleTop) {
                continue;
            }
            for (Pile pile : group.piles) {
                if (group.getBottom() < visibleTop) {
                    continue;
                }
                for (ItemInfo item : pile.items) {
                    if (item.getTop() >= visibleTop) {
                        return item;
                    }
                }
            }
        }
        if (orderedItems.isEmpty()) {
            return null;
        }
        return orderedItems.get(0);
    }

    @Override
    protected void onResize() {
        updateLayout(false); //need to update layout to adjust wrapping of items
    }

    @Override
    protected void onRefresh() {
        Group otherItems = groupBy == null ? groups.get(0) : null;

        for (Group group : groups) {
            group.items.clear();
        }
        clearSelection();

        for (Entry<T, Integer> itemEntry : model.getOrderedList()) {
            T item = itemEntry.getKey();
            int qty = itemEntry.getValue();
            int groupIndex = groupBy == null ? -1 : groupBy.getItemGroupIndex(item);

            for (int i = 0; i < qty; i++) {
                if (groupIndex >= 0) {
                    groups.get(groupIndex).add(new ItemInfo(item));
                }
                else {
                    if (otherItems == null) {
                        //reuse existing Other group if possible
                        if (groups.size() > groupBy.getGroups().length) {
                            otherItems = groups.get(groups.size() - 1);
                        }
                        else {
                            otherItems = new Group("Other");
                            otherItems.isCollapsed = btnExpandCollapseAll.isAllCollapsed;
                            groups.add(otherItems);
                        }
                    }
                    otherItems.add(new ItemInfo(item));
                }
            }
        }

        if (otherItems == null && groups.size() > groupBy.getGroups().length) {
            groups.remove(groups.size() - 1); //remove Other group if empty
            btnExpandCollapseAll.updateIsAllCollapsed();
        }

        updateLayout(true);
    }

    @Override
    protected float layoutOptionsPanel(float visibleWidth, float height) {
        float padding = ItemFilter.PADDING;
        float x = 0;
        float y = padding;
        float h = height - 2 * y;
        btnExpandCollapseAll.setBounds(x, y, h, h);
        x += h + padding;
        lblGroupBy.setBounds(x, y, lblGroupBy.getAutoSizeBounds().width, h);
        x += lblGroupBy.getWidth();

        //determine width of combo boxes based on available width versus auto-size widths
        float lblPileByWidth = lblPileBy.getAutoSizeBounds().width;
        float availableComboBoxWidth = visibleWidth - x - lblPileByWidth - padding;
        float groupByWidth = availableComboBoxWidth * 0.66f;
        float pileByWidth = availableComboBoxWidth - groupByWidth;

        cbGroupByOptions.setBounds(x, y, groupByWidth, h);
        x += groupByWidth + padding;
        lblPileBy.setBounds(x, y, lblPileByWidth, h);
        x += lblPileByWidth;
        cbPileByOptions.setBounds(x, y, pileByWidth, h);
        x += pileByWidth + padding;

        return x;
    }

    private void updateLayout(boolean forRefresh) {
        lockInput = true; //lock input until next repaint finishes
        focalItem = null; //clear cached focalItem when layout changes

        float x, groupY, pileY, pileHeight, maxPileHeight;
        float y = PADDING;
        float groupX = PADDING;
        float itemAreaWidth = display.getWidth();
        float groupWidth = itemAreaWidth - 2 * groupX;
        float pileX = PADDING;
        float pileWidth = itemAreaWidth - 2 * pileX;

        float gap = (MAX_COLUMN_COUNT - columnCount) / 2 + 2; //more items per row == less gap between them
        float itemWidth = Math.round((pileWidth + gap) / columnCount - gap);
        float itemHeight = Math.round(itemWidth * FCardPanel.ASPECT_RATIO);
        float dx = itemWidth + gap;
        float dy = pileBy == null ? itemHeight + gap : Math.round(itemHeight * PILE_SPACING_Y);

        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i);

            if (forRefresh && pileBy != null) { //refresh piles if needed
                //use TreeMap to build pile set so iterating below sorts on key
                ColumnDef groupPileBy = groupBy == null ? pileBy : groupBy.getGroupPileBy(i, pileBy);
                TreeMap<Comparable<?>, Pile> piles = new TreeMap<Comparable<?>, Pile>();
                for (ItemInfo itemInfo : group.items) {
                    Comparable<?> key = groupPileBy.fnSort.apply(itemInfo);
                    if (!piles.containsKey(key)) {
                        piles.put(key, new Pile());
                    }
                    piles.get(key).items.add(itemInfo);
                }
                group.piles.clear();
                for (Pile pile : piles.values()) {
                    group.piles.add(pile);
                }
            }

            groupY = y;
            if (groupBy != null) {
                y += GROUP_HEADER_HEIGHT + PADDING; //leave room for group header
                if (group.isCollapsed || group.items.isEmpty()) {
                    group.setBounds(groupX, groupY, groupWidth, GROUP_HEADER_HEIGHT);
                    continue;
                }
            }
            else if (group.items.isEmpty()) {
                group.setBounds(groupX, groupY, groupWidth, 0);
                continue;
            }

            if (pileBy == null) {
                //if not piling by anything, wrap items using a pile for each row
                group.piles.clear();
                Pile pile = new Pile();
                x = pileX;

                for (ItemInfo itemInfo : group.items) {
                    if (pile.items.size() == columnCount) {
                        pile = new Pile();
                        x = pileX;
                        y += dy;
                    }

                    itemInfo.setBounds(x, y, itemWidth, itemHeight);

                    if (pile.items.size() == 0) {
                        pile.setBounds(pileX, y, pileWidth, itemHeight);
                        group.piles.add(pile);
                    }
                    pile.items.add(itemInfo);
                    x += dx;
                }
                y += itemHeight;
            }
            else {
                x = pileX;
                pileY = y;
                maxPileHeight = 0;
                for (int j = 0; j < group.piles.size(); j++) {
                    if (j > 0 && j % columnCount == 0) {
                        //start new row if needed
                        y = pileY + maxPileHeight + gap;
                        x = pileX;
                        pileY = y;
                        maxPileHeight = 0;
                    }
                    Pile pile = group.piles.get(j);
                    y = pileY;
                    for (ItemInfo itemInfo : pile.items) {
                        itemInfo.setBounds(x, y, itemWidth, itemHeight);
                        y += dy;
                    }
                    pileHeight = y + itemHeight - dy - pileY;
                    if (pileHeight > maxPileHeight) {
                        maxPileHeight = pileHeight;
                    }
                    pile.setBounds(x, pileY, itemWidth, pileHeight);
                    x += dx;
                }
                y = pileY + maxPileHeight; //update y for setting group height below
            }

            group.setBounds(groupX, groupY, groupWidth, y - groupY);
            y += PADDING;
        }

        if (forRefresh) { //refresh ordered items if needed
            int index = 0;
            orderedItems.clear();
            for (Group group : groups) {
                if (group.isCollapsed || group.items.isEmpty()) { continue; }

                for (Pile pile : group.piles) {
                    for (ItemInfo itemInfo : pile.items) {
                        itemInfo.index = index++;
                        orderedItems.add(itemInfo);
                    }
                }
            }
        }
    }

    private ItemInfo getItemAtPoint(float x, float y) {
        for (int i = groups.size() - 1; i >= 0; i--) {
            Group group = groups.get(i);
            if (!group.isCollapsed && group.contains(x, y)) {
                for (int j = group.piles.size() - 1; j >= 0; j--) {
                    Pile pile = group.piles.get(j);
                    if (pile.contains(x, y)) {
                        for (int k = pile.items.size() - 1; k >= 0; k--) {
                            ItemInfo item = pile.items.get(k);
                            if (item.contains(x, y)) {
                                return item;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void setLockHoveredItem(boolean lockHoveredItem0) {
        if (lockHoveredItem == lockHoveredItem0) { return; }
        lockHoveredItem = lockHoveredItem0;
        if (!lockHoveredItem) {
            updateHoveredItem(hoverPoint, hoverScrollPos);
        }
    }

    private boolean updateHoveredItem(Vector2 hoverPoint0, Vector2 hoverScrollPos0) {
        hoverPoint = hoverPoint0;
        if (hoverScrollPos != hoverScrollPos0) {
            hoverScrollPos = hoverScrollPos0;
            focalItem = null; //clear cached focalItem when scroll changes
        }

        if (lockHoveredItem) { return false; }

        ItemInfo item = null;
        FScrollPane scroller = getScroller();
        if (hoverPoint0 != null) {
            Vector2 displayPoint = new Vector2(hoverPoint0);
            //account for change in scroll positions since mouse last moved
            displayPoint.x += scroller.getScrollLeft() - hoverScrollPos0.x;
            displayPoint.y += scroller.getScrollTop() - hoverScrollPos0.y;
            item = getItemAtPoint(displayPoint.x, displayPoint.y);
        }

        if (hoveredItem == item) { return false; }
        hoveredItem = item;
        return true;
    }

    @Override
    public FDisplayObject getComponent() {
        return display;
    }

    @Override
    public void setAllowMultipleSelections(boolean allowMultipleSelections0) {
        allowMultipleSelections = allowMultipleSelections0;
    }

    @Override
    public T getItemAtIndex(int index) {
        if (index >= 0 && index < getCount()) {
            return orderedItems.get(index).item;
        }
        return null;
    }

    @Override
    public int getIndexOfItem(T item) {
        for (Group group : groups) {
            for (ItemInfo itemInfo : group.items) {
                if (itemInfo.item == item) {
                    //if group containing item is collapsed, expand it so the item can be selected and has a valid index
                    if (group.isCollapsed) {
                        group.isCollapsed = false;
                        btnExpandCollapseAll.updateIsAllCollapsed();
                        clearSelection(); //must clear selection since indices and visible items will be changing
                        updateLayout(false);
                    }
                    return itemInfo.index;
                }
            }
        }
        return -1;
    }

    @Override
    public int getSelectedIndex() {
        return selectedIndices.isEmpty() ? -1 : selectedIndices.get(0);
    }

    @Override
    public Iterable<Integer> getSelectedIndices() {
        return selectedIndices;
    }

    @Override
    public int getCount() {
        return orderedItems.size();
    }

    @Override
    public int getSelectionCount() {
        return selectedIndices.size();
    }

    @Override
    public int getIndexAtPoint(float x, float y) {
        ItemInfo item = getItemAtPoint(x, y);
        if (item != null) {
            return item.index;
        }
        return -1;
    }

    @Override
    protected FImage getIcon() {
        if (itemManager.getGenericType().equals(DeckProxy.class)) {
            return FSkinImage.PACK;
        }
        return FSkinImage.CARD_IMAGE;
    }

    @Override
    protected String getCaption() {
        return "Image View";
    }

    @Override
    public void selectAll() {
        clearSelection();
        for (Integer i = 0; i < getCount(); i++) {
            selectedIndices.add(i);
        }
        updateSelection();
        onSelectionChange();
    }

    @Override
    protected void onSetSelectedIndex(int index) {
        clearSelection();
        selectedIndices.add(index);
        updateSelection();
    }

    @Override
    protected void onSetSelectedIndices(Iterable<Integer> indices) {
        clearSelection();
        for (Integer index : indices) {
            selectedIndices.add(index);
        }
        updateSelection();
    }

    private void clearSelection() {
        int count = getCount();
        for (Integer i : selectedIndices) {
            if (i < count) {
                orderedItems.get(i).selected = false;
            }
        }
        selectedIndices.clear();
    }

    private void updateSelection() {
        for (Integer i : selectedIndices) {
            orderedItems.get(i).selected = true;
        }
    }

    @Override
    public void scrollSelectionIntoView() {
        if (selectedIndices.isEmpty()) { return; }

        ItemInfo itemInfo = orderedItems.get(selectedIndices.get(0));
        getScroller().scrollIntoView(itemInfo);
    }

    private class Group extends FDisplayObject {
        private final List<ItemInfo> items = new ArrayList<ItemInfo>();
        private final List<Pile> piles = new ArrayList<Pile>();
        private final String name;
        private boolean isCollapsed;

        public Group(String name0) {
            name = name0;
        }

        public void add(ItemInfo item) {
            items.add(item);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public void draw(Graphics g) {
        }
    }
    private class Pile extends FDisplayObject {
        private final List<ItemInfo> items = new ArrayList<ItemInfo>();

        @Override
        public void draw(Graphics g) {
        }
    }
    private class ItemInfo extends FDisplayObject implements Entry<InventoryItem, Integer> {
        private final T item;
        private int index;
        private boolean selected;

        private ItemInfo(T item0) {
            item = item0;
        }

        @Override
        public String toString() {
            return item.toString();
        }

        @Override
        public InventoryItem getKey() {
            return item;
        }

        @Override
        public Integer getValue() {
            return 1;
        }

        @Override
        public Integer setValue(Integer value) {
            return 1;
        }

        @Override
        public void draw(Graphics g) {
        }
    }

    private class CardViewDisplay extends FDisplayObject {
        private CardViewDisplay() {
        }

        @Override
        public final void draw(final Graphics g) {
            if (groups.isEmpty() || groups.get(0).getWidth() <= 0) {
                return; //don't render anything until first group has width
            }

            updateHoveredItem(hoverPoint, hoverScrollPos); //ensure hovered item up to date

            final float visibleWidth = getWidth();
            final float visibleHeight = getHeight();
            final float visibleTop = getScrollValue();
            final float visibleBottom = visibleTop + visibleHeight;

            for (Group group : groups) {
                if (group.getBottom() < visibleTop) {
                    continue;
                }
                if (group.getTop() >= visibleBottom) {
                    break;
                }
                if (groupBy != null) {
                    /*Rectangle bounds = group.getBounds();

                    //draw header background and border if hovered
                    //TODO: Uncomment
                    //FSkin.setGraphicsColor(g2d, ItemListView.HEADER_BACK_COLOR);
                    //g2d.fillRect(bounds.x, bounds.y, bounds.width, GROUP_HEADER_HEIGHT - 1);
                    //FSkin.setGraphicsColor(g2d, ItemListView.GRID_COLOR);
                    //g2d.drawRect(bounds.x, bounds.y, bounds.width - 1, GROUP_HEADER_HEIGHT - 1);

                    //draw group name and horizontal line
                    float x = bounds.x + GROUP_HEADER_GLYPH_WIDTH + PADDING + 1;
                    float y = bounds.y + fontOffsetY;
                    FSkin.setGraphicsColor(g2d, GROUP_HEADER_FORE_COLOR);
                    String caption = group.name + " (" + group.items.size() + ")";
                    g2d.drawString(caption, x, y);
                    x += fm.stringWidth(caption) + PADDING;
                    y = bounds.y + GROUP_HEADER_HEIGHT / 2;
                    FSkin.setGraphicsColor(g2d, GROUP_HEADER_LINE_COLOR);
                    g2d.drawLine(x, y, bounds.x + bounds.width - 1, y);

                    if (!group.items.isEmpty()) { //draw expand/collapse glyph as long as group isn't empty
                        Polygon glyph = new Polygon();
                        float offset = GROUP_HEADER_GLYPH_WIDTH / 2 + 1;
                        x = bounds.x + offset;
                        if (group.isCollapsed) {
                            y++;
                            glyph.addPoint(x, y - offset);
                            glyph.addPoint(x + offset, y);
                            glyph.addPoint(x, y + offset);
                        }
                        else {
                            glyph.addPoint(x - offset + 2, y + offset - 1);
                            glyph.addPoint(x + offset, y + offset - 1);
                            glyph.addPoint(x + offset, y - offset + 1);
                        }
                        g2d.fill(glyph);
                    }*/
                    if (group.isCollapsed || group.items.isEmpty()) {
                        continue;
                    }
                }
                else if (group.items.isEmpty()) {
                    continue;
                }

                ItemInfo skippedItem = null;
                for (Pile pile : group.piles) {
                    if (pile.getBottom() < visibleTop) {
                        continue;
                    }
                    if (pile.getTop() >= visibleBottom) {
                        break;
                    }
                    for (ItemInfo itemInfo : pile.items) {
                        if (itemInfo.getBottom() < visibleTop) {
                            continue;
                        }
                        if (itemInfo.getTop() >= visibleBottom) {
                            break;
                        }
                        if (itemInfo != hoveredItem) { //save hovered item for last
                            drawItemImage(g, itemInfo);
                        }
                        else {
                            skippedItem = itemInfo;
                        }
                    }
                }
                if (skippedItem != null) { //draw hovered item on top
                    drawItemImage(g, skippedItem);
                }
            }

            if (lockInput) { //unlock input after repaint finishes if needed
                /*SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        lockInput = false;
                    }
                });*/
                lockInput = false;
            }
        }

        private void drawItemImage(Graphics g, ItemInfo itemInfo) {
            final float selBorderSize = 1;

            if (itemInfo.selected || itemInfo == hoveredItem) {
                g.fillRect(Color.GREEN, itemInfo.getLeft() - selBorderSize, itemInfo.getTop() - selBorderSize,
                        itemInfo.getWidth() + 2 * selBorderSize, itemInfo.getHeight() + 2 * selBorderSize);
            }

            InventoryItem item = itemInfo.item;
            Texture img = ImageCache.getImage(itemInfo.item);
            if (img != null) {
                g.drawImage(img, itemInfo.getLeft(), itemInfo.getTop(), itemInfo.getWidth(), itemInfo.getHeight());
            }
            else {
                //g.drawString(itemInfo.item.getName(), itemInfo.getLeft() + 10, itemInfo.getTop() + 20);
            }

            //draw foil effect if needed
            /*if (item instanceof IPaperCard) {
                IPaperCard paperCard = (IPaperCard)item;
                if (paperCard.isFoil()) {
                    Card card = Card.getCardForUi(paperCard);
                    if (card.getFoil() == 0) { //if foil finish not yet established, assign a random one
                        card.setRandomFoil();
                    }
                    CardPanel.drawFoilEffect(g, card, itemInfo.getLeft(), itemInfo.getTop(), itemInfo.getWidth(), itemInfo.getHeight(), borderSize);
                }
            }*/
        }
    }
}
