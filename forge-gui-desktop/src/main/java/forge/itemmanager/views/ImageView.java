package forge.itemmanager.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import forge.ImageCache;
import forge.card.ColorSet;
import forge.deck.DeckProxy;
import forge.deck.io.DeckPreferences;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.gui.framework.ILocalRepaint;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.GroupDef;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerModel;
import forge.itemmanager.SItemManagerUtil;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.CardFaceSymbols;
import forge.toolbox.FComboBoxWrapper;
import forge.toolbox.FLabel;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinFont;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FTextField;
import forge.toolbox.special.CardZoomer;
import forge.util.ImageUtil;
import forge.util.Localizer;
import forge.view.arcane.CardPanel;

public class ImageView<T extends InventoryItem> extends ItemView<T> {
    private static final int PADDING = 5;
    private static final float PILE_SPACING_Y = 0.1f;
    private static final SkinColor GROUP_HEADER_FORE_COLOR = FSkin.getColor(FSkin.Colors.CLR_TEXT);
    private static final SkinColor GROUP_HEADER_LINE_COLOR = GROUP_HEADER_FORE_COLOR.alphaColor(120);
    private static final SkinFont GROUP_HEADER_FONT = FSkin.getFont();
    private static final int GROUP_HEADER_HEIGHT = 19;
    private static final int GROUP_HEADER_GLYPH_WIDTH = 6;
    private static final int MIN_COLUMN_COUNT = 1;
    private static final int MAX_COLUMN_COUNT = 10;

    private final CardViewDisplay display;
    private final List<Integer> selectedIndices = new ArrayList<>();
    private int columnCount = 4;
    private boolean allowMultipleSelections;
    private ColumnDef pileBy = null;
    private GroupDef groupBy = null;
    private boolean lockHoveredItem = false;
    private boolean lockInput = false;
    private Point hoverPoint;
    private Point hoverScrollPos;
    private ItemInfo hoveredItem;
    private ItemInfo focalItem;
    private boolean panelOptionsCreated = false;
    // cards with alternate states are added twice for displaying
    private InventoryItem lastAltCard = null;

    private final List<ItemInfo> orderedItems = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();
    final Localizer localizer = Localizer.getInstance();

    private static boolean isPreferenceEnabled(final ForgePreferences.FPref preferenceName) {
        return FModel.getPreferences().getPrefBoolean(preferenceName);
    }

    @SuppressWarnings("serial")
    private class ExpandCollapseButton extends FLabel {
        private boolean isAllCollapsed;

