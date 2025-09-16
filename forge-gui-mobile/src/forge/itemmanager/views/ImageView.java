package forge.itemmanager.views;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import forge.Forge;
import forge.Forge.KeyInputAdapter;
import forge.Graphics;
import forge.ImageKeys;
import forge.adventure.util.Config;
import forge.assets.*;
import forge.assets.FSkinColor.Colors;
import forge.card.*;
import forge.card.CardRenderer.CardStackPosition;
import forge.deck.*;
import forge.deck.io.DeckPreferences;
import forge.game.card.CardView;
import forge.gamemodes.limited.CardRanker;
import forge.gamemodes.planarconquest.ConquestCommander;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.*;
import forge.itemmanager.filters.ItemFilter;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.toolbox.*;
import forge.util.ImageFetcher;
import forge.util.ImageUtil;
import forge.util.TextUtil;
import forge.util.Utils;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.IntStream;

import static forge.assets.FSkin.getDefaultSkinFile;

public class ImageView<T extends InventoryItem> extends ItemView<T> {
    private static final float PADDING = Utils.scale(5);
    private static final float PILE_SPACING_Y = 0.1f;
    private static final FSkinFont LABEL_FONT = FSkinFont.get(12);
    private TextRenderer textRenderer = new TextRenderer(true);

