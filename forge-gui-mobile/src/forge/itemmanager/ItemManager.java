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

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Rectangle;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.item.InventoryItem;
import forge.itemmanager.filters.ItemFilter;
import forge.itemmanager.filters.TextSearchFilter;
import forge.itemmanager.views.ImageView;
import forge.itemmanager.views.ItemListView;
import forge.itemmanager.views.ItemView;
import forge.menu.FDropDownMenu;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FEvent.FEventType;
import forge.toolbox.FLabel;
import forge.util.Aggregates;
import forge.util.ItemPool;
import forge.util.LayoutHelper;
import forge.util.ReflectionUtil;

import java.util.*;
import java.util.Map.Entry;


public abstract class ItemManager<T extends InventoryItem> extends FContainer implements IItemManager<T> {
    private ItemPool<T> pool;
    private final ItemManagerModel<T> model;
    private Predicate<? super T> filterPredicate = null;
    private final Map<Class<? extends ItemFilter<? extends T>>, List<ItemFilter<? extends T>>> filters =
            new HashMap<Class<? extends ItemFilter<? extends T>>, List<ItemFilter<? extends T>>>();
    private final List<ItemFilter<? extends T>> orderedFilters = new ArrayList<ItemFilter<? extends T>>();
    private boolean wantUnique = false;
    private boolean alwaysNonUnique = false;
    private boolean hideFilters = false;
    private FEventHandler selectionChangedHandler, itemActivateHandler;
    private ContextMenuBuilder<T> contextMenuBuilder;
    private ContextMenu contextMenu;
    private final Class<T> genericType;
    private ItemManagerConfig config;
    private boolean hasNewColumn;

    private final TextSearchFilter<? extends T> searchFilter;

    private static final FSkinImage VIEW_OPTIONS_ICON = FSkinImage.SETTINGS;
    private final FLabel btnAdvancedSearchOptions = new FLabel.Builder()
        .selectable(true).align(HAlignment.CENTER)
        .icon(VIEW_OPTIONS_ICON).iconScaleFactor(0.9f)
        .build();

    private final List<ItemView<T>> views = new ArrayList<ItemView<T>>();
    private final ItemListView<T> listView;
    private final ImageView<T> imageView;
    private ItemView<T> currentView;
    private boolean initialized;
    protected boolean lockFiltering;

