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
package forge.itemmanager;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardZoom.ActivateHandler;
import forge.gui.FThreads;
import forge.item.InventoryItem;
import forge.itemmanager.filters.*;
import forge.itemmanager.views.ImageView;
import forge.itemmanager.views.ItemListView;
import forge.itemmanager.views.ItemView;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.screens.FScreen;
import forge.screens.planarconquest.ConquestCommandersScreen;
import forge.toolbox.*;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.toolbox.FList.CompactModeHandler;
import forge.util.*;


public abstract class ItemManager<T extends InventoryItem> extends FContainer implements IItemManager<T>, ActivateHandler {
    private ItemManager<T> instance;
    private float itemLeft = 0f, itemWidth = 0f;
    private ItemPool<T> pool;
    protected final ItemManagerModel<T> model;
    private Predicate<? super T> filterPredicate = null;
    private AdvancedSearchFilter<? extends T> advancedSearchFilter;
    private Supplier<List<ItemFilter<? extends T>>> filters = Suppliers.memoize(ArrayList::new);
    private boolean hideFilters = false;
    private boolean wantUnique = false;
    private boolean showRanking = false;
    private boolean showPriceInfo = false;
    private boolean multiSelectMode = false;
    private FEventHandler selectionChangedHandler, itemActivateHandler;
    private ContextMenuBuilder<T> contextMenuBuilder;
    private ContextMenu contextMenu;
    private final Class<T> genericType;
    private ItemManagerConfig config;
    private Function<Entry<? extends InventoryItem, Integer>, Object> fnNewGet, fnFavoriteGet;
    private boolean viewUpdating, needSecondUpdate;
    private Supplier<List<ItemColumn>> sortCols = Suppliers.memoize(ArrayList::new);
    private final TextSearchFilter<? extends T> searchFilter;

    private final FLabel btnSearch = new FLabel.ButtonBuilder()
            .icon(Forge.hdbuttons ? FSkinImage.HDSEARCH : FSkinImage.SEARCH).iconScaleFactor(0.9f).selectable().build();
    private final FLabel btnView = new FLabel.ButtonBuilder()
            .iconScaleFactor(0.9f).selectable().build(); //icon set later
    private final FLabel btnAdvancedSearchOptions = new FLabel.Builder()
            .selectable(true).align(Align.center)
            .icon(Forge.hdbuttons ? FSkinImage.HDPREFERENCE : FSkinImage.SETTINGS).iconScaleFactor(0.9f)
            .build();

    private final FComboBox<ItemColumn> cbxSortOptions;

    private final List<ItemView<T>> views = new ArrayList<>();
    private final ItemListView<T> listView;
    private final ImageView<T> imageView;
    private ItemView<T> currentView;
    private boolean initialized;
    protected boolean lockFiltering;