    private static FSkinColor getGroupHeaderForeColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_TEXT);
        return FSkinColor.get(Colors.CLR_TEXT);
    }

    private static FSkinColor getGroupHeaderLineColor() {
        return getGroupHeaderForeColor().alphaColor(0.5f);
    }

    private static final FSkinFont GROUP_HEADER_FONT = LABEL_FONT;
    private static final float GROUP_HEADER_HEIGHT = Utils.scale(19);
    private static final float GROUP_HEADER_GLYPH_WIDTH = Utils.scale(6);
    private static final float GROUP_HEADER_LINE_THICKNESS = Utils.scale(1);
    private static final float SEL_BORDER_SIZE = Utils.scale(1);
    private static final int MIN_COLUMN_COUNT = Forge.isLandscapeMode() ? 2 : 1;
    private static final int MAX_COLUMN_COUNT = 10;

    private Supplier<List<Integer>> selectedIndices = Suppliers.memoize(ArrayList::new);
    private int columnCount = 4;
    private float scrollHeight = 0;
    private ColumnDef pileBy = null;
    private GroupDef groupBy = null;
    private ItemInfo focalItem;
    private boolean updatingLayout;
    private float totalZoomAmount;
    private Supplier<List<ItemInfo>> orderedItems = Suppliers.memoize(ArrayList::new);
    private Supplier<List<Group>> groups = Suppliers.memoize(ArrayList::new);
    private Function<Entry<? extends InventoryItem, Integer>, ?> fnIsFavorite = ColumnDef.FAVORITE.fnDisplay, fnPrice = null;

    private class SafeList<T> {
        private final List<T> internalList;
        private final Object lock = new Object(); // Object for synchronization

        private SafeList() {
            this.internalList = new ArrayList<>();
        }

        private void add(T element) {
            synchronized (lock) {
                internalList.add(element);
            }
        }

        private T get(int index) {
            synchronized (lock) {
                return internalList.get(index);
            }
        }

        private T remove(int index) {
            synchronized (lock) {
                return internalList.remove(index);
            }
        }

        private int size() {
            synchronized (lock) {
                return internalList.size();
            }
        }

        private void clear() {
            synchronized (lock) {
                internalList.clear();
            }
        }

        private boolean isEmpty() {
            synchronized (lock) {
                return internalList.isEmpty();
            }
        }

        private boolean addAll(Collection c) {
            synchronized (lock) {
                return internalList.addAll(c);
            }
        }

        // Add other list operations as needed, ensuring synchronization
    }

    private class ExpandCollapseButton extends FLabel {
        private boolean isAllCollapsed;

        private ExpandCollapseButton() {
            super(new FLabel.ButtonBuilder());
            setCommand(e -> {
                if (groupBy == null || model.getItems().isEmpty()) {
                    return;
                }

                boolean collapsed = !isAllCollapsed;
                for (Group group : groups.get()) {
                    group.isCollapsed = collapsed;
                }

                updateIsAllCollapsed();
                clearSelection(); //must clear selection since indices and visible items will be changing
                updateLayout(false);
            });
        }

        private void updateIsAllCollapsed() {
            boolean isAllCollapsed0 = true;
            for (Group group : groups.get()) {
                if (!group.isCollapsed) {
                    isAllCollapsed0 = false;
                    break;
                }
            }
            isAllCollapsed = isAllCollapsed0;
        }

        @Override
        protected void drawContent(Graphics g, float w, float h, final boolean pressed) {
            float lineThickness = Utils.scale(1);
            float offset = 2 * lineThickness;
            float squareSize = Math.round(w / 2 - offset);
            if (squareSize % 2 == 1) {
                squareSize++; //needs to be even number for this to look right
            }
            float x = Math.round((w - squareSize) / 2 - offset);
            float y = Math.round((h - squareSize) / 2 - offset);
            if (pressed) {
                y += lineThickness;
            } else {
                x -= lineThickness;
            }

            for (int i = 0; i < 2; i++) {
                g.drawLine(lineThickness, getGroupHeaderForeColor(), x, y, x + squareSize, y);
                g.drawLine(lineThickness, getGroupHeaderForeColor(), x + squareSize, y, x + squareSize, y + offset);
                g.drawLine(lineThickness, getGroupHeaderForeColor(), x, y, x, y + squareSize);
                g.drawLine(lineThickness, getGroupHeaderForeColor(), x, y + squareSize, x + offset, y + squareSize);
                x += offset;
                y += offset;
            }
            g.drawLine(lineThickness, getGroupHeaderForeColor(), x, y, x + squareSize, y);
            g.drawLine(lineThickness, getGroupHeaderForeColor(), x + squareSize, y, x + squareSize, y + squareSize);
            g.drawLine(lineThickness, getGroupHeaderForeColor(), x, y, x, y + squareSize);
            g.drawLine(lineThickness, getGroupHeaderForeColor(), x, y + squareSize, x + squareSize, y + squareSize);
            g.drawLine(lineThickness, getGroupHeaderForeColor(), x + offset + 1, y + squareSize / 2, x + squareSize - offset, y + squareSize / 2);
            if (isAllCollapsed) {
                g.drawLine(lineThickness, getGroupHeaderForeColor(), x + squareSize / 2, y + offset, x + squareSize / 2, y + squareSize - offset - 1);
            }
        }
    }

    private final ExpandCollapseButton btnExpandCollapseAll = new ExpandCollapseButton();
    private final FComboBox<Object> cbGroupByOptions = new FComboBox<>(Forge.getLocalizer().getMessage("lblGroups") + " ");
    private final FComboBox<Object> cbPileByOptions = new FComboBox<>(Forge.getLocalizer().getMessage("lblPiles") + " ");

    public ImageView(ItemManager<T> itemManager0, ItemManagerModel<T> model0) {
        super(itemManager0, model0);

        SItemManagerUtil.populateImageViewOptions(itemManager0, cbGroupByOptions, cbPileByOptions);

        cbGroupByOptions.setChangedHandler(e -> {
            if (cbGroupByOptions.getSelectedIndex() > 0) {
                setGroupBy((GroupDef) cbGroupByOptions.getSelectedItem());
            } else {
                setGroupBy(null);
            }
        });
        cbPileByOptions.setChangedHandler(e -> {
            if (cbPileByOptions.getSelectedIndex() > 0) {
                setPileBy((ColumnDef) cbPileByOptions.getSelectedItem());
            } else {
                setPileBy(null);
            }
        });

        cbGroupByOptions.setFont(LABEL_FONT);
        cbPileByOptions.setFont(LABEL_FONT);
        getPnlOptions().add(btnExpandCollapseAll);
        getPnlOptions().add(cbGroupByOptions);
        getPnlOptions().add(cbPileByOptions);

        Group group = new Group(""); //add default group
        groups.get().add(group);
        getScroller().add(group);
    }

    @Override
    public void setup(ItemManagerConfig config, Map<ColumnDef, ItemColumn> colOverrides) {
        setGroupBy(config.getGroupBy(), true);
        setPileBy(config.getPileBy(), true);
        setColumnCount(config.getImageColumnCount(), true);

        if (colOverrides != null) {
            if (colOverrides.containsKey(ColumnDef.FAVORITE) && colOverrides.get(ColumnDef.FAVORITE).getFnDisplay() != null) {
                this.fnIsFavorite = colOverrides.get(ColumnDef.FAVORITE).getFnDisplay();
            }
            if (colOverrides.containsKey(ColumnDef.PRICE) && colOverrides.get(ColumnDef.PRICE).getFnDisplay() != null) {
                this.fnPrice = colOverrides.get(ColumnDef.PRICE).getFnDisplay();
            }
        }
    }

    public GroupDef getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(GroupDef groupBy0) {
        setGroupBy(groupBy0, false);
    }

    private void setGroupBy(GroupDef groupBy0, boolean forSetup) {
        if (groupBy == groupBy0) {
            return;
        }
        groupBy = groupBy0;

        if (groupBy == null) {
            cbGroupByOptions.setSelectedIndex(0);
        } else {
            cbGroupByOptions.setSelectedItem(groupBy);
        }

        groups.get().clear();

        if (groupBy == null) {
            groups.get().add(new Group(""));
            btnExpandCollapseAll.updateIsAllCollapsed();
        } else {
            for (String groupName : groupBy.getGroups()) {
                groups.get().add(new Group(groupName));
            }

            //collapse all groups by default if all previous groups were collapsed
            if (btnExpandCollapseAll.isAllCollapsed) {
                for (Group group : groups.get()) {
                    group.isCollapsed = true;
                }
            }
        }

        getScroller().clear();
        for (Group group : groups.get()) {
            getScroller().add(group);
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
        if (pileBy == pileBy0) {
            return;
        }
        pileBy = pileBy0;

        if (pileBy == null) {
            cbPileByOptions.setSelectedIndex(0);
        } else {
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
        clearSelection(); //just clear selection instead of fixing selection this way
        setScrollValue(scrollValueToRestore); //ensure scroll value restored
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
        } else if (columnCount0 > MAX_COLUMN_COUNT) {
            columnCount0 = MAX_COLUMN_COUNT;
        }
        if (columnCount == columnCount0) {
            return;
        }
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

        //if not item hovered, use first fully visible item as focal point
        final float visibleTop = getScrollValue();
        for (Group group : groups.get()) {
            if (group.getBottom() < visibleTop) {
                continue;
            }
            for (int i = 0; i < group.piles.size(); i++) {
                Pile pile = group.piles.get(i);
                if (pile == null)
                    continue;
                if (group.getBottom() < visibleTop) {
                    continue;
                }
                for (int j = 0; j < pile.items.size(); j++) {
                    ItemInfo item = pile.items.get(j);
                    if (item == null)
                        continue;
                    if (item.getTop() >= visibleTop) {
                        return item;
                    }
                }
            }
        }
        if (orderedItems.get().isEmpty()) {
            return null;
        }
        return orderedItems.get().get(0);
    }

    @Override
    protected void onResize(float visibleWidth, float visibleHeight) {
        updateLayout(false); //need to update layout to adjust wrapping of items
    }

    @Override
    protected void onRefresh() {
        Group otherItems = groupBy == null ? groups.get().get(0) : null;

        for (Group group : groups.get()) {
            group.items.clear();
        }
        clearSelection();

        if (model.getOrderedList() != null) {
            for (Entry<T, Integer> itemEntry : new ArrayList<>(model.getOrderedList())) {
                T item = itemEntry.getKey();
                int qty = itemEntry.getValue();
                int groupIndex = groupBy == null ? -1 : groupBy.getItemGroupIndex(item);

                Group group;
                if (groupIndex >= 0) {
                    if (groupIndex >= groups.get().size())
                        group = groups.get().get(groups.get().size() - 1);
                    else
                        group = groups.get().get(groupIndex);
                } else {
                    if (otherItems == null) {
                        //reuse existing Other group if possible
                        if (groups.get().size() > groupBy.getGroups().length) {
                            otherItems = groups.get().get(groups.get().size() - 1);
                        } else {
                            otherItems = new Group(Forge.getLocalizer().getMessage("lblOther"));
                            otherItems.isCollapsed = btnExpandCollapseAll.isAllCollapsed;
                            groups.get().add(otherItems);
                            getScroller().add(otherItems);
                        }
                    }
                    group = otherItems;
                }

                for (int i = 0; i < qty; i++) {
                    group.add(new ItemInfo(item, group));
                }
            }
        }

        if (otherItems == null && groups.get().size() > groupBy.getGroups().length) {
            int index = groups.get().size() - 1;
            if (index < groups.get().size() && index >= 0)
                groups.get().remove(index); //remove Other group if empty
            btnExpandCollapseAll.updateIsAllCollapsed();
        }

        updateLayout(true);
    }

    @Override
    protected void layoutOptionsPanel(float width, float height) {
        float padding = ItemFilter.PADDING;
        float x = 0;
        float h = FTextField.getDefaultHeight(ItemFilter.DEFAULT_FONT);
        float y = padding;
        btnExpandCollapseAll.setBounds(x, y, h, h);
        x += h + padding;

        // hide piles only for deckmanager since its unusable unlike group
        float newWidth = itemManager instanceof DeckManager ? 0f : width / 2f;
        float pileByWidth = newWidth - padding;
        float groupByWidth = width - x - newWidth;

        cbGroupByOptions.setBounds(x, y, groupByWidth, h);
        x += groupByWidth + padding;
        cbPileByOptions.setBounds(x, y, pileByWidth, h);
    }

    private void updateLayout(boolean forRefresh) {
        if (updatingLayout) {
            return;
        } //prevent infinite loop
        updatingLayout = true;

        focalItem = null; //clear cached focalItem when layout changes

        float x, groupY, pileY, pileHeight, maxPileHeight;
        float y = PADDING;
        float groupX = PADDING;
        float itemAreaWidth = getScroller().getWidth();
        float groupWidth = itemAreaWidth - 2 * groupX;

        float gap = (MAX_COLUMN_COUNT - columnCount) / 2f + Utils.scale(2); //more items per row == less gap between them
        float itemWidth = (groupWidth + gap) / columnCount - gap;
        if (pileBy != null) {
            //if showing piles, make smaller so part of the next card is visible so it's obvious if scrolling is needed
            itemWidth *= (1 - 0.2f / columnCount);
        }
        float itemHeight = itemWidth * FCardPanel.ASPECT_RATIO;
        float dx = itemWidth + gap;
        float dy = pileBy == null ? itemHeight + gap : itemHeight * PILE_SPACING_Y;

        for (int i = 0; i < groups.get().size(); i++) {
            Group group = groups.get().get(i);

            if (forRefresh && pileBy != null) { //refresh piles if needed
                //use TreeMap to build pile set so iterating below sorts on key
                ColumnDef groupPileBy = groupBy == null ? pileBy : groupBy.getGroupPileBy(i, pileBy);
                Map<Comparable<?>, Pile> piles = new TreeMap<>();
                for (int j = 0; j < group.items.size(); j++) {
                    ItemInfo itemInfo = group.items.get(j);
                    if (itemInfo == null)
                        continue;
                    Comparable<?> key = groupPileBy.fnSort.apply(itemInfo);
                    if (key != null && !piles.containsKey(key)) {
                        piles.put(key, new Pile());
                    }
                    Pile p = key == null ? null : piles.getOrDefault(key, null);
                    if (p != null)
                        p.items.add(itemInfo);
                }
                group.piles.clear();
                group.piles.addAll(piles.values());
            }

            groupY = y;

            if (group.items.isEmpty()) {
                group.setBounds(groupX, groupY, groupWidth, 0);
                group.scrollWidth = groupWidth;
                continue;
            }

            if (groupBy != null) {
                y += GROUP_HEADER_HEIGHT + PADDING; //leave room for group header
                if (group.isCollapsed) {
                    group.setBounds(groupX, groupY, groupWidth, GROUP_HEADER_HEIGHT);
                    group.scrollWidth = groupWidth;
                    continue;
                }
            }

            if (pileBy == null) {
                //if not piling by anything, wrap items using a pile for each row
                group.piles.clear();
                Pile pile = new Pile();
                x = 0;

                for (int j = 0; j < group.items.size(); j++) {
                    ItemInfo itemInfo = group.items.get(j);
                    if (itemInfo == null)
                        continue;
                    itemInfo.pos = CardStackPosition.Top;

                    if (pile.items.size() == columnCount) {
                        pile = new Pile();
                        x = 0;
                        y += dy;
                    }

                    itemInfo.setBounds(x, y, itemWidth, itemHeight);

                    if (pile.items.isEmpty()) {
                        pile.setBounds(0, y, groupWidth, itemHeight);
                        group.piles.add(pile);
                    }
                    pile.items.add(itemInfo);
                    x += dx;
                }
                y += itemHeight;
                group.scrollWidth = groupWidth;
            } else {
                x = 0;
                pileY = y;
                maxPileHeight = 0;
                for (int j = 0; j < group.piles.size(); j++) {
                    Pile pile = group.piles.get(j);
                    y = pileY;
                    for (int k = 0; k < pile.items.size(); k++) {
                        ItemInfo itemInfo = pile.items.get(k);
                        if (itemInfo == null)
                            continue;
                        itemInfo.pos = CardStackPosition.BehindVert;
                        itemInfo.setBounds(x, y, itemWidth, itemHeight);
                        y += dy;
                    }
                    pile.items.get(pile.items.size() - 1).pos = CardStackPosition.Top;
                    pileHeight = y + itemHeight - dy - pileY;
                    if (pileHeight > maxPileHeight) {
                        maxPileHeight = pileHeight;
                    }
                    pile.setBounds(x, pileY, itemWidth, pileHeight);
                    x += dx;
                }
                y = pileY + maxPileHeight; //update y for setting group height below
                group.scrollWidth = Math.max(x - gap, groupWidth);
            }

            group.setBounds(groupX, groupY, groupWidth, y - groupY);
            y += PADDING;
        }
        scrollHeight = y;

        if (forRefresh) { //refresh ordered items if needed
            int index = 0;
            orderedItems.get().clear();
            for (Group group : groups.get()) {
                if (group.items.isEmpty()) {
                    continue;
                }

                if (group.isCollapsed && pileBy == null) {
                    //Piles won't have been generated in this case.
                    for (int i = 0; i < group.items.size(); i++) {
                        ItemInfo itemInfo = group.items.get(i);
                        if (itemInfo == null)
                            continue;
                        itemInfo.index = index++;
                        orderedItems.get().add(itemInfo);
                    }
                    continue;
                }

                for (int i = 0; i < group.piles.size(); i++) {
                    Pile pile = group.piles.get(i);
                    if (pile == null)
                        continue;
                    for (int j = 0; j < pile.items.size(); j++) {
                        ItemInfo itemInfo = pile.items.get(j);
                        if (itemInfo == null)
                            continue;
                        itemInfo.index = index++;
                        orderedItems.get().add(itemInfo);
                    }
                }
            }
        }
        getScroller().revalidate();
        updatingLayout = false;
    }

    @Override
    protected float getScrollHeight() {
        return scrollHeight;
    }

    @Override
    protected boolean tap(float x, float y, int count) {
        ItemInfo item = getItemAtPoint(x, y);
        if (count == 1) {
            selectItem(item);
            if (item != null)
                itemManager.showMenu(true, item.getLeft(), item.getWidth());
            else
                itemManager.showMenu(true);
        } else if (count == 2) {
            if (item != null && item.selected) {
                if (!(item.getKey() instanceof DeckProxy)) {
                    itemManager.activateSelectedItems();
                    itemManager.closeMenu();
                }
            }
        }
        return true;
    }

    @Override
    protected boolean zoom(float x, float y, float amount) {
        totalZoomAmount += amount;

        float columnZoomAmount = 2 * Utils.AVG_FINGER_WIDTH;
        while (totalZoomAmount >= columnZoomAmount) {
            setColumnCount(getColumnCount() - 1);
            totalZoomAmount -= columnZoomAmount;
        }
        while (totalZoomAmount <= -columnZoomAmount) {
            setColumnCount(getColumnCount() + 1);
            totalZoomAmount += columnZoomAmount;
        }
        return true;
    }

    private ItemInfo getItemAtPoint(float x, float y) {
        //check selected items first since they appear on top
        for (int i = selectedIndices.get().size() - 1; i >= 0; i--) {
            int currentIndex = selectedIndices.get().get(i);
            if (currentIndex < 0 || orderedItems.get().size() <= currentIndex)
                continue;
            ItemInfo item = orderedItems.get().get(currentIndex);
            float relX = x + item.group.getScrollLeft() - item.group.getLeft();
            float relY = y + getScrollValue();
            if (item.contains(relX, relY)) {
                return item;
            }
        }

        for (int i = groups.get().size() - 1; i >= 0; i--) {
            Group group = groups.get().get(i);
            if (!group.isCollapsed && !group.items.isEmpty()) {
                for (int j = group.piles.size() - 1; j >= 0; j--) {
                    float relX = x + group.getScrollLeft() - group.getLeft();
                    float relY = y + getScrollValue();
                    Pile pile = group.piles.get(j);
                    if (pile.contains(relX, relY)) {
                        for (int k = pile.items.size() - 1; k >= 0; k--) {
                            ItemInfo item = pile.items.get(k);
                            if (item.contains(relX, relY)) {
                                return item;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public T getItemAtIndex(int index) {
        if (index >= 0 && index < getCount()) {
            return orderedItems.get().get(index).item;
        }
        return null;
    }

    @Override
    public int getIndexOfItem(T item) {
        for (Group group : groups.get()) {
            for (int i = 0; i <  group.items.size(); i++) {
                ItemInfo itemInfo =  group.items.get(i);
                if (itemInfo == null)
                    continue;
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
        return selectedIndices.get().isEmpty() ? -1 : selectedIndices.get().get(0);
    }

    @Override
    public Iterable<Integer> getSelectedIndices() {
        return selectedIndices.get();
    }

    @Override
    public int getCount() {
        return orderedItems.get().size();
    }

    @Override
    public int getSelectionCount() {
        return selectedIndices.get().size();
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
    public FImage getIcon() {
        if (itemManager.getGenericType().equals(DeckProxy.class)) {
            return FSkinImage.PACK;
        }
        return FSkinImage.CARD_IMAGE;
    }

    @Override
    public String getCaption() {
        return Forge.getLocalizer().getMessage("lblImageView");
    }

    @Override
    public void selectAll() {
        clearSelection();
        IntStream.range(0, getCount()).forEach(selectedIndices.get()::add);
        updateSelection();
        onSelectionChange();
    }

    @Override
    protected void onSetSelectedIndex(int index) {
        clearSelection();
        selectedIndices.get().add(index);
        updateSelection();
    }

    @Override
    protected void onSetSelectedIndices(Iterable<Integer> indices) {
        clearSelection();
        for (Integer index : indices) {
            selectedIndices.get().add(index);
        }
        updateSelection();
    }

    private void clearSelection() {
        int count = getCount();
        for (Integer i : selectedIndices.get()) {
            if (i < count) {
                orderedItems.get().get(i).selected = false;
            }
        }
        selectedIndices.get().clear();
    }

    private void updateSelection() {
        for (Integer i : selectedIndices.get()) {
            orderedItems.get().get(i).selected = true;
        }
    }

    private boolean selectItem(ItemInfo item) {
        if (item == null) {
            if (!KeyInputAdapter.isCtrlKeyDown() && !KeyInputAdapter.isShiftKeyDown()) {
                if (minSelections == 0) {
                    clearSelection();
                    onSelectionChange();
                }
            }
            return false;
        }

        if (item.selected) { //unselect item if already selected
            if (selectedIndices.get().size() > minSelections) {
                item.selected = false;
                selectedIndices.get().remove((Object) item.index);
                onSelectionChange();
                item.group.scrollIntoView(item);
            }
            return true;
        }
        if (maxSelections <= 1 || (!KeyInputAdapter.isCtrlKeyDown() && !KeyInputAdapter.isShiftKeyDown())) {
            clearSelection();
        }
        if (selectedIndices.get().size() < maxSelections) {
            selectedIndices.get().add(0, item.index);
            item.selected = true;
            onSelectionChange();
            item.group.scrollIntoView(item);
            getScroller().scrollIntoView(item);
        }
        return true;
    }

    @Override
    public void scrollSelectionIntoView() {
        if (selectedIndices.get().isEmpty()) {
            return;
        }
        int index = selectedIndices.get().get(0);
        if (index < 0 || orderedItems.get().size() <= index) {
            return;
        }

        ItemInfo itemInfo = orderedItems.get().get(index);
        getScroller().scrollIntoView(itemInfo);
    }

    @Override
    public Rectangle getSelectionBounds() {
        if (selectedIndices.get().isEmpty()) {
            return new Rectangle();
        }

        int index = selectedIndices.get().get(0);
        if (index < 0 || orderedItems.get().size() <= index) {
            return new Rectangle();
        }
        ItemInfo itemInfo = orderedItems.get().get(index);
        Vector2 relPos = itemInfo.group.getChildRelativePosition(itemInfo);
        return new Rectangle(itemInfo.group.screenPos.x + relPos.x - SEL_BORDER_SIZE + itemInfo.group.getLeft(),
                itemInfo.group.screenPos.y + relPos.y - SEL_BORDER_SIZE,
                itemInfo.getWidth() + 2 * SEL_BORDER_SIZE, itemInfo.getHeight() + 2 * SEL_BORDER_SIZE);
    }

    @Override
    public void zoomSelected() {
        if (selectedIndices.get().isEmpty()) {
            return;
        }
        int index = selectedIndices.get().get(0);
        if (index < 0 || orderedItems.get().size() <= index) {
            return;
        }

        ItemInfo itemInfo = orderedItems.get().get(index);
        if (itemInfo != null) {
            if (itemInfo.getKey() instanceof CardThemedDeckGenerator || itemInfo.getKey() instanceof CommanderDeckGenerator
                    || itemInfo.getKey() instanceof ArchetypeDeckGenerator || itemInfo.getKey() instanceof DeckProxy) {
                FDeckViewer.show(((DeckProxy) itemInfo.getKey()).getDeck());
            }
            CardZoom.show(orderedItems.get(), orderedItems.get().indexOf(itemInfo), itemManager);
        }
    }

    private class Group extends FScrollPane {
        private final SafeList<ItemInfo> items = new SafeList<>();
        private final SafeList<Pile> piles = new SafeList<>();
        private final String name;
        private boolean isCollapsed;
        private float scrollWidth;

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
            if (items.isEmpty()) {
                return;
            }

            if (groupBy != null) {
                //draw group name and horizontal line
                float x = GROUP_HEADER_GLYPH_WIDTH + PADDING + 1;
                float y = 0;
                String caption = name + " (" + items.size() + ")";
                g.drawText(caption, GROUP_HEADER_FONT, getGroupHeaderForeColor(), x, y, getWidth(), GROUP_HEADER_HEIGHT, false, Align.left, true);
                x += GROUP_HEADER_FONT.getBounds(caption).width + PADDING;
                y += GROUP_HEADER_HEIGHT / 2;
                g.drawLine(GROUP_HEADER_LINE_THICKNESS, getGroupHeaderLineColor(), x, y, getWidth(), y);

                //draw expand/collapse glyph
                float offset = GROUP_HEADER_GLYPH_WIDTH / 2 + 1;
                x = offset;
                if (isCollapsed) {
                    y += GROUP_HEADER_LINE_THICKNESS;
                    g.fillTriangle(getGroupHeaderLineColor(),
                            x, y - offset,
                            x + offset, y,
                            x, y + offset);
                } else {
                    g.fillTriangle(getGroupHeaderLineColor(),
                            x - offset + 2, y + offset - 1,
                            x + offset, y + offset - 1,
                            x + offset, y - offset + 1);
                }

                if (isCollapsed) {
                    return;
                }
            }

            float visibleLeft = getScrollLeft();
            float visibleRight = visibleLeft + getWidth();
            for (int i = 0; i < piles.size(); i++) {
                Pile pile = piles.get(i);
                if (pile == null)
                    continue;
                if (pile.getRight() < visibleLeft) {
                    continue;
                }
                if (pile.getLeft() >= visibleRight) {
                    break;
                }
                pile.draw(g);
            }
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            return new ScrollBounds(scrollWidth, visibleHeight);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            ItemInfo item = getItemAtPoint(x + getLeft(), y + getTop());
            if (item != null) {
                if (item.getKey() instanceof DeckProxy) {
                    DeckProxy dp = (DeckProxy) item.getKey();
                    if (count >= 2 && !dp.isGeneratedDeck()) {
                        //double tap to add to favorites or remove....
                        if (DeckPreferences.getPrefs(dp).getStarCount() > 0)
                            DeckPreferences.getPrefs(dp).setStarCount(0);
                        else
                            DeckPreferences.getPrefs(dp).setStarCount(1);

                        updateLayout(false);
                    }
                }
            }
            if (groupBy != null && !items.isEmpty() && y < GROUP_HEADER_HEIGHT) {
                isCollapsed = !isCollapsed;
                btnExpandCollapseAll.updateIsAllCollapsed();
                clearSelection(); //must clear selection since indices and visible items will be changing
                updateLayout(false);
                return true;
            }
            return false;
        }

        @Override
        public boolean longPress(float x, float y) {
            ItemInfo item = getItemAtPoint(x + getLeft(), y + getTop());
            if (item != null) {
                if (item.getKey() instanceof CardThemedDeckGenerator || item.getKey() instanceof CommanderDeckGenerator
                        || item.getKey() instanceof ArchetypeDeckGenerator || item.getKey() instanceof DeckProxy) {
                    FDeckViewer.show(((DeckProxy) item.getKey()).getDeck());
                    return true;
                }
                CardZoom.show(orderedItems.get(), orderedItems.get().indexOf(item), itemManager);
                return true;
            }
            return false;
        }

        //provide special override for this function to account for special ItemInfo positioning logic
        @Override
        protected Vector2 getChildRelativePosition(FDisplayObject child, float offsetX, float offsetY) {
            return new Vector2(child.getLeft() - getScrollLeft() + offsetX - getLeft(), child.getTop() - getScrollValue() + offsetY - getTop());
        }
    }

    private class Pile extends FDisplayObject {
        private final SafeList<ItemInfo> items = new SafeList<>();

        @Override
        public void draw(Graphics g) {
            final float visibleTop = getScrollValue();
            final float visibleBottom = visibleTop + getScroller().getHeight();

            ItemInfo skippedItem = null;
            for (int i = 0; i < items.size(); i++) {
                ItemInfo itemInfo = items.get(i);
                if (itemInfo == null)
                    continue;
                if (itemInfo.getBottom() < visibleTop) {
                    continue;
                }
                if (itemInfo.getTop() >= visibleBottom) {
                    break;
                }
                if (itemInfo.selected) {
                    skippedItem = itemInfo;
                } else {
                    itemInfo.draw(g);
                }
            }
            if (skippedItem != null) {
                CardStackPosition backupPos = skippedItem.pos;
                skippedItem.pos = CardStackPosition.Top; //ensure skipped item rendered as if it was on top
                skippedItem.draw(g);
                skippedItem.pos = backupPos;
            }
        }
    }

    private class ItemInfo extends FDisplayObject implements Entry<InventoryItem, Integer>, ImageFetcher.Callback {
        private final T item;
        private Integer cardPrice;
        private final Group group;
        private int index, draftRank;
        private FSkinImage draftRankImage = FSkinImage.DRAFTRANK_D;
        private CardStackPosition pos;
        private boolean selected, deckSelectMode, showRanking;
        private final float IMAGE_SIZE = CardRenderer.MANA_SYMBOL_SIZE;
        private DeckProxy deckProxy = null;
        private String markedColors = null;
        private FImageComplex deckCover = null;
        private Texture dpImg = null;
        //private TextureRegion tr;

        private ItemInfo(T item0, Group group0) {
            item = item0;
            group = group0;
            if (item instanceof DeckProxy) {
                deckSelectMode = true;
                deckProxy = (DeckProxy) item;
            }
            if (item instanceof PaperCard pc) {
                showRanking = itemManager.getShowRanking() && FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_OVERLAY_DRAFT_RANKING);
                if (showRanking) {
                    double score = CardRanker.getRawScore(pc);
                    draftRank = score <= 0 ? 0 : score > 99 ? 99 : (int) Math.round(CardRanker.getRawScore((PaperCard) item));
                    if (draftRank >= 90) {
                        draftRankImage = FSkinImage.DRAFTRANK_S;
                    } else if (draftRank >= 80) {
                        draftRankImage = FSkinImage.DRAFTRANK_A;
                    } else if (draftRank >= 60) {
                        draftRankImage = FSkinImage.DRAFTRANK_B;
                    } else if (draftRank >= 25) {
                        draftRankImage = FSkinImage.DRAFTRANK_C;
                    }
                }
                if (pc.getMarkedColors() != null) {
                    markedColors = pc.getMarkedColors().toString();
                }
            }
            if(fnPrice != null) {
                cardPrice = (Integer) fnPrice.apply(this);
            }
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

        private void drawCardLabel(Graphics g, String message, Color bgColor, float x, float y, float w, float h) {
            FSkinFont skinFont = FSkinFont.forHeight(w / 7);
            float fontHeight = skinFont.getLineHeight();
            float ymod = h * 0.7f - fontHeight / 2;
            float oldAlpha = g.getfloatAlphaComposite();
            g.setAlphaComposite(0.6f);
            g.fillRect(bgColor, x, y + ymod, w, fontHeight);
            g.setAlphaComposite(oldAlpha);
            textRenderer.drawText(g, message, skinFont, Color.BLACK, x, y + ymod, w, fontHeight, y + ymod, fontHeight, false, Align.center, true);
        }

        @Override
        public void draw(Graphics g) {
            final float x = getLeft() - group.getScrollLeft();
            final float y = getTop() - group.getTop() - getScrollValue();
            final float w = getWidth();
            final float h = getHeight();
            if (deckSelectMode) {
                dpImg = ImageCache.getInstance().getImage(item);
            }
            if (selected) {
                if (!deckSelectMode) {
                    //if round border is enabled, the select highlight is also rounded..
                    if (Forge.enableUIMask.equals("Full")) {
                        //fillroundrect has rough/aliased corner
                        g.fillRoundRect(Color.GREEN, x - SEL_BORDER_SIZE, y - SEL_BORDER_SIZE, w + 2 * SEL_BORDER_SIZE, h + 2 * SEL_BORDER_SIZE, (h - w) / 10);
                        //drawroundrect has GL_SMOOTH to `smoothen/faux` the aliased corner
                        g.drawRoundRect(1f, Color.GREEN, x - SEL_BORDER_SIZE, y - SEL_BORDER_SIZE, w + 1.5f * SEL_BORDER_SIZE, h + 1.5f * SEL_BORDER_SIZE, (h - w) / 10);
                    } else //default rectangle highlight
                        g.fillRect(Color.GREEN, x - SEL_BORDER_SIZE, y - SEL_BORDER_SIZE, w + 2 * SEL_BORDER_SIZE, h + 2 * SEL_BORDER_SIZE);
                }
            }

            if (item instanceof PaperCard pc) {
                CardRenderer.drawCard(g, pc, x, y, w, h, pos);
                if (showRanking) {
                    float rankSize = w / 2;
                    float y2 = y + (rankSize - (rankSize * 0.1f));
                    float x2 = x + rankSize / 2;
                    g.drawImage(draftRankImage, x2, y2 + 1, rankSize, rankSize);
                    g.drawText(String.valueOf(draftRank), FSkinFont.forHeight(rankSize / 4), Color.WHITE, x, y, w, h, true, Align.center, true);
                }

                if (itemManager.showPriceInfo()) {
                    if (pc.hasNoSellValue() && !(Forge.isMobileAdventureMode || Config.instance().getSettingData().disableNotForSale)) {
                        Texture nfs = Forge.getAssets().getTexture(getDefaultSkinFile("nfs.png"), false);
                        if (nfs != null)
                            g.drawImage(nfs, x, y, w, h);
                        else
                            drawCardLabel(g, Forge.getLocalizer().getMessage("lblNoSell"), Color.RED, x, y, w, h);
                    }
                    else {
                        if (cardPrice != null)
                            drawCardLabel(g, "{CS} " + cardPrice, Color.GOLD, x, y, w, h);
                    }
                }
                // spire colors
                if (markedColors != null && !markedColors.isEmpty()) {
                    textRenderer.drawText(g, markedColors, FSkinFont.forHeight(w / 5), Color.WHITE, x, y + h / 4, w, h, y, h, false, Align.center, true);
                }
            } else if (item instanceof ConquestCommander) {
                CardRenderer.drawCard(g, ((ConquestCommander) item).getCard(), x, y, w, h, pos);
            } else if (deckSelectMode) {
                ColorSet deckColor = deckProxy.getColor();
                float scale = 0.75f;

                if (dpImg != null) {//generated decks have missing info...
                    if (Forge.enableUIMask.equals("Off")) {
                        if (selected)
                            g.fillRect(Color.GREEN, x - SEL_BORDER_SIZE, y - SEL_BORDER_SIZE, w + 2 * SEL_BORDER_SIZE, h + 2 * SEL_BORDER_SIZE);
                        g.drawImage(dpImg, x, y, w, h);
                    } else {
                        //commander bg
                        g.drawImage(FSkin.getDeckbox().get(0), FSkin.getDeckbox().get(0), x, y, w, h, Color.GREEN, selected);

                        PaperCard paperCard = null;
                        String imageKey = item.getImageKey(false);
                        if (imageKey != null) {
                            if (imageKey.startsWith(ImageKeys.CARD_PREFIX)) {
                                paperCard = ImageUtil.getPaperCardFromImageKey(imageKey);
                                CardRenderer.getCardArt(paperCard);
                            }
                        }
                        if (paperCard != null && Forge.enableUIMask.equals("Art")) {
                            CardImageRenderer.drawCardImage(g, CardView.getCardForUi(paperCard), false,
                                    x + (w - w * scale) / 2, y + (h - h * scale) / 1.5f, w * scale, h * scale, CardStackPosition.Top, true, false, false, true);
                        } else {
                            //tr = ImageCache.getInstance().croppedBorderImage(dpImg);
                            g.drawImage(dpImg, x + (w - w * scale) / 2, y + (h - h * scale) / 1.5f, w * scale, h * scale);
                            //g.drawImage(tr, x + (w - w * scale) / 2, y + (h - h * scale) / 1.5f, w * scale, h * scale);
                        }
                        //draw plastic effect overlay.
                        g.drawImage(Forge.getAssets().getTexture(getDefaultSkinFile("cover.png"), false), x + (w - w * scale) / 2, y + (h - h * scale) / 1.5f, w * scale, h * scale);
                    }
                    //fake labelname shadow
                    g.drawText(item.getName(), GROUP_HEADER_FONT, Color.BLACK, (x + PADDING) - 1f, (y + PADDING * 2) + 1f, w - 2 * PADDING, h - 2 * PADDING, true, Align.center, false);
                    //labelname
                    g.drawText(item.getName(), GROUP_HEADER_FONT, Color.WHITE, x + PADDING, y + PADDING * 2, w - 2 * PADDING, h - 2 * PADDING, true, Align.center, false);
                } else {
                    if (!deckProxy.isGeneratedDeck()) {
                        if (deckProxy.getDeck().isEmpty()) {
                            g.drawImage(FSkin.getDeckbox().get(2), FSkin.getDeckbox().get(2), x, y - (h * 0.25f), w, h, Color.RED, selected);
                        } else {
                            deckCover = CardRenderer.getCardArt(deckProxy.getDeck().getCommanders().isEmpty() ? deckProxy.getHighestCMCCard() : deckProxy.getDeck().getCommanders().get(0));
                            if (deckCover != null) {
                                g.drawDeckBox(deckCover, scale, FSkin.getDeckbox().get(1), FSkin.getDeckbox().get(2), x, y, w, h, Color.GREEN, selected);
                            } else {
                                g.drawImage(FSkin.getDeckbox().get(2), FSkin.getDeckbox().get(2), x, y - (h * 0.25f), w, h, Color.GREEN, selected);
                            }
                        }
                    } else {
                        if (itemManager.getConfig() == ItemManagerConfig.STRING_ONLY) {
                            //draw generic box for stringOnly config
                            g.drawImage(FSkin.getDeckbox().get(2), FSkin.getDeckbox().get(2), x, y - (h * 0.25f), w, h, Color.GREEN, selected);
                        } else {
                            //draw missing box display if not avail or loading...
                            g.drawImage(FSkin.getDeckbox().get(0), FSkin.getDeckbox().get(0), x, y, w, h, Color.GREEN, selected);
                            //temporary fill image
                            g.fillRect(Color.BLACK, x + (w - w * scale) / 2, y + (h - h * scale) / 1.5f, w * scale, h * scale);
                            //draw plastic effect overlay.
                            g.drawImage(Forge.getAssets().getTexture(getDefaultSkinFile("cover.png"), false), x + (w - w * scale) / 2, y + (h - h * scale) / 1.5f, w * scale, h * scale);

                        }
                    }
                    if (deckColor != null) {
                        //deck color identity
                        float symbolSize = IMAGE_SIZE;
                        if (Forge.isLandscapeMode()) {
                            if (columnCount == 4)
                                symbolSize = IMAGE_SIZE * 1.5f;
                            else if (columnCount == 3)
                                symbolSize = IMAGE_SIZE * 2f;
                            else if (columnCount == 2)
                                symbolSize = IMAGE_SIZE * 3f;
                            else if (columnCount == 1)
                                symbolSize = IMAGE_SIZE * 4f;
                        } else {
                            if (columnCount > 2)
                                symbolSize = IMAGE_SIZE * (0.5f);
                        }
                        //vertical mana icons
                        CardFaceSymbols.drawColorSet(g, deckColor, x + (w - symbolSize), y + (h / 8), symbolSize, true);
                        if (!deckProxy.isGeneratedDeck()) {
                            if (deckProxy.getDeck().isEmpty()) {
                                g.drawImage(Forge.hdbuttons ? FSkinImage.HDYIELD : FSkinImage.WARNING, x, y, symbolSize, symbolSize);
                            } else {
                                if (Forge.hdbuttons)
                                    g.drawImage(DeckPreferences.getPrefs(deckProxy).getStarCount() > 0 ? FSkinImage.HDSTAR_FILLED : FSkinImage.HDSTAR_OUTLINE, x, y, symbolSize, symbolSize);
                                else
                                    g.drawImage(DeckPreferences.getPrefs(deckProxy).getStarCount() > 0 ? FSkinImage.STAR_FILLED : FSkinImage.STAR_OUTLINE, x, y, symbolSize, symbolSize);
                                //AI Icon
                                g.drawImage(deckProxy.getAI().inMainDeck == 0 ? FSkinImage.AI_ACTIVE : FSkinImage.AI_INACTIVE, x, y + symbolSize, symbolSize, symbolSize);
                            }
                        }
                    }
                    String deckname = TextUtil.fastReplace(item.getName(), "] #", "]\n#");
                    //deckname fakeshadow
                    g.drawText(deckname, GROUP_HEADER_FONT, Color.BLACK, (x + PADDING) - 1f, (y + (h / 10) + PADDING) + 1f, w - 2 * PADDING, h - 2 * PADDING, true, Align.center, true);
                    //deck name
                    g.drawText(deckname, GROUP_HEADER_FONT, Color.WHITE, x + PADDING, y + (h / 10) + PADDING, w - 2 * PADDING, h - 2 * PADDING, true, Align.center, true);
                }
            } else {
                Texture img = ImageCache.getInstance().getImage(item);
                if (img != null) {
                    g.drawImage(img, x, y, w, h);
                } else {
                    if (img == null && item instanceof InventoryItem) {
                        String key = item.getImageKey(false);
                        if (key.startsWith(ImageKeys.PRECON_PREFIX) || key.startsWith(ImageKeys.FATPACK_PREFIX)
                                || key.startsWith(ImageKeys.BOOSTERBOX_PREFIX) || key.startsWith(ImageKeys.BOOSTER_PREFIX) || key.startsWith(ImageKeys.TOURNAMENTPACK_PREFIX)) {
                            CardView cv = new CardView(-1, null, item.getName(), null, item.getImageKey(false));
                            CardImageRenderer.drawCardImage(g, cv, false, x, y, w, h, CardStackPosition.Top, false, false);
                            return;
                        }
                    }
                    g.fillRect(Color.BLACK, x, y, w, h);
                    g.drawText(item.getName(), GROUP_HEADER_FONT, Color.WHITE, x + PADDING, y + PADDING, w - 2 * PADDING, h - 2 * PADDING, true, Align.center, false);
                }
            }

            if (itemManager.itemIsFavorite(this)) {
                float offset = w * 0.05f;
                float size = w * 0.15f;
                g.drawImage(FSkinImage.HDSTAR_FILLED, x + offset, y + h - offset - size, size, size);
            }
        }

        @Override
        public void onImageFetched() {
            if (deckSelectMode) {
                if (dpImg == ImageCache.getInstance().getDefaultImage()) {
                    dpImg = ImageCache.getInstance().getImage(item);
                }
            }
        }
    }
}