    /**
     * ItemManager Constructor.
     * 
     * @param genericType0 the class of item that this table will contain
     * @param statLabels0 stat labels for this item manager
     * @param wantUnique0 whether this table should display only one item with the same name
     */
    protected ItemManager(final Class<T> genericType0, final boolean wantUnique0) {
        genericType = genericType0;
        wantUnique = wantUnique0;
        model = new ItemManagerModel<T>(genericType0);

        searchFilter = createSearchFilter();

        listView = new ItemListView<T>(this, model);
        imageView = createImageView(model);

        views.add(listView);
        views.add(imageView);
        currentView = listView;

        //initialize views
        for (int i = 0; i < views.size(); i++) {
            views.get(i).initialize(i);
        }

        //build display
        add(searchFilter.getWidget());
        for (ItemView<T> view : views) {
            add(view.getButton());
            view.getButton().setSelected(view == currentView);
        }
        add(btnAdvancedSearchOptions);
        btnAdvancedSearchOptions.setSelected(!hideFilters);
        add(currentView.getPnlOptions());
        add(currentView.getScroller());

        btnAdvancedSearchOptions.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                setHideFilters(!hideFilters);
            }
        });

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
        return new ImageView<T>(this, model0);
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
        for (ItemView<T> view : views) {
            view.setup(config0, colOverrides);
        }
        setViewIndex(config0.getViewIndex());
        setHideFilters(config0.getHideFilters());
        hasNewColumn = config.getCols().containsKey(ColumnDef.NEW);
    }

    protected String getItemSuffix(Entry<T, Integer> item) {
        return hasNewColumn && FModel.getQuest().getCards().isNew(item.getKey()) ? " *NEW*" : null;
    }

    public abstract class ItemRenderer {
        public abstract float getItemHeight();
        public abstract boolean tap(Entry<T, Integer> value, float x, float y, int count);
        public abstract boolean longPress(Entry<T, Integer> value, float x, float y);
        public abstract void drawValue(Graphics g, Entry<T, Integer> value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h);
    }
    public abstract ItemRenderer getListItemRenderer();

    public void setViewIndex(int viewIndex) {
        if (viewIndex < 0 || viewIndex >= views.size()) { return; }
        ItemView<T> view = views.get(viewIndex);
        if (currentView == view) { return; }

        if (config != null) {
            config.setViewIndex(viewIndex);
        }

        final int backupIndexToSelect = currentView.getSelectedIndex();
        final Iterable<T> itemsToSelect; //only retain selected items if not single selection of first item
        if (backupIndexToSelect > 0 || currentView.getSelectionCount() > 1) {
            itemsToSelect = currentView.getSelectedItems();
        }
        else {
            itemsToSelect = null;
        }

        currentView.getButton().setSelected(false);
        remove(currentView.getPnlOptions());
        remove(currentView.getScroller());

        currentView = view;

        view.getButton().setSelected(true);
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
        float viewButtonCount = views.size() + 1;
        helper.offset(0, ItemFilter.PADDING);
        helper.fillLine(searchFilter.getWidget(), fieldHeight, (viewButtonWidth + helper.getGapX()) * viewButtonCount); //leave room for view buttons
        for (ItemView<T> view : views) {
            helper.include(view.getButton(), viewButtonWidth, fieldHeight);
        }
        helper.include(btnAdvancedSearchOptions, viewButtonWidth, fieldHeight);
        helper.newLine();
        if (!hideFilters) {
            for (ItemFilter<? extends T> filter : orderedFilters) {
                helper.include(filter.getWidget(), filter.getPreferredWidth(helper.getRemainingLineWidth(), fieldHeight), fieldHeight);
            }
            helper.newLine(-ItemFilter.PADDING);
            if (currentView.getPnlOptions().getChildCount() > 0) {
                helper.fillLine(currentView.getPnlOptions(), fieldHeight + ItemFilter.PADDING);
            }
            else {
                helper.offset(0, -fieldHeight); //prevent showing whitespace for empty view options panel
            }
        }
        helper.fill(currentView.getScroller());
    }

    /**
     * 
     * getGenericType.
     * 
     * @return generic type of items
     */
    public Class<T> getGenericType() {
        return genericType;
    }

    /**
     * 
     * getCaption.
     * 
     * @return caption to display before ratio
     */
    public String getCaption() {
        return searchFilter.getCaption();
    }

    /**
     * 
     * setCaption.
     * 
     * @param caption - caption to display before ratio
     */
    public void setCaption(String caption0) {
        searchFilter.setCaption(caption0);
    }

    /**
     * 
     * Gets the item pool.
     * 
     * @return ItemPoolView
     */
    public ItemPool<T> getPool() {
        return pool;
    }

    /**
     * 
     * Sets the item pool.
     * 
     * @param items
     */
    public void setPool(final Iterable<T> items) {
        setPool(ItemPool.createFrom(items, genericType), false);
    }

    /**
     * 
     * Sets the item pool.
     * 
     * @param poolView
     * @param infinite
     */
    public void setPool(final ItemPool<T> poolView, boolean infinite) {
        setPoolImpl(ItemPool.createFrom(poolView, genericType), infinite);
    }

    public void setPool(final ItemPool<T> pool0) {
        setPoolImpl(pool0, false);
    }

    /**
     * 
     * Sets the item pool.
     * 
     * @param pool0
     * @param infinite
     */
    private void setPoolImpl(final ItemPool<T> pool0, boolean infinite) {
        model.clear();
        pool = pool0;
        model.addItems(pool);
        model.setInfinite(infinite);
        updateView(true, null);
    }

    public ItemView<T> getCurrentView() {
        return currentView;
    }

    /**
     * 
     * getItemCount.
     * 
     * @return int
     */
    public int getItemCount() {
        return currentView.getCount();
    }

    /**
     * 
     * getSelectionCount.
     * 
     * @return int
     */
    public int getSelectionCount() {
        return currentView.getSelectionCount();
    }

    /**
     * 
     * getSelectedItem.
     * 
     * @return T
     */
    public T getSelectedItem() {
        return currentView.getSelectedItem();
    }

    /**
     * 
     * getSelectedItems.
     * 
     * @return Iterable<T>
     */
    public Collection<T> getSelectedItems() {
        return currentView.getSelectedItems();
    }

    /**
     * 
     * getSelectedItems.
     * 
     * @return ItemPool<T>
     */
    public ItemPool<T> getSelectedItemPool() {
        ItemPool<T> selectedItemPool = new ItemPool<T>(genericType);
        for (T item : getSelectedItems()) {
            selectedItemPool.add(item, getItemCount(item));
        }
        return selectedItemPool;
    }

    /**
     * 
     * setSelectedItem.
     * 
     * @param item - Item to select
     */
    public boolean setSelectedItem(T item) {
    	return currentView.setSelectedItem(item);
    }

    /**
     * 
     * setSelectedItems.
     * 
     * @param items - Items to select
     */
    public boolean setSelectedItems(Iterable<T> items) {
        return currentView.setSelectedItems(items);
    }

    /**
     * 
     * stringToItem.
     * 
     * @param str - String to get item corresponding to
     */
    public T stringToItem(String str) {
        for (Entry<T, Integer> itemEntry : pool) {
            if (itemEntry.getKey().toString().equals(str)) {
                return itemEntry.getKey();
            }
        }
        return null;
    }

    /**
     * 
     * setSelectedString.
     * 
     * @param str - String to select
     */
    public boolean setSelectedString(String str) {
        T item = stringToItem(str);
        if (item != null) {
            return setSelectedItem(item);
        }
        return false;
    }

    /**
     * 
     * setSelectedStrings.
     * 
     * @param strings - Strings to select
     */
    public boolean setSelectedStrings(Iterable<String> strings) {
        List<T> items = new ArrayList<T>();
        for (String str : strings) {
            T item = stringToItem(str);
            if (item != null) {
                items.add(item);
            }
        }
        return setSelectedItems(items);
    }

    /**
     * 
     * selectItemEntrys.
     * 
     * @param itemEntrys - Item entrys to select
     */
    public boolean selectItemEntrys(Iterable<Entry<T, Integer>> itemEntrys) {
        List<T> items = new ArrayList<T>();
        for (Entry<T, Integer> itemEntry : itemEntrys) {
            items.add(itemEntry.getKey());
        }
        return setSelectedItems(items);
    }

    /**
     * 
     * selectAll.
     * 
     */
    public void selectAll() {
        currentView.selectAll();
    }

    /**
     * 
     * getSelectedItem.
     * 
     * @return T
     */
    public int getSelectedIndex() {
        return currentView.getSelectedIndex();
    }

    /**
     * 
     * getSelectedItems.
     * 
     * @return Iterable<Integer>
     */
    public Iterable<Integer> getSelectedIndices() {
        return currentView.getSelectedIndices();
    }

    /**
     * 
     * setSelectedIndex.
     * 
     * @param index - Index to select
     */
    public void setSelectedIndex(int index) {
        currentView.setSelectedIndex(index);
    }

    /**
     * 
     * setSelectedIndices.
     * 
     * @param indices - Indices to select
     */
    public void setSelectedIndices(Integer[] indices) {
        currentView.setSelectedIndices(Arrays.asList(indices));
    }
    public void setSelectedIndices(Iterable<Integer> indices) {
        currentView.setSelectedIndices(indices);
    }

    /**
     * 
     * addItem.
     * 
     * @param item
     * @param qty
     */
    public void addItem(final T item, int qty) {
        pool.add(item, qty);
        if (isUnfiltered()) {
            model.addItem(item, qty);
        }
        List<T> items = new ArrayList<T>();
        items.add(item);
        updateView(false, items);
    }

    /**
     * 
     * addItems.
     * 
     * @param itemsToAdd
     */
    public void addItems(Iterable<Entry<T, Integer>> itemsToAdd) {
        pool.addAll(itemsToAdd);
        if (isUnfiltered()) {
            model.addItems(itemsToAdd);
        }

        List<T> items = new ArrayList<T>();
        for (Map.Entry<T, Integer> item : itemsToAdd) {
            items.add(item.getKey());
        }
        updateView(false, items);
    }

    /**
     * 
     * removeItem.
     * 
     * @param item
     * @param qty
     */
    public void removeItem(final T item, int qty) {
        final Iterable<T> itemsToSelect = currentView == listView ? getSelectedItems() : null;

        pool.remove(item, qty);
        if (isUnfiltered()) {
            model.removeItem(item, qty);
        }
        updateView(false, itemsToSelect);
    }

    /**
     * 
     * removeItems.
     * 
     * @param itemsToRemove
     */
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

    /**
     * 
     * removeAllItems.
     * 
     */
    public void removeAllItems() {
        pool.clear();
        model.clear();
        updateView(false, null);
    }

    /**
     * 
     * scrollSelectionIntoView.
     * 
     */
    public void scrollSelectionIntoView() {
        currentView.scrollSelectionIntoView();
    }

    /**
     * 
     * getItemCount.
     * 
     * @param item
     */
    public int getItemCount(final T item) {
        return model.isInfinite() ? Integer.MAX_VALUE : pool.count(item);
    }

    /**
     * Gets all filtered items in the model.
     * 
     * @return ItemPoolView<T>
     */
    public ItemPool<T> getFilteredItems() {
        return model.getItems();
    }

    protected abstract void addDefaultFilters();
    protected abstract TextSearchFilter<? extends T> createSearchFilter();

    protected <F extends ItemFilter<? extends T>> F getFilter(Class<F> filterClass) {
        return ReflectionUtil.safeCast(filters.get(filterClass), filterClass);
    }

    @SuppressWarnings("unchecked")
    public void addFilter(final ItemFilter<? extends T> filter) {
        final Class<? extends ItemFilter<? extends T>> filterClass = (Class<? extends ItemFilter<? extends T>>) filter.getClass();
        List<ItemFilter<? extends T>> classFilters = filters.get(filterClass);
        if (classFilters == null) {
            classFilters = new ArrayList<ItemFilter<? extends T>>();
            filters.put(filterClass, classFilters);
        }
        if (classFilters.size() > 0) {
            //if filter with the same class already exists, try to merge if allowed
            //NOTE: can always use first filter for these checks since if
            //merge is supported, only one will ever exist
            final ItemFilter<? extends T> existingFilter = classFilters.get(0);
            if (existingFilter.merge(filter)) {
                //if new filter merged with existing filter, just refresh the widget
                existingFilter.refreshWidget();
                applyNewOrModifiedFilter(existingFilter);
                return;
            }
        }
        classFilters.add(filter);
        orderedFilters.add(filter);
        add(filter.getWidget());

        boolean visible = !hideFilters;
        filter.getWidget().setVisible(visible);
        if (visible && initialized) {
            revalidate();
            applyNewOrModifiedFilter(filter);
        }
    }

    //apply filters and focus existing filter's main component if filtering not locked
    private void applyNewOrModifiedFilter(final ItemFilter<? extends T> filter) {
        if (lockFiltering) { return; }
        applyFilters();
    }

    public void restoreDefaultFilters() {
        lockFiltering = true;
        for (ItemFilter<? extends T> filter : orderedFilters) {
            remove(filter.getWidget());
        }
        filters.clear();
        orderedFilters.clear();
        addDefaultFilters();
        lockFiltering = false;
        revalidate();
        applyFilters();
    }

    @SuppressWarnings("unchecked")
    public void removeFilter(ItemFilter<? extends T> filter) {
        final Class<? extends ItemFilter<? extends T>> filterClass = (Class<? extends ItemFilter<? extends T>>) filter.getClass();
        final List<ItemFilter<? extends T>> classFilters = filters.get(filterClass);
        if (classFilters != null && classFilters.remove(filter)) {
            if (classFilters.size() == 0) {
                filters.remove(filterClass);
            }
            orderedFilters.remove(filter);
            remove(filter.getWidget());
            revalidate();
            applyFilters();
        }
    }

    public boolean applyFilters() {
        if (lockFiltering || !initialized) { return false; }

        List<Predicate<? super T>> predicates = new ArrayList<Predicate<? super T>>();
        for (ItemFilter<? extends T> filter : orderedFilters) { //TODO: Support custom filter logic
            if (!filter.isEmpty()) {
                predicates.add(filter.buildPredicate(genericType));
            }
        }
        if (!searchFilter.isEmpty()) {
            predicates.add(searchFilter.buildPredicate(genericType));
        }

        Predicate<? super T> newFilterPredicate = predicates.size() == 0 ? null : Predicates.and(predicates);
        if (filterPredicate == newFilterPredicate) { return false; }

        filterPredicate = newFilterPredicate;
        if (pool != null) {
            updateView(true, null);
        }
        return true;
    }

    /**
     * 
     * isUnfiltered.
     * 
     */
    private boolean isUnfiltered() {
        return filterPredicate == null;
    }

    /**
     * 
     * getHideFilters.
     * 
     * @return true if filters are hidden, false otherwise
     */
    public boolean getHideFilters() {
        return hideFilters;
    }

    /**
     * 
     * setHideFilters.
     * 
     * @param hideFilters0 - if true, hide the filters, otherwise show them
     */
    public void setHideFilters(boolean hideFilters0) {
        if (hideFilters == hideFilters0) { return; }
        hideFilters = hideFilters0;

        boolean visible = !hideFilters0;
        for (ItemFilter<? extends T> filter : orderedFilters) {
            filter.getWidget().setVisible(visible);
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

    /**
     * Refresh displayed items
     */
    public void refresh() {
        updateView(true, getSelectedItems());
    }

    /**
     * 
     * updateView.
     * 
     * @param bForceFilter
     */
    public void updateView(final boolean forceFilter, final Iterable<T> itemsToSelect) {
        final boolean useFilter = (forceFilter && (filterPredicate != null)) || !isUnfiltered();

        if (useFilter || wantUnique || forceFilter) {
            model.clear();
        }

        if (useFilter && wantUnique) {
            Predicate<Entry<T, Integer>> filterForPool = Predicates.compose(filterPredicate, pool.FN_GET_KEY);
            Iterable<Entry<T, Integer>> items = Aggregates.uniqueByLast(Iterables.filter(pool, filterForPool), pool.FN_GET_NAME);
            model.addItems(items);
        }
        else if (useFilter) {
            Predicate<Entry<T, Integer>> pred = Predicates.compose(filterPredicate, pool.FN_GET_KEY);
            model.addItems(Iterables.filter(pool, pred));
        }
        else if (wantUnique) {
            Iterable<Entry<T, Integer>> items = Aggregates.uniqueByLast(pool, pool.FN_GET_NAME);
            model.addItems(items);
        }
        else if (!useFilter && forceFilter) {
            model.addItems(pool);
        }

        currentView.refresh(itemsToSelect, getSelectedIndex(), forceFilter ? 0 : currentView.getScrollValue());

        //update ratio of # in filtered pool / # in total pool
        int total;
        if (!useFilter) {
            total = getFilteredItems().countAll();
        }
        else if (wantUnique) {
            total = 0;
            Iterable<Entry<T, Integer>> items = Aggregates.uniqueByLast(pool, pool.FN_GET_NAME);
            for (Entry<T, Integer> entry : items) {
                total += entry.getValue();
            }
        }
        else {
            total = pool.countAll();
        }
        searchFilter.setRatio("(" + getFilteredItems().countAll() + " / " + total + ")");
    }

    /**
     * 
     * isIncrementalSearchActive.
     * 
     * @return true if an incremental search is currently active
     */
    public boolean isIncrementalSearchActive() {
        return currentView.isIncrementalSearchActive();
    }

    /**
     * 
     * getWantUnique.
     * 
     * @return true if the editor is in "unique item names only" mode.
     */
    public boolean getWantUnique() {
        return wantUnique;
    }

    /**
     * 
     * setWantUnique.
     * 
     * @param unique - if true, the editor will be set to the "unique item names only" mode.
     */
    public void setWantUnique(boolean unique) {
        wantUnique = alwaysNonUnique ? false : unique;
    }

    /**
     * 
     * getAlwaysNonUnique.
     * 
     * @return if true, this editor must always show non-unique items (e.g. quest editor).
     */
    public boolean getAlwaysNonUnique() {
        return alwaysNonUnique;
    }

    /**
     * 
     * setAlwaysNonUnique.
     * 
     * @param nonUniqueOnly - if true, this editor must always show non-unique items (e.g. quest editor).
     */
    public void setAlwaysNonUnique(boolean nonUniqueOnly) {
        alwaysNonUnique = nonUniqueOnly;
    }

    public void setSelectionSupport(int minSelections0, int maxSelections0) {
        for (ItemView<T> view : views) {
            view.setSelectionSupport(minSelections0, maxSelections0);
        }
    }

    /**
     * 
     * isInfinite.
     * 
     * @return whether item manager's pool of items is in infinite supply
     */
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

    public void showMenu() {
        if (contextMenuBuilder != null && getSelectionCount() > 0) {
            if (contextMenu == null) {
                contextMenu = new ContextMenu();
            }
            contextMenu.show();
        }
    }

    public boolean isContextMenuOpen() {
        return contextMenu != null && contextMenu.isVisible();
    }

    public static abstract class ContextMenuBuilder<T> {
        public abstract void buildMenu(final FDropDownMenu menu, final T item);
    }

    private class ContextMenu extends FDropDownMenu {
        @Override
        protected void buildMenu() {
            contextMenuBuilder.buildMenu(this, getSelectedItem());
        }

        @Override
        protected boolean hideBackdropOnPress(float x, float y) {
            Rectangle bounds = currentView.getSelectionBounds();
            if (bounds == null || bounds.contains(x, y)) {
                return false; //don't hide on press if within selection bounds
            }
            return true;
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

            paneSize = updateAndGetPaneSize(screenWidth, screenHeight);
            float w = paneSize.getWidth();
            float h = paneSize.getHeight();

            Rectangle bounds = currentView.getSelectionBounds();

            //try displaying right of selection if possible
            float x = bounds.x + bounds.width;
            float y = bounds.y;
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
                }
            }
            if (y + h > screenHeight) {
                if (y == bounds.y) {
                    //if displaying to left or right, move up if not enough room
                    y = screenHeight - h;
                }
                else {
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

            setBounds(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
        }
    }
}