    /**
     * ItemManager Constructor.
     *
     * @param genericType0 the class of item that this table will contain
     * @param wantUnique0  whether this table should display only one item with the same name
     */
    protected ItemManager(final Class<T> genericType0, final boolean wantUnique0) {
        instance = this;
        genericType = genericType0;
        wantUnique = wantUnique0;
        model = new ItemManagerModel<>(genericType0);

        searchFilter = createSearchFilter();

        listView = new ItemListView<>(this, model);
        imageView = createImageView(model);

        views.add(listView);
        views.add(imageView);
        currentView = listView;
        btnView.setIcon(currentView.getIcon());

        //build display
        add(searchFilter.getWidget());
        add(btnSearch);
        add(btnView);
        add(btnAdvancedSearchOptions);
        btnAdvancedSearchOptions.setSelected(!hideFilters);
        if (allowSortChange()) {
            cbxSortOptions = add(new FComboBox<>(Forge.getLocalizer().getMessage("lblSort") + ": "));
            cbxSortOptions.setFont(FSkinFont.get(12));
        } else {
            cbxSortOptions = null;
        }
        add(currentView.getPnlOptions());
        add(currentView.getScroller());

        btnSearch.setCommand(e -> {
            FPopupMenu menu = new FPopupMenu() {
                @Override
                protected void buildMenu() {
                    addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblAdvancedSearch"), Forge.hdbuttons ? FSkinImage.HDSEARCH : FSkinImage.SEARCH, e1 -> {
                        if (advancedSearchFilter == null) {
                            advancedSearchFilter = createAdvancedSearchFilter();
                            ItemManager.this.add(advancedSearchFilter.getWidget());
                        }
                        advancedSearchFilter.edit();
                    }));
                    addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblResetFilters"), Forge.hdbuttons ? FSkinImage.HDDELETE : FSkinImage.DELETE, e12 -> resetFilters()));
                }
            };
            menu.show(btnSearch, 0, btnSearch.getHeight());
        });
        btnView.setCommand(e -> {
            FPopupMenu menu = new FPopupMenu() {
                @Override
                protected void buildMenu() {
                    for (int i = 0; i < views.size(); i++) {
                        final int index = i;
                        ItemView<T> view = views.get(i);
                        FMenuItem item = new FMenuItem(view.getCaption(), view.getIcon(), e13 -> setViewIndex(index));
                        if (currentView == view) {
                            item.setSelected(true);
                        }
                        addItem(item);
                    }
                }
            };
            menu.show(btnView, 0, btnView.getHeight());
        });
        btnAdvancedSearchOptions.setCommand(e -> setHideFilters(!hideFilters));

        //setup initial filters
        addDefaultFilters();

        initialized = true; //must set flag just before applying filters
        if (!applyFilters()) {
            if (pool != null) { //ensure view updated even if filter predicate didn't change
                updateView(true, null);
            }
        }
    }

    protected ImageView<T> createImageView(final ItemManagerModel<T> model0) {
        return new ImageView<>(this, model0);
    }

    public ItemManagerConfig getConfig() {
        return config;
    }

    public void setup(ItemManagerConfig config0) {
        setup(config0, null);
    }

    public void setup(ItemManagerConfig config0, Map<ColumnDef, ItemColumn> colOverrides) {
        config = config0;
        setWantUnique(config0.getUniqueCardsOnly());

        //ensure sort cols ordered properly
        final List<ItemColumn> cols = new LinkedList<>();
        for (ItemColumnConfig colConfig : config.getCols().values()) {
            if (colOverrides == null || !colOverrides.containsKey(colConfig.getDef())) {
                cols.add(new ItemColumn(colConfig));
            } else {
                cols.add(colOverrides.get(colConfig.getDef()));
            }
        }
        cols.sort(Comparator.comparingInt(arg0 -> arg0.getConfig().getIndex()));

        sortCols.get().clear();
        if (cbxSortOptions != null) {
            cbxSortOptions.setDropDownItemTap(null);
            cbxSortOptions.removeAllItems();
        }

        int modelIndex = 0;
        for (final ItemColumn col : cols) {
            col.setIndex(modelIndex++);
            if (col.isVisible()) {
                sortCols.get().add(col);
            }
        }

        final ItemColumn[] sortcols = new ItemColumn[sortCols.get().size()];

        // Assemble priority sort.
        for (ItemColumn col : sortCols.get()) {
            if (cbxSortOptions != null) {
                cbxSortOptions.addItem(col);
            }
            if (col.getSortPriority() > 0 && col.getSortPriority() <= sortcols.length) {
                sortcols[col.getSortPriority() - 1] = col;
            }
        }

        if (cbxSortOptions != null) {
            cbxSortOptions.setText("(" + Forge.getLocalizer().getMessage("lblNone") + ")");
        }

        model.getCascadeManager().reset();

        for (int i = sortcols.length - 1; i >= 0; i--) {
            ItemColumn col = sortcols[i];
            if (col != null) {
                model.getCascadeManager().add(col, true);
                if (cbxSortOptions != null) {
                    cbxSortOptions.setSelectedItem(col);
                }
            }
        }

        if (cbxSortOptions != null) {
            cbxSortOptions.setDropDownItemTap(e -> {
                model.getCascadeManager().add((ItemColumn) e.getArgs(), false);
                model.refreshSort();
                ItemManagerConfig.save();
                updateView(true, null);
            });
        }

        for (ItemView<T> view : views) {
            view.setup(config0, colOverrides);
        }
        setViewIndex(config0.getViewIndex());
        setHideFilters(config0.getHideFilters());

        if(colOverrides == null) {
            fnNewGet = null;
            fnFavoriteGet = ColumnDef.FAVORITE.fnDisplay;
        }
        else {
            if (!colOverrides.containsKey(ColumnDef.NEW))
                fnNewGet = null;
            else
                fnNewGet = colOverrides.get(ColumnDef.NEW).getFnDisplay();

            if (!colOverrides.containsKey(ColumnDef.FAVORITE))
                fnFavoriteGet = ColumnDef.FAVORITE.fnDisplay;
            else
                fnFavoriteGet = colOverrides.get(ColumnDef.FAVORITE).getFnDisplay();
        }
    }

    protected boolean allowSortChange() {
        return true;
    }

    protected String getItemSuffix(Entry<T, Integer> item) {
        if (fnNewGet != null) {
            String suffix = fnNewGet.apply(item).toString();
            if (!suffix.isEmpty()) {
                return " *" + suffix + "*";
            }
        }
        return null;
    }

    public boolean itemIsFavorite(Entry<? extends InventoryItem, Integer> item) {
        if(fnFavoriteGet == null)
            return false;
        Integer favorite = (Integer) fnFavoriteGet.apply(item);
        return favorite != null && favorite != 0;
    }

    public abstract class ItemRenderer {
        public abstract float getItemHeight();

        public abstract boolean allowPressEffect(FList<Entry<T, Integer>> list, float x, float y);

        public abstract boolean tap(Integer index, Entry<T, Integer> value, float x, float y, int count);

        public abstract boolean longPress(Integer index, Entry<T, Integer> value, float x, float y);

        public abstract void drawValue(Graphics g, Entry<T, Integer> value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h);
    }

    public abstract ItemRenderer getListItemRenderer(final CompactModeHandler compactModeHandler);

    public void setViewIndex(int viewIndex) {
        if (viewIndex < 0 || viewIndex >= views.size()) {
            return;
        }
        ItemView<T> view = views.get(viewIndex);
        if (currentView == view) {
            return;
        }

        if (config != null) {
            config.setViewIndex(viewIndex);
        }

        final int backupIndexToSelect = currentView.getSelectedIndex();
        final Iterable<T> itemsToSelect; //only retain selected items if not single selection of first item
        if (backupIndexToSelect > 0 || currentView.getSelectionCount() > 1) {
            itemsToSelect = currentView.getSelectedItems();
        } else {
            itemsToSelect = null;
        }

        remove(currentView.getPnlOptions());
        remove(currentView.getScroller());

        currentView = view;
        btnView.setIcon(view.getIcon());

        view.refresh(itemsToSelect, backupIndexToSelect, 0);

        add(view.getPnlOptions());
        add(view.getScroller());
        revalidate();
    }

    @Override
    public void doLayout(float width, float height) {
        LayoutHelper helper = new LayoutHelper(this, ItemFilter.PADDING, ItemFilter.PADDING);
        float fieldHeight = searchFilter.getMainComponent().getHeight();
        float viewButtonWidth = fieldHeight;
        helper.offset(0, ItemFilter.PADDING);
        List<FLabel> buttons = new ArrayList<>(3);
        if(btnSearch.isEnabled())
            buttons.add(btnSearch);
        buttons.add(btnView);
        if(btnAdvancedSearchOptions.isEnabled())
            buttons.add(btnAdvancedSearchOptions);
        float rightPadding = (viewButtonWidth + helper.getGapX()) * buttons.size();
        helper.fillLine(searchFilter.getWidget(), fieldHeight, rightPadding); //leave room for search, view, and options buttons
        for(FLabel button : buttons)
            helper.include(button, viewButtonWidth, fieldHeight);
        helper.newLine();
        if (advancedSearchFilter != null && advancedSearchFilter.getWidget().isVisible()) {
            helper.fillLine(advancedSearchFilter.getWidget(), fieldHeight);
        }
        if (!hideFilters) {
            if (Forge.isLandscapeMode())
                drawLandscape(this.filters.get(), helper, fieldHeight);
            else
                drawPortrait(this.filters.get(), helper, width, fieldHeight);
        }
        helper.fill(currentView.getScroller());
    }

    private void drawLandscape(List<ItemFilter<? extends T>> filters, LayoutHelper helper, float fieldHeight) {
        // TODO reduce landscape mode combobox buttons for the filters
        for (ItemFilter<? extends T> filter : filters) {
            helper.include(filter.getWidget(), filter.getPreferredWidth(helper.getRemainingLineWidth(), fieldHeight), fieldHeight);
        }
        if (allowSortChange()) {
            helper.fillLine(cbxSortOptions, fieldHeight);
        }
        helper.newLine(-ItemFilter.PADDING);
        if (currentView.getPnlOptions().getChildCount() > 0) {
            helper.fillLine(currentView.getPnlOptions(), fieldHeight + ItemFilter.PADDING);
        } else {
            helper.offset(0, -fieldHeight); //prevent showing whitespace for empty view options panel
        }
    }

    private void drawPortrait(List<ItemFilter<? extends T>> filters, LayoutHelper helper, float width, float fieldHeight) {
        CardTypeFilter cardTypeFilter = null;
        CardColorFilter colorFilter = null;
        CardFormatFilter cardFormatFilter = null;
        DeckColorFilter deckColorFilter = null;
        DeckFormatFilter deckFormatFilter = null;
        ConquestCommandersScreen.CommanderColorFilter commanderColorFilter = null;
        ConquestCommandersScreen.CommanderOriginFilter commanderOriginFilter = null;
        for (ItemFilter<? extends T> filter : filters) {
            if (filter instanceof CardTypeFilter ct) {
                cardTypeFilter = ct;
                continue;
            }
            if (filter instanceof CardColorFilter cr) {
                colorFilter = cr;
                continue;
            }
            if (filter instanceof CardFormatFilter cf) {
                cardFormatFilter = cf;
                continue;
            }
            if (filter instanceof DeckColorFilter dc) {
                deckColorFilter = dc;
                continue;
            }
            if (filter instanceof DeckFormatFilter df) {
                deckFormatFilter = df;
                continue;
            }
            if (filter instanceof ConquestCommandersScreen.CommanderColorFilter ccf) {
                commanderColorFilter = ccf;
                continue;
            }
            if (filter instanceof ConquestCommandersScreen.CommanderOriginFilter cof) {
                commanderOriginFilter = cof;
                continue;
            }
            helper.include(filter.getWidget(), filter.getPreferredWidth(helper.getRemainingLineWidth(), fieldHeight), fieldHeight);
        }
        if (deckColorFilter != null) {
            helper.fillLine(deckColorFilter.getWidget(), fieldHeight);
        }
        if (deckFormatFilter != null) {
            helper.fillLine(deckFormatFilter.getWidget(), fieldHeight);
        }
        if (colorFilter != null)
            helper.fillLine(colorFilter.getWidget(), fieldHeight);
        if (cardTypeFilter != null)
            helper.fillLine(cardTypeFilter.getWidget(), fieldHeight);
        if (commanderColorFilter != null)
            helper.fillLine(commanderColorFilter.getWidget(), fieldHeight);
        if (commanderOriginFilter != null)
            helper.fillLine(commanderOriginFilter.getWidget(), fieldHeight);
        helper.newLine();
        if (cardFormatFilter != null)
            helper.include(cardFormatFilter.getWidget(), width / 2f, fieldHeight);
        if (allowSortChange()) {
            helper.fillLine(cbxSortOptions, fieldHeight);
        }
        helper.newLine(-ItemFilter.PADDING);
        if (currentView.getPnlOptions().getChildCount() > 0) {
            helper.fillLine(currentView.getPnlOptions(), fieldHeight + ItemFilter.PADDING);
        } else {
            helper.offset(0, -fieldHeight); //prevent showing whitespace for empty view options panel
        }
    }

    public Class<T> getGenericType() {
        return genericType;
    }

    public String getCaption() {
        return searchFilter.getCaption();
    }

    public void setCaption(String caption0) {
        searchFilter.setCaption(caption0);
    }

    public ItemPool<T> getPool() {
        return pool;
    }

    public void setPool(final Iterable<T> items) {
        setPool(ItemPool.createFrom(items, genericType), false);
    }

    public void setPool(final ItemPool<T> pool0) {
        setPool(pool0, false);
    }

    public void setPool(final ItemPool<T> pool0, boolean infinite) {
        pool = pool0;
        model.clear();
        model.addItems(pool);
        model.setInfinite(infinite);
        updateView(true, null);
    }

    public ItemView<T> getCurrentView() {
        return currentView;
    }

    public int getItemCount() {
        return currentView.getCount();
    }

    public int getSelectionCount() {
        return currentView.getSelectionCount();
    }

    public T getSelectedItem() {
        return currentView.getSelectedItem();
    }

    public Collection<T> getSelectedItems() {
        return currentView.getSelectedItems();
    }

    public ItemPool<T> getSelectedItemPool() {
        ItemPool<T> selectedItemPool = new ItemPool<>(genericType);
        if (currentView == listView) {
            for (T item : getSelectedItems()) {
                selectedItemPool.add(item, getItemCount(item));
            }
        } else { //just add all flat for image view
            selectedItemPool.addAllFlat(getSelectedItems());
        }
        return selectedItemPool;
    }

    public boolean setSelectedItem(T item) {
        return currentView.setSelectedItem(item);
    }

    public boolean setSelectedItems(Iterable<T> items) {
        return currentView.setSelectedItems(items);
    }

    public T stringToItem(String str) {
        for (Entry<T, Integer> itemEntry : pool) {
            if (itemEntry.getKey().toString().equals(str)) {
                return itemEntry.getKey();
            }
        }
        return null;
    }

    public boolean setSelectedString(String str) {
        T item = stringToItem(str);
        if (item != null) {
            return setSelectedItem(item);
        }
        return false;
    }

    public boolean setSelectedStrings(Iterable<String> strings) {
        List<T> items = new ArrayList<>();
        for (String str : strings) {
            T item = stringToItem(str);
            if (item != null) {
                items.add(item);
            }
        }
        return setSelectedItems(items);
    }

    public boolean selectItemEntrys(Iterable<Entry<T, Integer>> itemEntrys) {
        List<T> items = new ArrayList<>();
        for (Entry<T, Integer> itemEntry : itemEntrys) {
            items.add(itemEntry.getKey());
        }
        return setSelectedItems(items);
    }

    public void selectAll() {
        currentView.selectAll();
    }

    public int getSelectedIndex() {
        return currentView.getSelectedIndex();
    }

    public Iterable<Integer> getSelectedIndices() {
        return currentView.getSelectedIndices();
    }

    public void setSelectedIndex(int index) {
        currentView.setSelectedIndex(index);
    }

    public void setSelectedIndices(Integer[] indices) {
        currentView.setSelectedIndices(Arrays.asList(indices));
    }

    public void setSelectedIndices(Iterable<Integer> indices) {
        currentView.setSelectedIndices(indices);
    }

    public void setSelectedIndexRelative(int indexOffset) {
        int current = getSelectedIndex();
        int size = getItemCount();
        if(size == 0)
            return;
        //Desired behavior: if we're on item 8 out of 10, and we move the selection by 5, stop at item 10 first.
        //A second input will wrap the selection around to item 1 again.
        if(current <= 0 && indexOffset < 0)
            setSelectedIndex(size - 1);
        else if(current >= size - 1 && indexOffset > 0)
            setSelectedIndex(0);
        else
            setSelectedIndex(Math.max(0, Math.min(current + indexOffset, size - 1)));
    }

    public void addItem(final T item, int qty) {
        if (pool == null) {
            return;
        }
        try {
            pool.add(item, qty);
            if (isUnfiltered()) {
                model.addItem(item, qty);
            }
            List<T> items = new ArrayList<>();
            items.add(item);
            updateView(false, items);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addItems(Iterable<Entry<T, Integer>> itemsToAdd) {
        pool.addAll(itemsToAdd);
        if (isUnfiltered()) {
            model.addItems(itemsToAdd);
        }

        List<T> items = new ArrayList<>();
        for (Map.Entry<T, Integer> item : itemsToAdd) {
            items.add(item.getKey());
        }
        updateView(false, items);
    }

    public void addItemsFlat(Iterable<T> itemsToAdd) {
        pool.addAllFlat(itemsToAdd);
        if (isUnfiltered()) {
            for (T item : itemsToAdd) {
                model.addItem(item, 1);
            }
        }
        updateView(false, itemsToAdd);
    }

    public void setItems(Iterable<Entry<T, Integer>> items) {
        pool.clear();
        model.clear();
        addItems(items);
    }

    public void removeItem(final T item, int qty) {
        final Iterable<T> itemsToSelect = currentView == listView ? getSelectedItems() : null;

        pool.remove(item, qty);
        if (isUnfiltered()) {
            model.removeItem(item, qty);
        }
        updateView(false, itemsToSelect);
    }

    public void removeItems(Iterable<Map.Entry<T, Integer>> itemsToRemove) {
        final Iterable<T> itemsToSelect = currentView == listView ? getSelectedItems() : null;

        for (Map.Entry<T, Integer> item : itemsToRemove) {
            pool.remove(item.getKey(), item.getValue());
            if (isUnfiltered()) {
                model.removeItem(item.getKey(), item.getValue());
            }
        }
        updateView(false, itemsToSelect);
    }

    public void removeItemsFlat(Iterable<T> itemsToRemove) {
        final Iterable<T> itemsToSelect = currentView == listView ? getSelectedItems() : null;

        pool.removeAllFlat(itemsToRemove);
        if (isUnfiltered()) {
            for (T item : itemsToRemove) {
                model.removeItem(item, 1);
            }
        }
        updateView(false, itemsToSelect);
    }

    public void removeAllItems() {
        pool.clear();
        model.clear();
        updateView(false, null);
    }

    public void replaceAll(final T item, final T replacement) {
        int count = pool.count(item);
        if (count == 0) { return; }

        final Iterable<T> itemsToSelect = currentView == listView ? getSelectedItems() : null;

        pool.removeAll(item);
        pool.add(replacement, count);
        if (isUnfiltered()) {
            model.replaceAll(item, replacement);
        }
        updateView(false, itemsToSelect);
    }

    public void setBtnAdvancedSearchOptions(boolean enable) {
        btnAdvancedSearchOptions.setEnabled(enable);
    }

    public void scrollSelectionIntoView() {
        currentView.scrollSelectionIntoView();
    }

    public int getItemCount(final T item) {
        return model.isInfinite() ? Integer.MAX_VALUE : pool.count(item);
    }

    public ItemPool<T> getFilteredItems() {
        return model.getItems();
    }

    protected abstract void addDefaultFilters();

    protected abstract TextSearchFilter<? extends T> createSearchFilter();

    protected abstract AdvancedSearchFilter<? extends T> createAdvancedSearchFilter();

    protected Iterable<Entry<T, Integer>> getUnique(final Iterable<Entry<T, Integer>> items) {
        return Aggregates.uniqueByLast(items, from -> from.getKey().getName());
    }

    public void addFilter(final ItemFilter<? extends T> filter) {
        filters.get().add(filter);
        add(filter.getWidget());

        boolean visible = !hideFilters;
        filter.getWidget().setVisible(visible);
        if (visible && initialized) {
            revalidate();
            applyNewOrModifiedFilter(filter);
        }
    }

    //apply filters and focus existing filter's main component if filtering not locked
    public void applyNewOrModifiedFilter(final ItemFilter<? extends T> filter) {
        if (lockFiltering) {
            return;
        }

        if (filter == advancedSearchFilter) {
            //handle update the visibility of the advanced search filter
            boolean empty = filter.isEmpty();
            ItemFilter<? extends T>.Widget widget = filter.getWidget();
            if (widget.isVisible() == empty) {
                widget.setVisible(!empty);
                revalidate();
            }
        }

        applyFilters();
    }

    public void restoreDefaultFilters() {
        lockFiltering = true;
        for (ItemFilter<? extends T> filter : filters.get()) {
            remove(filter.getWidget());
        }
        filters.get().clear();
        addDefaultFilters();
        lockFiltering = false;
        revalidate();
        applyFilters();
    }

    public void resetFilters() {
        lockFiltering = true; //prevent updating filtering from this change until all filters reset
        for (final ItemFilter<? extends T> filter : filters.get()) {
            filter.reset();
        }
        searchFilter.reset();
        if (advancedSearchFilter != null) {
            advancedSearchFilter.reset();
            ItemFilter<? extends T>.Widget widget = advancedSearchFilter.getWidget();
            if (widget.isVisible()) {
                widget.setVisible(false);
                revalidate();
            }
        }
        lockFiltering = false;

        applyFilters();
    }

    public void removeFilter(ItemFilter<? extends T> filter) {
        filters.get().remove(filter);
        remove(filter.getWidget());
        revalidate();
        applyFilters();
    }

    public boolean applyFilters() {
        if (lockFiltering || !initialized) {
            return false;
        }

        List<Predicate<? super T>> predicates = new ArrayList<>();
        for (ItemFilter<? extends T> filter : filters.get()) {
            if (!filter.isEmpty()) {
                predicates.add(filter.buildPredicate(genericType));
            }
        }
        if (!searchFilter.isEmpty()) {
            predicates.add(searchFilter.buildPredicate(genericType));
        }
        if (advancedSearchFilter != null && !advancedSearchFilter.isEmpty()) {
            predicates.add(advancedSearchFilter.buildPredicate(genericType));
        }

        Predicate<? super T> newFilterPredicate = predicates.isEmpty() ? null : IterableUtil.<T>and(predicates);
        if (filterPredicate == newFilterPredicate) {
            return false;
        }

        filterPredicate = newFilterPredicate;
        if (pool != null) {
            if (viewUpdating) {
                needSecondUpdate = true;
            } else {
                viewUpdating = true;
                FThreads.invokeInBackgroundThread(() -> {
                    do {
                        needSecondUpdate = false;
                        updateView(true, null);
                        Gdx.graphics.requestRendering();
                    } while (needSecondUpdate);
                    viewUpdating = false;
                });
            }
        }
        return true;
    }

    private boolean isUnfiltered() {
        return filterPredicate == null;
    }

    public boolean getHideFilters() {
        return hideFilters;
    }

    public void setHideFilters(boolean hideFilters0) {
        if (hideFilters == hideFilters0) {
            return;
        }
        hideFilters = hideFilters0;

        boolean visible = !hideFilters0;
        for (ItemFilter<? extends T> filter : filters.get()) {
            filter.getWidget().setVisible(visible);
        }
        if (allowSortChange()) {
            cbxSortOptions.setVisible(visible);
        }
        for (ItemView<T> view : views) {
            view.getPnlOptions().setVisible(visible);
        }

        if (initialized) {
            btnAdvancedSearchOptions.setSelected(visible);

            revalidate();

            if (config != null) {
                config.setHideFilters(hideFilters0);
            }
        }
    }

    public void applyAdvancedSearchFilter(String filterString) {
        applyAdvancedSearchFilter(new String[]{filterString}, false);
    }

    /**
     * Programmatic method to set this ItemManager's advanced search filter value.
     * Other filters will be cleared.
     */
    public void applyAdvancedSearchFilter(String[] filterStrings, boolean joinAnd) {
        if (advancedSearchFilter == null) {
            advancedSearchFilter = createAdvancedSearchFilter();
            ItemManager.this.add(advancedSearchFilter.getWidget());
        }
        lockFiltering = true;
        for (final ItemFilter<? extends T> filter : filters.get()) {
            filter.reset();
        }
        searchFilter.reset();
        advancedSearchFilter.reset();
        advancedSearchFilter.setFilterParts(filterStrings, joinAnd);
        lockFiltering = false;

        applyFilters();
        advancedSearchFilter.refreshWidget();
        revalidate();
    }

    //Refresh displayed items
    public void refresh() {
        updateView(true, getSelectedItems());
    }

    public void updateView(final boolean forceFilter, final Iterable<T> itemsToSelect) {
        //TO-maybe-DO: Share logic between this and identical method in desktop.
        final boolean useFilter = (forceFilter && (filterPredicate != null)) || !isUnfiltered();

        if (useFilter || this.wantUnique || forceFilter) {
            this.model.clear();
        }

        if (useFilter && this.wantUnique) {
            final Predicate<Entry<T, Integer>> filterForPool = x -> this.filterPredicate.test(x.getKey());
            final Iterable<Entry<T, Integer>> items = getUnique(IterableUtil.filter(this.pool, filterForPool));
            this.model.addItems(items);
        }
        else if (useFilter) {
            final Predicate<Entry<T, Integer>> pred = x -> this.filterPredicate.test(x.getKey());
            this.model.addItems(IterableUtil.filter(this.pool, pred));
        }
        else if (this.wantUnique) {
            final Iterable<Entry<T, Integer>> items = getUnique(this.pool);
            this.model.addItems(items);
        }
        else if (forceFilter) {
            this.model.addItems(this.pool);
        }
        currentView.refresh(itemsToSelect, getSelectedIndex(), forceFilter ? 0 : currentView.getScrollValue());

        //update ratio of # in filtered pool / # in total pool
        ItemPool<T> filteredItems = getFilteredItems();
        int filteredCount = filteredItems.countAll();
        int totalCount = useFilter ? pool.countAll() : filteredCount;

        searchFilter.setRatio("(" + filteredCount + " / " + totalCount + ")");
    }

    public boolean isIncrementalSearchActive() {
        return currentView.isIncrementalSearchActive();
    }

    public boolean getWantUnique() {
        return wantUnique;
    }

    public boolean getShowRanking() {
        return showRanking;
    }

    public boolean showPriceInfo() {
        ItemColumn currentSort = cbxSortOptions.getSelectedItem();
        if(currentSort != null && currentSort.getConfig().getDef() == ColumnDef.PRICE)
            return true;
        return showPriceInfo;
    }

    public void setWantUnique(boolean unique) {
        wantUnique = unique;
    }

    public void setShowRanking(boolean showRanking0) {
        showRanking = showRanking0;
    }

    public void setShowPriceInfo(boolean val) {
        showPriceInfo = val;
    }

    public void setSelectionSupport(int minSelections0, int maxSelections0) {
        for (ItemView<T> view : views) {
            view.setSelectionSupport(minSelections0, maxSelections0);
        }
    }

    public boolean getMultiSelectMode() {
        return multiSelectMode;
    }

    public void toggleMultiSelectMode(int indexToSelect) {
        multiSelectMode = !multiSelectMode;
        if (multiSelectMode) {
            setSelectionSupport(0, Integer.MAX_VALUE);
        } else {
            setSelectionSupport(0, 1);
        }
        if (isContextMenuOpen()) {
            contextMenu.hide(); //ensure context menu hidden
        }
        if (indexToSelect != -1) {
            setSelectedIndex(indexToSelect);
        }
    }

    //whether item manager's pool of items is in infinite supply
    public boolean isInfinite() {
        return model.isInfinite();
    }

    public void focusSearch() {
        setHideFilters(false); //ensure filters shown
    }

    public FEventHandler getSelectionChangedHandler() {
        return selectionChangedHandler;
    }

    public void setSelectionChangedHandler(FEventHandler selectionChangedHandler0) {
        selectionChangedHandler = selectionChangedHandler0;
    }

    public void setItemActivateHandler(FEventHandler itemActivateHandler0) {
        itemActivateHandler = itemActivateHandler0;
    }

    public void activateSelectedItems() {
        if (itemActivateHandler != null) {
            itemActivateHandler.handleEvent(new FEvent(this, FEventType.ACTIVATE));
        }
    }

    public void setContextMenuBuilder(ContextMenuBuilder<T> contextMenuBuilder0) {
        contextMenuBuilder = contextMenuBuilder0;
    }

    public void showMenu(boolean delay) {
        showMenu(delay, 0f, 0f);
    }

    /**
     * Generic object that can be deleted to abort a delayed showMenu invocation.
     */
    private Object menuDelayCancel = null;
    public void showMenu(boolean delay, float left, float width) {
        if (contextMenuBuilder != null && getSelectionCount() > 0) {
            itemLeft = left;
            itemWidth = width;
            if (contextMenu == null) {
                contextMenu = new ContextMenu();
            }
            if (delay) { //delay showing menu to prevent it hiding right away
                final Object delayObj = new Object();
                this.menuDelayCancel = delayObj;
                FThreads.delayInEDT(50, () -> {
                    if(menuDelayCancel != delayObj)
                        return;
                    contextMenu.show();
                    Gdx.graphics.requestRendering();
                });
            } else {
                contextMenu.show();
            }
        }
    }

    public void closeMenu() {
        if (isContextMenuOpen())
            contextMenu.hide();
        menuDelayCancel = null;
    }

    public boolean isContextMenuOpen() {
        return contextMenu != null && contextMenu.isVisible();
    }

    public void selectNextContext() {
        if (contextMenu != null) {
            contextMenu.setNextSelected();
        }
    }

    public void selectPreviousContext() {
        if (contextMenu != null) {
            contextMenu.setPreviousSelected();
        }
    }

    public void activateSelectedContext() {
        if (contextMenu != null)
            contextMenu.tapChild();
    }

    public static abstract class ContextMenuBuilder<T> {
        public abstract void buildMenu(final FDropDownMenu menu, final T item);
    }

    private class ContextMenu extends FDropDownMenu {
        @Override
        protected void buildMenu() {
            if(getSelectedItem() == null)
                return;
            contextMenuBuilder.buildMenu(this, getSelectedItem());
        }

        @Override
        protected boolean hideBackdropOnPress(float x, float y) {
            Rectangle bounds = currentView.getSelectionBounds();
            return bounds != null && !bounds.contains(x, y); //don't hide on press if within selection bounds
        }

        @Override
        protected boolean preventOwnerHandlingBackupTap(float x, float y, int count) {
            //prevent view handling single tap, but allow it to handle double tap
            return count == 1;
        }

        @Override
        protected void updateSizeAndPosition() {
            FScreen screen = Forge.getCurrentScreen();
            float screenWidth = screen.getWidth();
            float screenHeight = screen.getHeight();

            Rectangle scrollerBounds = currentView.getScroller().screenPos;;

            paneSize = updateAndGetPaneSize(screenWidth, screenHeight);
            float w = paneSize.getWidth();
            float h = paneSize.getHeight();

            try {
                Rectangle bounds = currentView.getSelectionBounds();

                //try displaying right of selection if possible
                float x = bounds.x + bounds.width;
                float y = bounds.y;

                if(x < scrollerBounds.x)
                    x = scrollerBounds.x;
                if(y < scrollerBounds.y)
                    y = scrollerBounds.y;

                boolean tooNarrow = false;

                if (x + w > screenWidth) {
                    //try displaying left of selection if possible
                    x = bounds.x - w;
                    if (x < 0) {
                        //display below selection if no room left or right of selection
                        x = bounds.x;
                        if (w < bounds.width) {
                            //center below item if needed
                            x += (bounds.width - w) / 2;
                        }
                        if (x + w > screenWidth) {
                            x = screenWidth - w;
                        }
                        y += bounds.height;
                        tooNarrow = true;
                    }
                }
                if (y + h > screenHeight) {
                    if (tooNarrow) {
                        //if displaying to left or right, move up if not enough room
                        y = screenHeight - h;
                    } else {
                        //if displaying below selection and not enough room, display above selection
                        y -= bounds.height + h;
                    }
                    if (y < 0) {
                        y = 0;
                        if (h > bounds.y) {
                            h = bounds.y; //cut off menu if not enough room above or below selection
                        }
                    }
                }
                if (Forge.isLandscapeMode() && getSelectedItem() != null) {
                    if (instance instanceof SpellShopManager) {
                        if (instance.currentView == imageView) {
                            x = instance.itemLeft + instance.itemWidth / 2 - this.getWidth() / 2;
                        }
                    }
                }
                setBounds(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
            } catch (Exception ignored) {
                //FIXME: IndexOutOfBoundsException on Rectangle bounds = currentView.getSelectionBounds();
            }
        }
    }

    @Override
    public String getActivateAction(int index) {
        if (contextMenuBuilder != null) {
            return Forge.getLocalizer().getMessage("lblSelectCard");
        }
        return null;
    }

    @Override
    public void activate(int index) {
        setSelectedIndex(index);
        showMenu(true);
    }

    public float getPileByWidth() {
        if (filters.get().isEmpty()) {
            return 0f;
        }
        float preferredSize = filters.get().get(filters.get().size() - 1).getWidget().getWidth();
        if (cbxSortOptions != null && Math.abs(1 - (cbxSortOptions.getWidth() / preferredSize)) < 0.25) {
            //Match the size of the sort box if it's not too far off from what we'd prefer.
            return cbxSortOptions.getWidth();
        }
        return preferredSize;
    }

    @Override
    public boolean keyDown(int keyCode) {
        if(isContextMenuOpen()) {
            switch (keyCode) {
                case Input.Keys.DPAD_UP:
                    selectPreviousContext();
                    return true;
                case Input.Keys.DPAD_DOWN:
                    selectNextContext();
                    return true;
                case Input.Keys.BUTTON_A:
                    activateSelectedContext();
                    return true;
                case Input.Keys.BUTTON_B:
                    closeMenu();
                    return true;
                case Input.Keys.BUTTON_Y:
                case Input.Keys.BUTTON_L1:
                    closeMenu();
                    break;
                default:
                    return false;
            }
        }

        boolean usingListView = currentView == listView;
        switch(keyCode) {
            case Input.Keys.DPAD_RIGHT:
            case Input.Keys.PAGE_DOWN:
                setSelectedIndexRelative(usingListView ? 10 : 1);
                return true;
            case Input.Keys.DPAD_LEFT:
            case Input.Keys.PAGE_UP:
                setSelectedIndexRelative(usingListView ? -10 : -1);
                return true;
            case Input.Keys.DPAD_DOWN:
                setSelectedIndexRelative(usingListView ? 1 : getConfig().getImageColumnCount());
                return true;
            case Input.Keys.DPAD_UP:
                setSelectedIndexRelative(usingListView ? -1 : -getConfig().getImageColumnCount());
                return true;
            case Input.Keys.BUTTON_A:
                showMenu(true);
                return true;
            case Input.Keys.BUTTON_Y:
                if(getCurrentView().getSelectionCount() > 0) {
                    getCurrentView().zoomSelected();
                    return true;
                }
                break;
            case Input.Keys.BUTTON_L1:
                setViewIndex(config.getViewIndex() == 1 ? 0 : 1);
                return true;
        }

        return false;
    }
}