        private ExpandCollapseButton() {
            super(new FLabel.ButtonBuilder());
            setFocusable(false);
            updateToolTip();
            setCommand(new Runnable() {
                @Override
                public void run() {
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
            if (isAllCollapsed != isAllCollapsed0) {
                isAllCollapsed = isAllCollapsed0;
                updateToolTip();
                repaintSelf();
            }
        }
        private void updateToolTip() {
            setToolTipText(isAllCollapsed ? localizer.getMessage("lblExpandallgroups") : localizer.getMessage("lblCollapseallgroups"));
        }

        @Override
        protected void paintContent(final Graphics2D g, int w, int h, final boolean paintPressedState) {
            int squareSize = w / 2 - 2;
            int offset = 2;
            int x = (w - squareSize) / 2 - offset;
            int y = (h - squareSize) / 2 - offset;
            if (!paintPressedState) {
                x--;
                y--;
            }

            FSkin.setGraphicsColor(g, GROUP_HEADER_FORE_COLOR);
            for (int i = 0; i < 2; i++) {
                g.drawLine(x, y, x + squareSize, y);
                g.drawLine(x + squareSize, y, x + squareSize, y + offset);
                g.drawLine(x, y, x, y + squareSize);
                g.drawLine(x, y + squareSize, x + offset, y + squareSize);
                x += offset;
                y += offset;
            }
            g.drawRect(x, y, squareSize, squareSize);
            g.drawLine(x + offset + 1, y + squareSize / 2, x + squareSize - 2 * offset + 1, y + squareSize / 2);
            if (isAllCollapsed) {
                g.drawLine(x + squareSize / 2, y + offset + 1, x + squareSize / 2, y + squareSize - 2 * offset + 1);
            }
        }
    }
    private final ExpandCollapseButton btnExpandCollapseAll = new ExpandCollapseButton();

    private final FComboBoxWrapper<Object> cbGroupByOptions = new FComboBoxWrapper<>();
    private final FComboBoxWrapper<Object> cbPileByOptions = new FComboBoxWrapper<>();
    private final FComboBoxWrapper<Integer> cbColumnCount = new FComboBoxWrapper<>();

    public ImageView(final ItemManager<T> itemManager0, final ItemManagerModel<T> model0) {
        super(itemManager0, model0);

        SItemManagerUtil.populateImageViewOptions(itemManager0, cbGroupByOptions, cbPileByOptions);

        for (Integer i = MIN_COLUMN_COUNT; i <= MAX_COLUMN_COUNT; i++) {
            cbColumnCount.addItem(i);
        }
        cbGroupByOptions.setMaximumRowCount(cbGroupByOptions.getItemCount());
        cbPileByOptions.setMaximumRowCount(cbPileByOptions.getItemCount());
        cbColumnCount.setMaximumRowCount(cbColumnCount.getItemCount());
        cbColumnCount.setSelectedIndex(columnCount - MIN_COLUMN_COUNT);

        cbGroupByOptions.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                focus();
                if (cbGroupByOptions.getSelectedIndex() > 0) {
                    setGroupBy((GroupDef) cbGroupByOptions.getSelectedItem());
                }
                else {
                    setGroupBy(null);
                }
            }
        });
        cbPileByOptions.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                focus();
                if (cbPileByOptions.getSelectedIndex() > 0) {
                    setPileBy((ColumnDef) cbPileByOptions.getSelectedItem());
                }
                else {
                    setPileBy(null);
                }
            }
        });
        cbColumnCount.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                focus();
                setColumnCount(cbColumnCount.getSelectedItem());
            }
        });

        //setup display
        display = new CardViewDisplay();
        display.addMouseListener(new FMouseAdapter() {
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
                                    updateLayout(true);
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
            public void onRightDoubleClick(MouseEvent e) {
                if (lockInput) { return; }

                ItemInfo item = getItemAtPoint(e.getPoint());
                if (item != null && item.selected) {
                    if (item.item instanceof DeckProxy) {
                        DeckProxy dp = (DeckProxy) item.item;
                        if (!dp.isGeneratedDeck()) {
                            if (DeckPreferences.getPrefs(dp).getStarCount() > 0)
                                DeckPreferences.getPrefs(dp).setStarCount(0);
                            else
                                DeckPreferences.getPrefs(dp).setStarCount(1);

                            updateLayout(false);
                        }
                    }
                }
            }

            @Override
            public void onMiddleMouseDown(MouseEvent e) {
                if (lockInput) { return; }

                ItemInfo item = getItemAtPoint(e.getPoint());
                if (item != null && item.item instanceof IPaperCard) {
                    setLockHoveredItem(true); //lock hoveredItem while zoomer open
                    final CardView card = CardView.getCardForUi((IPaperCard) item.item);
                    CardZoomer.SINGLETON_INSTANCE.setCard(card.getCurrentState(), true);
                    CardZoomer.SINGLETON_INSTANCE.doMouseButtonZoom();
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
        });
        groups.add(new Group("")); //add default group
        getScroller().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    @Override
    public void setup(ItemManagerConfig config, Map<ColumnDef, ItemTableColumn> colOverrides) {
        // if this is the first setup call, panel options will be added to UI components
        if (!this.panelOptionsCreated){
            setPanelOptions(config.getShowUniqueCardsOption());
            this.panelOptionsCreated = true;
        }
        // set status of components in the panel
        setGroupBy(config.getGroupBy(), true);
        setPileBy(config.getPileBy(), true);
        setColumnCount(config.getImageColumnCount(), true);
    }

    private void setPanelOptions(boolean showUniqueCardsOption) {
        // Collapse all groups first
        getPnlOptions().add(btnExpandCollapseAll, "w " + FTextField.HEIGHT + "px, h " + FTextField.HEIGHT + "px");
        // Show Unique Cards Only Option
        if (showUniqueCardsOption) {
            setUniqueCardsOnlyFilter();
        }
        // GroupBy, Pile by, Columns
        getPnlOptions().add(new FLabel.Builder().text(localizer.getMessage("lblGroupby") +":").fontSize(12).build());
        cbGroupByOptions.addTo(getPnlOptions(), "pushx, growx");
        getPnlOptions().add(new FLabel.Builder().text(localizer.getMessage("lblPileby") +":").fontSize(12).build());
        cbPileByOptions.addTo(getPnlOptions(), "pushx, growx");
        getPnlOptions().add(new FLabel.Builder().text(localizer.getMessage("lblColumns") +":").fontSize(12).build());
        cbColumnCount.addTo(getPnlOptions(), "w 38px!");
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
    protected void fixSelection(final Iterable<T> itemsToSelect, final int backupIndexToSelect, final int scrollValueToRestore) {
        if (itemsToSelect == null) {
            clearSelection(); //just clear selection if no items to select
            setScrollValue(scrollValueToRestore); //ensure scroll value restored
            onSelectionChange();
        }
        else {
            if (!setSelectedItems(itemsToSelect)) {
                setSelectedIndex(backupIndexToSelect);
            }
        }
    }

    @Override
    protected void onMouseWheelZoom(MouseWheelEvent e) {
        if (e.getWheelRotation() > 0) {
            setColumnCount(columnCount + 1);
        }
        else {
            setColumnCount(columnCount - 1);
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
        cbColumnCount.setSelectedIndex(columnCount - MIN_COLUMN_COUNT);

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

            int offsetTop = focalItem0.getTop() - getScrollValue();
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
        final int visibleTop = getScrollValue();
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

    private void updateLayout(boolean forRefresh) {
        lockInput = true; //lock input until next repaint finishes
        focalItem = null; //clear cached focalItem when layout changes

        int x, groupY, pileY, pileHeight, maxPileHeight;
        int y = PADDING;
        int groupX = PADDING;
        int itemAreaWidth = getVisibleSize().width;
        int groupWidth = itemAreaWidth - 2 * groupX;
        int pileX = PADDING;
        int pileWidth = itemAreaWidth - 2 * pileX;

        int gap = (MAX_COLUMN_COUNT - columnCount) / 2 + 2; //more items per row == less gap between them
        int itemWidth = Math.round((pileWidth + gap) / columnCount - gap);
        int itemHeight = Math.round(itemWidth * CardPanel.ASPECT_RATIO);
        int dx = itemWidth + gap;
        int dy = pileBy == null ? itemHeight + gap : Math.round(itemHeight * PILE_SPACING_Y);

        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i);

            if (forRefresh && pileBy != null) { //refresh piles if needed
                //use TreeMap to build pile set so iterating below sorts on key
                ColumnDef groupPileBy = groupBy == null ? pileBy : groupBy.getGroupPileBy(i, pileBy);
                Map<Comparable<?>, Pile> piles = new TreeMap<>();
                for (ItemInfo itemInfo : group.items) {
                    Comparable<?> key = groupPileBy.fnSort.apply(itemInfo);
                    if (!piles.containsKey(key)) {
                        piles.put(key, new Pile());
                    }
                    piles.get(key).items.add(itemInfo);
                }
                group.piles.clear();
                group.piles.addAll(piles.values());
            }

            groupY = y;

            if (group.items.isEmpty()) {
                group.setBounds(groupX, groupY, groupWidth, 0);
                continue;
            }

            if (groupBy != null) {
                y += GROUP_HEADER_HEIGHT + PADDING; //leave room for group header
                if (group.isCollapsed) {
                    group.setBounds(groupX, groupY, groupWidth, GROUP_HEADER_HEIGHT);
                    continue;
                }
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

        display.setPreferredSize(new Dimension(itemAreaWidth, y));
        display.revalidate();
        display.repaintSelf();
    }

    private ItemInfo getItemAtPoint(Point p) {
        for (int i = groups.size() - 1; i >= 0; i--) {
            Group group = groups.get(i);
            if (!group.isCollapsed && group.getBounds().contains(p)) {
                for (int j = group.piles.size() - 1; j >= 0; j--) {
                    Pile pile = group.piles.get(j);
                    if (pile.getBounds().contains(p)) {
                        for (int k = pile.items.size() - 1; k >= 0; k--) {
                            ItemInfo item = pile.items.get(k);
                            if (item.getBounds().contains(p)) {
                                return item;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private Dimension getVisibleSize() {
        FScrollPane scroller = getScroller();
        Dimension size = getScroller().getSize();
        Insets insets = getScroller().getInsets();
        size =  new Dimension(size.width - insets.left - insets.right,
                size.height - insets.top - insets.bottom);
        size.width -= scroller.getVerticalScrollBar().getPreferredSize().width;
        return size;
    }

    private void setLockHoveredItem(boolean lockHoveredItem0) {
        if (lockHoveredItem == lockHoveredItem0) { return; }
        lockHoveredItem = lockHoveredItem0;
        if (!lockHoveredItem && updateHoveredItem(hoverPoint, hoverScrollPos)) {
            display.repaintSelf(); //redraw hover effect immediately if needed
        }
    }

    private boolean updateHoveredItem(Point hoverPoint0, Point hoverScrollPos0) {
        hoverPoint = hoverPoint0;
        if (hoverScrollPos != hoverScrollPos0) {
            hoverScrollPos = hoverScrollPos0;
            focalItem = null; //clear cached focalItem when scroll changes
        }

        if (lockHoveredItem) { return false; }

        ItemInfo item = null;
        FScrollPane scroller = getScroller();
        if (hoverPoint0 != null) {
            Point displayPoint = new Point(hoverPoint0);
            //account for change in scroll positions since mouse last moved
            displayPoint.x += scroller.getHorizontalScrollBar().getValue() - hoverScrollPos0.x;
            displayPoint.y += scroller.getVerticalScrollBar().getValue() - hoverScrollPos0.y;
            item = getItemAtPoint(displayPoint);
        }

        if (hoveredItem == item) { return false; }
        hoveredItem = item;
        if (item != null) {
            final CDetailPicture cDetailPicture = itemManager.getCDetailPicture();
            if (cDetailPicture != null) {
                cDetailPicture.displayAlt(item.alt);
            }
            showHoveredItem(item.item);
        }
        return true;
    }

    protected void showHoveredItem(final T item) {
        final CDetailPicture cDetailPicture = itemManager.getCDetailPicture();
        if (cDetailPicture != null) {
            cDetailPicture.showItem(item);
        }
        else {
            // if opened from lobby ItemManager has no own
            CDeckEditorUI.SINGLETON_INSTANCE.setCard(item);
        }
    }

    @Override
    public JComponent getComponent() {
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
    public int getIndexAtPoint(Point p) {
        ItemInfo item = getItemAtPoint(p);
        if (item != null) {
            return item.index;
        }
        return -1;
    }

    @Override
    protected SkinImage getIcon() {
        if (itemManager.getGenericType().equals(DeckProxy.class)) {
            return FSkin.getImage(FSkinProp.IMG_PACK).resize(18, 18);
        }
        return FSkin.getIcon(FSkinProp.ICO_CARD_IMAGE);
    }

    @Override
    protected String getCaption() {
        return Localizer.getInstance().getMessage("lblImageView");
    }

    @Override
    public void selectAll() {
        clearSelection();
        for (Integer i = 0; i < getCount(); i++) {
            selectedIndices.add(i);
        }
        updateSelection();
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
        onSelectionChange();
    }

    @Override
    protected void onSelectionChange() {
        super.onSelectionChange();
        display.repaintSelf();
    }

    @Override
    protected void onScrollSelectionIntoView(JViewport viewport) {
        if (selectedIndices.isEmpty()) { return; }

        ItemInfo itemInfo = orderedItems.get(selectedIndices.get(0));
        itemInfo.scrollIntoView();
    }

    private class DisplayArea {
        private final Rectangle bounds = new Rectangle();

        public Rectangle getBounds() {
            return bounds;
        }
        public void setBounds(int x, int y, int width, int height) {
            bounds.x = x;
            bounds.y = y;
            bounds.width = width;
            bounds.height = height;
        }
        public int getTop() {
            return bounds.y;
        }
        public int getBottom() {
            return bounds.y + bounds.height;
        }
        public void scrollIntoView() {
            int x = bounds.x - PADDING;
            int y = bounds.y - PADDING;
            int width = bounds.width + 2 * PADDING;
            int height = bounds.height + 2 * PADDING;
            display.scrollRectToVisible(new Rectangle(x, y, width, height));
        }
    }
    private class Group extends DisplayArea {
        private final List<ItemInfo> items = new ArrayList<>();
        private final List<Pile> piles = new ArrayList<>();
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
    }
    private class Pile extends DisplayArea {
        private final List<ItemInfo> items = new ArrayList<>();
    }
    private class ItemInfo extends DisplayArea implements Entry<InventoryItem, Integer> {
        private final T item;
        private int index;
        private boolean selected;
        private boolean alt;

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
    }

    @SuppressWarnings("serial")
    private class CardViewDisplay extends JPanel implements ILocalRepaint {
        private CardViewDisplay() {
            setOpaque(false);
            setFocusable(true);
        }

        @Override
        public void repaintSelf() {
            repaint(getVisibleRect());
        }

        @Override
        public final void paintComponent(final Graphics g) {
            if (groups.isEmpty() || groups.get(0).getBounds().width <= 0) {
                return; //don't render anything until first group has width
            }

            updateHoveredItem(hoverPoint, hoverScrollPos); //ensure hovered item up to date

            final Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final Dimension visibleSize = getVisibleSize();
            final int visibleTop = getScrollValue();
            final int visibleBottom = visibleTop + visibleSize.height;

            FSkin.setGraphicsFont(g2d, GROUP_HEADER_FONT);
            FontMetrics fm = g2d.getFontMetrics();
            int fontOffsetY = (GROUP_HEADER_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();

            for (Group group : groups) {
                if (group.items.isEmpty()) {
                    continue;
                }
                if (group.getBottom() < visibleTop) {
                    continue;
                }
                if (group.getTop() >= visibleBottom) {
                    break;
                }
                if (groupBy != null) {
                    Rectangle bounds = group.getBounds();

                    //draw header background and border if hovered
                    //TODO: Uncomment
                    //FSkin.setGraphicsColor(g2d, ItemListView.HEADER_BACK_COLOR);
                    //g2d.fillRect(bounds.x, bounds.y, bounds.width, GROUP_HEADER_HEIGHT - 1);
                    //FSkin.setGraphicsColor(g2d, ItemListView.GRID_COLOR);
                    //g2d.drawRect(bounds.x, bounds.y, bounds.width - 1, GROUP_HEADER_HEIGHT - 1);

                    //draw group name and horizontal line
                    int x = bounds.x + GROUP_HEADER_GLYPH_WIDTH + PADDING + 1;
                    int y = bounds.y + fontOffsetY;
                    FSkin.setGraphicsColor(g2d, GROUP_HEADER_FORE_COLOR);
                    String caption = group.name + " (" + group.items.size() + ")";
                    g2d.drawString(caption, x, y);
                    x += fm.stringWidth(caption) + PADDING;
                    y = bounds.y + GROUP_HEADER_HEIGHT / 2;
                    FSkin.setGraphicsColor(g2d, GROUP_HEADER_LINE_COLOR);
                    g2d.drawLine(x, y, bounds.x + bounds.width - 1, y);

                    //draw expand/collapse glyph
                    Polygon glyph = new Polygon();
                    int offset = GROUP_HEADER_GLYPH_WIDTH / 2 + 1;
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

                    if (group.isCollapsed) { continue; }
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

                        InventoryItem item = itemInfo.item;
                        itemInfo.alt = false;
                        if (!FModel.getPreferences().getPref(FPref.UI_SWITCH_STATES_DECKVIEW).equals(ForgeConstants.SWITCH_CARDSTATES_DECK_NEVER)) {
                            if ((hoveredItem == null || !hoveredItem.item.equals(item)) || (FModel.getPreferences().getPref(FPref.UI_SWITCH_STATES_DECKVIEW).equals(ForgeConstants.SWITCH_CARDSTATES_DECK_ALWAYS))) {
                                if (item instanceof PaperCard) {
                                    if (ImageUtil.hasBackFacePicture(((PaperCard)item))) {
                                        if (item.equals(lastAltCard)) {
                                            itemInfo.alt = true;
                                            lastAltCard = null;
                                        }
                                        else {
                                            lastAltCard = item;
                                        }
                                    }
                                    else {
                                        lastAltCard = null;
                                    }
                                }
                            }
                        }

                        if (itemInfo != hoveredItem) { //save hovered item for last
                            drawItemImage(g2d, itemInfo);
                        }
                        else {
                            skippedItem = itemInfo;
                        }
                    }
                }
                if (skippedItem != null) { //draw hovered item on top
                    drawItemImage(g2d, skippedItem);
                }
            }

            if (lockInput) { //unlock input after repaint finishes if needed
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        lockInput = false;
                    }
                });
            }
        }

        private void drawItemImage(Graphics2D g, ItemInfo itemInfo) {
            Rectangle bounds = itemInfo.getBounds();
            final int itemWidth = bounds.width;
            final int selBorderSize = 1;
            InventoryItem item = itemInfo.item;
            boolean deckSelectMode = item instanceof DeckProxy;

            // Determine whether to render border from properties
            boolean noBorder = !isPreferenceEnabled(ForgePreferences.FPref.UI_RENDER_BLACK_BORDERS);
            if (item instanceof IPaperCard) {
                CardView cv = CardView.getCardForUi((IPaperCard) item);
                // Amonkhet Invocations
                noBorder |= cv.getCurrentState().getSetCode().equalsIgnoreCase("MPS_AKH");
                // Unstable basic lands
                noBorder |= cv.getCurrentState().isBasicLand() && cv.getCurrentState().getSetCode().equalsIgnoreCase("UST");
            }

            final int borderSize = noBorder? 0 : Math.round(itemWidth * CardPanel.BLACK_BORDER_SIZE);
            final int cornerSize = Math.max(4, Math.round(itemWidth * CardPanel.ROUNDED_CORNER_SIZE));

            if (itemInfo.selected || itemInfo == hoveredItem) {
                g.setColor(Color.green);
                g.fillRoundRect(bounds.x - selBorderSize, bounds.y - selBorderSize,
                        bounds.width + 2 * selBorderSize, bounds.height + 2 * selBorderSize,
                        cornerSize + selBorderSize, cornerSize + selBorderSize);
            }

            g.setColor(Color.black);
            g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, cornerSize, cornerSize);

            BufferedImage img = ImageCache.getImage(item, bounds.width - 2 * borderSize, bounds.height - 2 * borderSize, itemInfo.alt);

            if (img != null) {
                g.drawImage(img, null, bounds.x + borderSize, bounds.y + borderSize);
            }
            else {
                if (deckSelectMode) {
                    DeckProxy dp = ((DeckProxy) item);
                    if (dp.isGeneratedDeck()) {
                        //draw generic box
                        FSkin.drawImage(g, FSkin.getImage(FSkinProp.IMG_DECK_GENERIC), bounds.x, bounds.y, bounds.width - 2 * cornerSize, bounds.height - 2 * cornerSize);
                    } else {
                        String deckImageKey = dp.getDeck().getCommanders().isEmpty() ? dp.getHighestCMCCard().getImageKey(false) : dp.getDeck().getCommanders().get(0).getImageKey(false);

                        ColorSet deckColor = dp.getColor();
                        int scale = CardFaceSymbols.getHeight() * cornerSize/8;
                        int scaleArt = CardFaceSymbols.getHeight() * cornerSize/7;

                        BufferedImage cardImage = ImageCache.scaleImage(deckImageKey, bounds.width, bounds.height, false, null);

                        if (cardImage == null) {
                            //draw generic box
                            FSkin.drawImage(g, FSkin.getImage(FSkinProp.IMG_DECK_GENERIC), bounds.x, bounds.y, bounds.width - 2 * cornerSize, bounds.height - 2 * cornerSize);
                        } else {
                            //draw card art
                            g.drawImage(ImageCache.getCroppedArt(cardImage,bounds.x, bounds.y,bounds.width, bounds.height).getScaledInstance(scaleArt*3,  Math.round(scaleArt*2.5f), Image.SCALE_SMOOTH),
                                    bounds.x+bounds.width/9, 2*cornerSize+bounds.y+bounds.height/7, null);
                            //draw deck box
                            FSkin.drawImage(g, FSkin.getImage(FSkinProp.IMG_DECK_CARD_ART), bounds.x, bounds.y, bounds.width - 2 * cornerSize, bounds.height - 2 * cornerSize);
                        }

                        //deck colors
                        if (deckColor != null) {
                            CardFaceSymbols.drawColorSet(g, deckColor, bounds.x + bounds.width - (scale*2) + cornerSize, bounds.y + bounds.height/2 - (scale*2), scale, true);
                        }
                        //favorite
                        FSkin.drawImage(g, DeckPreferences.getPrefs(dp).getStarCount() > 0 ? FSkin.getImage(FSkinProp.IMG_STAR_FILLED) : FSkin.getImage(FSkinProp.IMG_STAR_OUTLINE),
                                bounds.x, bounds.y + bounds.height/2 - (scaleArt*2), scaleArt/2, scaleArt/2);
                    }
                }
                g.setColor(Color.white);
                Shape clip = g.getClip();
                g.setClip(bounds);
                g.drawString(item.getName(), bounds.x + 10, bounds.y + 20);
                g.setClip(clip);
            }

            //draw foil effect if needed
            if (item instanceof IPaperCard) {
                IPaperCard paperCard = (IPaperCard)item;
                if (paperCard.isFoil()) {
                    final CardView card = CardView.getCardForUi(paperCard);
                    if (card.getCurrentState().getFoilIndex() == 0) { //if foil finish not yet established, assign a random one
                        // FIXME should assign a random foil here in all cases
                        // (currently assigns 1 for the deck editors where foils "flicker" otherwise)
                        if (item instanceof Card) {
                            card.getCurrentState().setFoilIndexOverride(-1); //-1 to set random foil
                        } else if (item instanceof IPaperCard) {
                            card.getCurrentState().setFoilIndexOverride(1);
                        }
                    }
                    CardPanel.drawFoilEffect(g, card, bounds.x, bounds.y, bounds.width, bounds.height, borderSize);
                }
            }
        }
    }
}
