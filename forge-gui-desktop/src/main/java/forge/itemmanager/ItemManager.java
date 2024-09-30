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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.gui.GuiUtils;
import forge.gui.UiCommand;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.filters.ItemFilter;
import forge.itemmanager.views.ImageView;
import forge.itemmanager.views.ItemListView;
import forge.itemmanager.views.ItemTableColumn;
import forge.itemmanager.views.ItemView;
import forge.localinstance.skin.FSkinProp;
import forge.screens.deckeditor.views.VCardCatalog;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.*;
import forge.toolbox.FSkin.Colors;
import forge.toolbox.FSkin.SkinIcon;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.util.Aggregates;
import forge.util.ItemPool;
import forge.util.Localizer;
import forge.util.ReflectionUtil;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;

/**
 * ItemManager.
 *
 * @param <T>
 *            the generic type
 */
@SuppressWarnings("serial")
public abstract class ItemManager<T extends InventoryItem> extends JPanel implements IItemManager<T> {
    private ItemPool<T> pool;
    private final ItemManagerModel<T> model;
    private Predicate<? super T> filterPredicate = null;
    private final Map<Class<? extends ItemFilter<? extends T>>, List<ItemFilter<? extends T>>> filters =
            new HashMap<>();
    private final List<ItemFilter<? extends T>> orderedFilters = new ArrayList<>();
    private boolean wantUnique = false;
    private boolean alwaysNonUnique = false;
    private boolean allowMultipleSelections = false;
    private boolean hideFilters = false;
    private boolean showRanking = false;
    private UiCommand itemActivateCommand;
    private ContextMenuBuilder contextMenuBuilder;
    private final Class<T> genericType;
    private final CDetailPicture cDetailPicture;
    private ItemManagerConfig config;
    private final List<ListSelectionListener> selectionListeners = new ArrayList<>();

    private final ItemFilter<? extends T> mainSearchFilter;
    private final SkinnedPanel pnlButtons = new SkinnedPanel(new MigLayout("insets 0, gap 0, ax center, hidemode 3"));

    private static final SkinIcon SEARCH_ICON = FSkin.getIcon(FSkinProp.ICO_SEARCH).resize(20, 20);
    private final FLabel btnFilters = new FLabel.ButtonBuilder()
        .icon(SEARCH_ICON).iconScaleAuto(false)
        .tooltip(Localizer.getInstance().getMessage("lblClickToconfigureFilters"))
        .reactOnMouseDown()
        .build();

    private final FLabel lblCaption = new FLabel.Builder()
        .fontAlign(SwingConstants.LEFT)
        .fontSize(12)
        .build();

    private final FLabel lblRatio = new FLabel.Builder()
        .tooltip(Localizer.getInstance().getMessage("lblShownOfTotalCards"))
        .fontAlign(SwingConstants.LEFT)
        .fontSize(12)
        .build();

    private final FLabel lblEmpty = new FLabel.Builder()
        .text("")
        .fontSize(12)
        .build();
    private FComboBox cbxSection = new FComboBox();

    private static final SkinIcon VIEW_OPTIONS_ICON = FSkin.getIcon(FSkinProp.ICO_SETTINGS).resize(20, 20);
    private final FLabel btnViewOptions = new FLabel.Builder()
        .hoverable()
        .selectable(true)
        .icon(VIEW_OPTIONS_ICON).iconScaleAuto(false)
        .tooltip(Localizer.getInstance().getMessage("lblToggleShowOrHideOptionsForCurrentView"))
        .build();

    private final List<ItemView<T>> views = new ArrayList<>();
    private final ItemListView<T> listView;
    private final ImageView<T> imageView;
    private ItemView<T> currentView;
    private boolean initialized;
    protected boolean lockFiltering;

    /**
     * ItemManager Constructor
     */
    protected ItemManager(final Class<T> genericType0, final CDetailPicture cDetailPicture, final boolean wantUnique0, final boolean showRanking) {
        this.showRanking  = showRanking;
        this.cDetailPicture = cDetailPicture;
        this.genericType = genericType0;
        this.wantUnique = wantUnique0;
        this.model = new ItemManagerModel<>(genericType0);

        this.mainSearchFilter = createSearchFilter();
        this.mainSearchFilter.setAllowRemove(false);

        this.listView = new ItemListView<>(this, this.model);
        this.imageView = createImageView(this.model);

        this.views.add(this.listView);
        this.views.add(this.imageView);
        this.currentView = this.listView;
    }

    protected ImageView<T> createImageView(final ItemManagerModel<T> model0) {
        return new ImageView<>(this, model0, this.showRanking);
    }

    public final CDetailPicture getCDetailPicture() {
        return cDetailPicture;
    }

    /**
     * Initialize item manager if needed
     */
    public void initialize() {
        if (this.initialized) { return; } //avoid initializing more than once

        //initialize views
        for (int i = 0; i < this.views.size(); i++) {
            this.views.get(i).initialize(i);
        }

        //build display
        this.setOpaque(false);
        this.setLayout(null);
        mainSearchFilter.getPanel().setBorder(null);
        mainSearchFilter.getPanel().setVisible(!hideFilters);
        this.add(mainSearchFilter.getPanel());
        this.pnlButtons.setOpaque(false);
        this.pnlButtons.setBorder(new FSkin.MatteSkinBorder(1, 0, 1, 0, FSkin.getColor(Colors.CLR_TEXT)));
        this.add(this.pnlButtons);
        this.add(this.btnFilters);
        this.add(this.lblCaption);
        this.add(this.lblRatio);
        this.add(this.lblEmpty);
        this.cbxSection.setVisible(false);
        this.add(this.cbxSection);
        for (final ItemView<T> view : this.views) {
            this.add(view.getButton());
            view.getButton().setSelected(view == this.currentView);
        }
        this.add(this.btnViewOptions);
        this.btnViewOptions.setSelected(this.currentView.getPnlOptions().isVisible());
        this.add(this.currentView.getPnlOptions());
        this.add(this.currentView.getScroller());

        final Runnable cmdAddCurrentSearch = () -> {
            final ItemFilter<? extends T> searchFilter = mainSearchFilter.createCopy();
            if (searchFilter != null) {
                lockFiltering = true; //prevent updating filtering from this change
                addFilter(searchFilter);
                mainSearchFilter.reset();
                lockFiltering = false;
            }
        };
        final Runnable cmdResetFilters = () -> {
            resetFilters();
            SwingUtilities.invokeLater(ItemManager.this::focus);
        };
        final Runnable cmdHideFilters = () -> {
            setHideFilters(!getHideFilters());
            SwingUtilities.invokeLater(ItemManager.this::focus);
        };

        this.mainSearchFilter.getMainComponent().addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(final KeyEvent e) {
                if (e.getKeyCode() == 10) {
                    if (e.isControlDown() || e.isMetaDown()) {
                        cmdAddCurrentSearch.run();
                    }
                }
            }
        });

        //setup command for btnFilters
        final UiCommand cmdBuildFilterMenu = () -> {
            final JPopupMenu menu = new JPopupMenu(Localizer.getInstance().getMessage("lblFilterMenu"));
            if (hideFilters) {
                GuiUtils.addMenuItem(menu, Localizer.getInstance().getMessage("lblShowFilters"), null, cmdHideFilters);
            } else {
                final JMenu addMenu = GuiUtils.createMenu(Localizer.getInstance().getMessage("lblAddOrEditFilter"));
                GuiUtils.addMenuItem(addMenu, Localizer.getInstance().getMessage("lblCurrentTextSearch"),
                        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                        cmdAddCurrentSearch, !mainSearchFilter.isEmpty());
                if (config != ItemManagerConfig.STRING_ONLY) {
                    buildAddFilterMenu(addMenu);
                }
                menu.add(addMenu);
                GuiUtils.addSeparator(menu);
                GuiUtils.addMenuItem(menu, Localizer.getInstance().getMessage("lblResetFilters"), null, cmdResetFilters);
                GuiUtils.addMenuItem(menu, Localizer.getInstance().getMessage("lblHideFilters"), null, cmdHideFilters);
            }
            menu.show(btnFilters, 0, btnFilters.getHeight());
        };
        this.btnFilters.setCommand(cmdBuildFilterMenu);
        this.btnFilters.setRightClickCommand(cmdBuildFilterMenu); //show menu on right-click too

        this.btnViewOptions.setCommand((Runnable) () -> {
            currentView.getPnlOptions().setVisible(!currentView.getPnlOptions().isVisible());
            revalidate();
        });

        //setup initial filters
        addDefaultFilters();

        this.initialized = true; //must set flag just before applying filters
        if (!applyFilters()) {
            if (this.pool != null) { //ensure view updated even if filter predicate didn't change
                this.updateView(true, null);
            }
        }
    }

    @Override
    public ItemManagerConfig getConfig() {
        return this.config;
    }

    @Override
    public void setup(final ItemManagerConfig config0) {
        this.setup(config0, null);
    }
    public void setup(final ItemManagerConfig config0, final Map<ColumnDef, ItemTableColumn> colOverrides) {
        this.config = config0;
        this.setWantUnique(config0.getUniqueCardsOnly());
        for (final ItemView<T> view : this.views) {
            view.setup(config0, colOverrides);
        }
        this.setViewIndex(config0.getViewIndex());
        this.setHideFilters(config0.getHideFilters());
    }

    public void setViewIndex(final int viewIndex) {
        if (viewIndex < 0 || viewIndex >= this.views.size()) { return; }
        final ItemView<T> view = this.views.get(viewIndex);
        if (this.currentView == view) { return; }

        if (this.config != null) {
            this.config.setViewIndex(viewIndex);
        }

        final int backupIndexToSelect = this.currentView.getSelectedIndex();
        final Iterable<T> itemsToSelect; //only retain selected items if not single selection of first item
        if (backupIndexToSelect > 0 || this.getSelectionCount() > 1) {
            itemsToSelect = this.currentView.getSelectedItems();
        }
        else {
            itemsToSelect = null;
        }

        this.currentView.getButton().setSelected(false);
        this.currentView.getPnlOptions().hideArrowButtons(); //ensure arrow buttons hidden for previous options panel
        this.remove(this.currentView.getPnlOptions());
        this.remove(this.currentView.getScroller());

        this.currentView = view;

        this.btnViewOptions.setSelected(view.getPnlOptions().isVisible());
        view.getButton().setSelected(true);
        view.refresh(itemsToSelect, backupIndexToSelect, 0);

        this.add(view.getPnlOptions());
        this.add(view.getScroller());
        this.revalidate();
        this.repaint();
        this.focus();
    }

    public void setHideViewOptions(final int viewIndex, final boolean hideViewOptions) {
        if (viewIndex < 0 || viewIndex >= this.views.size()) { return; }
        this.views.get(viewIndex).getPnlOptions().setVisible(!hideViewOptions);
    }

    @Override
    public void doLayout() {
        final int buttonPanelHeight = 32;
        final LayoutHelper helper = new LayoutHelper(this);

        boolean showButtonPanel = false;
        if (this.pnlButtons.isVisible()) {
            for (final Component comp : this.pnlButtons.getComponents()) {
                if (comp.isVisible()) {
                    showButtonPanel = true;
                    break;
                }
            }
        }

        if (this.hideFilters) {
            if (showButtonPanel) {
                helper.offset(0, -4);
                helper.fillLine(this.pnlButtons, buttonPanelHeight);
            } else {
                this.pnlButtons.setBounds(0, 0, 0, 0); //prevent horizontal line appearing
            }
        } else {
            for (final ItemFilter<? extends T> filter : this.orderedFilters) {
                helper.fillLine(filter.getPanel(), ItemFilter.PANEL_HEIGHT);
            }
            helper.fillLine(this.mainSearchFilter.getPanel(), ItemFilter.PANEL_HEIGHT);
            helper.newLine(-3);
            helper.fillLine(this.pnlButtons, showButtonPanel ? buttonPanelHeight : 1); //just show border if no buttons
        }
        // get the width for all components
        final int viewButtonWidth = FTextField.HEIGHT;
        final int ratioWidth = this.lblRatio.getAutoSizeWidth();
        int captionWidth = this.lblCaption.getAutoSizeWidth();
        final int cbxSectionWidth = this.cbxSection.isVisible() ? this.cbxSection.getAutoSizeWidth() : 0;
        final int viewButtonCount = this.views.size() + 1; // +1 is for the options button
        final int widthViewButtons = viewButtonCount * viewButtonWidth + helper.getGapX() * (viewButtonCount);

        // remove the space needed by all components that will be displayed
        int availableCaptionWidth = helper.getParentWidth()
                - viewButtonWidth   // btnFilters
                - cbxSectionWidth
                - ratioWidth
                - widthViewButtons;

        if (captionWidth > availableCaptionWidth) { //truncate caption if not enough room for it
            this.lblCaption.setToolTipText(this.lblCaption.getText());
            captionWidth = availableCaptionWidth;
        } else {
            this.lblCaption.setToolTipText(null);
        }

        helper.newLine();
        helper.offset(1, 0); //align filters button with expand/collapse all button
        helper.include(this.btnFilters, viewButtonWidth, FTextField.HEIGHT);
        helper.include(this.lblCaption, captionWidth, FTextField.HEIGHT);
        helper.include(this.cbxSection, cbxSectionWidth, FTextField.HEIGHT);
        helper.offset(helper.getGapX(), 0);
        helper.include(this.lblRatio, ratioWidth, FTextField.HEIGHT);
        helper.fillLine(this.lblEmpty, FTextField.HEIGHT, widthViewButtons);
        for (final ItemView<T> view : this.views) {
            helper.include(view.getButton(), viewButtonWidth, FTextField.HEIGHT);
            helper.offset(-1, 0);
        }
        helper.include(this.btnViewOptions, viewButtonWidth, FTextField.HEIGHT);

        helper.newLine(-1);
        if (this.currentView.getPnlOptions().isVisible()) {
            helper.fillLine(this.currentView.getPnlOptions(), FTextField.HEIGHT + 4);
        }
        helper.fill(this.currentView.getScroller());
    }

    /**
     *
     * getGenericType.
     *
     * @return generic type of items
     */
    @Override
    public Class<T> getGenericType() {
        return this.genericType;
    }

    /**
     *
     * getCaption.
     *
     * @return caption to display before ratio
     */
    @Override
    public String getCaption() {
        return this.lblCaption.getText();
    }

    /**
     *
     * setCaption.
     *
     * @param caption - caption to display before ratio
     */
    @Override
    public void setCaption(final String caption) {
        this.lblCaption.setText(caption);
        this.lblCaption.setLabelFor(this.listView.getTable());
    }

    /**
     *
     * Gets the item pool.
     *
     * @return ItemPoolView
     */
    @Override
    public ItemPool<T> getPool() {
        return this.pool;
    }

    /**
     *
     * Sets the item pool.
     *
     * @param items
     */
    @Override
    public void setPool(final Iterable<T> items) {
        this.setPool(ItemPool.createFrom(items, this.genericType), false);
    }

    /**
     *
     * Sets the item pool.
     *
     * @param poolView
     * @param infinite
     */
    @Override
    public void setPool(final ItemPool<T> poolView, final boolean infinite) {
        this.setPoolImpl(ItemPool.createFrom(poolView, this.genericType), infinite);
    }

    @Override
    public void setPool(final ItemPool<T> pool0) {
        this.setPoolImpl(pool0, false);
    }

    /**
     *
     * Sets the item pool.
     *
     * @param pool0
     * @param infinite
     */
    private void setPoolImpl(final ItemPool<T> pool0, final boolean infinite) {
        this.model.clear();
        this.pool = pool0;
        this.model.addItems(this.pool);
        this.model.setInfinite(infinite);
        this.updateView(true, null);
    }

    public ItemView<T> getCurrentView() {
        return this.currentView;
    }

    /**
     *
     * getItemCount.
     *
     * @return int
     */
    @Override
    public int getItemCount() {
        return this.currentView.getCount();
    }

    /**
     *
     * getSelectionCount.
     *
     * @return int
     */
    @Override
    public int getSelectionCount() {
        return this.currentView.getSelectionCount();
    }

    /**
     *
     * getSelectedItem.
     *
     * @return T
     */
    @Override
    public T getSelectedItem() {
        return this.currentView.getSelectedItem();
    }

    /**
     *
     * getSelectedItems.
     *
     * @return Iterable<T>
     */
    @Override
    public Collection<T> getSelectedItems() {
        return this.currentView.getSelectedItems();
    }

    /**
     *
     * getSelectedItems.
     *
     * @return ItemPool<T>
     */
    @Override
    public ItemPool<T> getSelectedItemPool() {
        final ItemPool<T> selectedItemPool = new ItemPool<>(this.genericType);
        for (final T item : getSelectedItems()) {
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
    @Override
    public boolean setSelectedItem(final T item) {
        return this.currentView.setSelectedItem(item);
    }

    /**
     *
     * setSelectedItems.
     *
     * @param items - Items to select
     */
    @Override
    public boolean setSelectedItems(final Iterable<T> items) {
        return this.currentView.setSelectedItems(items);
    }

    /**
     *
     * stringToItem.
     *
     * @param str - String to get item corresponding to
     */
    @Override
    public T stringToItem(final String str) {
        if (this.pool == null) {
            return null;
        }

        for (final Entry<T, Integer> itemEntry : this.pool) {
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
    @Override
    public boolean setSelectedString(final String str) {
        final T item = stringToItem(str);
        if (item != null) {
            return this.setSelectedItem(item);
        }
        return false;
    }

    /**
     *
     * setSelectedStrings.
     *
     * @param strings - Strings to select
     */
    @Override
    public boolean setSelectedStrings(final Iterable<String> strings) {
        final List<T> items = new ArrayList<>();
        for (final String str : strings) {
            final T item = stringToItem(str);
            if (item != null) {
                items.add(item);
            }
        }
        return this.setSelectedItems(items);
    }

    /**
     *
     * selectItemEntrys.
     *
     * @param itemEntrys - Item entrys to select
     */
    @Override
    public boolean selectItemEntrys(final Iterable<Entry<T, Integer>> itemEntrys) {
        final List<T> items = new ArrayList<>();
        for (final Entry<T, Integer> itemEntry : itemEntrys) {
            items.add(itemEntry.getKey());
        }
        return this.setSelectedItems(items);
    }

    /**
     *
     * selectAll.
     *
     */
    @Override
    public void selectAll() {
        this.currentView.selectAll();
    }

    /**
     *
     * getSelectedItem.
     *
     * @return T
     */
    @Override
    public int getSelectedIndex() {
        return this.currentView.getSelectedIndex();
    }

    /**
     *
     * getSelectedItems.
     *
     * @return Iterable<Integer>
     */
    @Override
    public Iterable<Integer> getSelectedIndices() {
        return this.currentView.getSelectedIndices();
    }

    /**
     *
     * setSelectedIndex.
     *
     * @param index - Index to select
     */
    @Override
    public void setSelectedIndex(final int index) {
        this.currentView.setSelectedIndex(index);
    }

    /**
     *
     * setSelectedIndices.
     *
     * @param indices - Indices to select
     */
    @Override
    public void setSelectedIndices(final Integer[] indices) {
        this.currentView.setSelectedIndices(Arrays.asList(indices));
    }

    @Override
    public void setSelectedIndices(final Iterable<Integer> indices) {
        this.currentView.setSelectedIndices(indices);
    }

    /**
     *
     * addItem.
     *
     * @param item
     * @param qty
     */
    @Override
    public void addItem(final T item, final int qty) {
        if (this.isInfinite() && this.model.getOrderedList().contains(item)) {
            return;
        }
        this.pool.add(item, qty);
        if (this.isUnfiltered()) {
            this.model.addItem(item, qty);
        }
        final List<T> items = new ArrayList<>();
        items.add(item);
        this.updateView(false, items);
    }

    /**
     *
     * addItems.
     *
     * @param itemsToAdd
     */
    @Override
    public void addItems(final Iterable<Entry<T, Integer>> itemsToAdd) {
        if (this.isInfinite() && this.model.getOrderedList().containsAll(Lists.newArrayList(itemsToAdd))) {
            return;
        }
        this.pool.addAll(itemsToAdd);
        if (this.isUnfiltered()) {
            this.model.addItems(itemsToAdd);
        }

        final List<T> items = new ArrayList<>();
        for (final Map.Entry<T, Integer> item : itemsToAdd) {
            items.add(item.getKey());
        }
        this.updateView(false, items);
    }

    /**
     *
     * removeItem.
     *
     * @param item
     * @param qty
     */
    @Override
    public void removeItem(final T item, final int qty) {
        final Iterable<T> itemsToSelect = this.currentView == this.listView ? this.getSelectedItems() : null;

        this.pool.remove(item, qty);
        if (this.isUnfiltered()) {
            this.model.removeItem(item, qty);
        }
        this.updateView(false, itemsToSelect);
    }

    /**
     *
     * removeItems.
     *
     * @param itemsToRemove
     */
    @Override
    public void removeItems(final Iterable<Map.Entry<T, Integer>> itemsToRemove) {
        final Iterable<T> itemsToSelect = this.currentView == this.listView ? this.getSelectedItems() : null;

        for (final Map.Entry<T, Integer> item : itemsToRemove) {
            this.pool.remove(item.getKey(), item.getValue());
            if (this.isUnfiltered()) {
                this.model.removeItem(item.getKey(), item.getValue());
            }
        }
        this.updateView(false, itemsToSelect);
    }

    /**
     *
     * removeAllItems.
     *
     */
    @Override
    public void removeAllItems() {
        this.pool.clear();
        this.model.clear();
        this.updateView(false, null);
    }

    /**
     *
     * scrollSelectionIntoView.
     *
     */
    @Override
    public void scrollSelectionIntoView() {
        this.currentView.scrollSelectionIntoView();
    }

    /**
     *
     * getItemCount.
     *
     * @param item
     */
    @Override
    public int getItemCount(final T item) {
        return this.model.isInfinite() ? Integer.MAX_VALUE : this.pool.count(item);
    }

    /**
     * Gets all filtered items in the model.
     *
     * @return ItemPoolView<T>
     */
    @Override
    public ItemPool<T> getFilteredItems() {
        return this.model.getItems();
    }

    protected abstract void addDefaultFilters();
    protected abstract ItemFilter<? extends T> createSearchFilter();
    protected abstract void buildAddFilterMenu(JMenu menu);

    protected <F extends ItemFilter<? extends T>> F getFilter(final Class<F> filterClass) {
        List<ItemFilter<? extends T>> filters = this.filters.get(filterClass);
        if (filters == null || filters.isEmpty()) {
            return null;
        }
        return ReflectionUtil.safeCast(filters.get(0), filterClass);
    }

    @SuppressWarnings("unchecked")
    public void addFilter(final ItemFilter<? extends T> filter) {
        final Class<? extends ItemFilter<? extends T>> filterClass = (Class<? extends ItemFilter<? extends T>>) filter.getClass();
        List<ItemFilter<? extends T>> classFilters = this.filters.computeIfAbsent(filterClass, k -> new ArrayList<>());
        if (classFilters.size() > 0) {
            //if filter with the same class already exists, try to merge if allowed
            //NOTE: can always use first filter for these checks since if
            //merge is supported, only one will ever exist
            final ItemFilter<? extends T> existingFilter = classFilters.get(0);
            if (existingFilter.merge(filter)) {
                //if new filter merged with existing filter, just refresh the widget
                existingFilter.refreshWidget();
                this.applyNewOrModifiedFilter(existingFilter);
                return;
            }
        }
        classFilters.add(filter);
        orderedFilters.add(filter);
        this.add(filter.getPanel());

        final boolean visible = !this.hideFilters;
        filter.getPanel().setVisible(visible);
        if (visible && this.initialized) {
            this.revalidate();
            this.applyNewOrModifiedFilter(filter);
        }
    }

    //apply filters and focus existing filter's main component if filtering not locked
    public void applyNewOrModifiedFilter(final ItemFilter<? extends T> filter) {
        if (this.lockFiltering) {
            filter.afterFiltersApplied(); //ensure this called even if filters currently locked
            return;
        }

        if (!applyFilters()) {
            filter.afterFiltersApplied(); //ensure this called even if filters didn't need to be updated
        }
        SwingUtilities.invokeLater(() -> filter.getMainComponent().requestFocusInWindow());
    }

    public void restoreDefaultFilters() {
        lockFiltering = true;
        for (final ItemFilter<? extends T> filter : this.orderedFilters) {
            this.remove(filter.getPanel());
        }
        this.filters.clear();
        this.orderedFilters.clear();
        addDefaultFilters();
        lockFiltering = false;
        this.revalidate();
        this.applyFilters();
    }

    @SuppressWarnings("unchecked")
    public void removeFilter(final ItemFilter<? extends T> filter) {
        final Class<? extends ItemFilter<? extends T>> filterClass = (Class<? extends ItemFilter<? extends T>>) filter.getClass();
        final List<ItemFilter<? extends T>> classFilters = this.filters.get(filterClass);
        if (classFilters != null && classFilters.remove(filter)) {
            if (classFilters.size() == 0) {
                this.filters.remove(filterClass);
            }
            this.orderedFilters.remove(filter);
            this.remove(filter.getPanel());
            this.revalidate();
            applyFilters();
        }
    }

    @Override
    public boolean applyFilters() {
        if (this.lockFiltering || !this.initialized) { return false; }

        final List<Predicate<? super T>> predicates = new ArrayList<>();
        for (final ItemFilter<? extends T> filter : this.orderedFilters) { //TODO: Support custom filter logic
            if (filter.isEnabled() && !filter.isEmpty()) {
                predicates.add(filter.buildPredicate(this.genericType));
            }
        }
        if (this.mainSearchFilter.isEnabled() && !this.mainSearchFilter.isEmpty()) {
            predicates.add(mainSearchFilter.buildPredicate(this.genericType));
        }

        final Predicate<? super T> newFilterPredicate = predicates.size() == 0 ? null : Predicates.and(predicates);
        if (this.filterPredicate == newFilterPredicate) { return false; }

        this.filterPredicate = newFilterPredicate;
        if (this.pool != null) {
            this.updateView(true, this.getSelectedItems());
        }
        return true;
    }

    /**
     *
     * isUnfiltered.
     *
     */
    private boolean isUnfiltered() {
        return this.filterPredicate == null;
    }

    /**
     *
     * getHideFilters.
     *
     * @return true if filters are hidden, false otherwise
     */
    public boolean getHideFilters() {
        return this.hideFilters;
    }

    /**
     *
     * setHideFilters.
     *
     * @param hideFilters0 - if true, hide the filters, otherwise show them
     */
    public void setHideFilters(final boolean hideFilters0) {
        if (this.hideFilters == hideFilters0) { return; }
        this.hideFilters = hideFilters0;

        final boolean visible = !hideFilters0;
        for (final ItemFilter<? extends T> filter : this.orderedFilters) {
            filter.getPanel().setVisible(visible);
        }
        this.mainSearchFilter.getPanel().setVisible(visible);

        if (this.initialized) {
            this.revalidate();

            if (hideFilters0) {
                this.resetFilters(); //reset filters when they're hidden
            }
            else {
                this.applyFilters();
            }

            if (this.config != null) {
                this.config.setHideFilters(hideFilters0);
            }
        }
    }

    /**
     *
     * resetFilters.
     *
     */
    public void resetFilters() {
        lockFiltering = true; //prevent updating filtering from this change until all filters reset
        for (final ItemFilter<? extends T> filter : orderedFilters) {
            filter.setEnabled(true);
            filter.reset();
        }
        mainSearchFilter.reset();
        lockFiltering = false;

        applyFilters();
    }

    /**
     * Refresh displayed items
     */
    public void refresh() {
        this.updateView(true, this.getSelectedItems());
    }

    protected Iterable<Entry<T, Integer>> getUnique(final Iterable<Entry<T, Integer>> items) {
        return Aggregates.uniqueByLast(items, from -> from.getKey().getName());
    }

    /**
     *
     * updateView
     */
    public void updateView(final boolean forceFilter, final Iterable<T> itemsToSelect) {
        final boolean useFilter = (forceFilter && (this.filterPredicate != null)) || !isUnfiltered();

        if (useFilter || this.wantUnique || forceFilter) {
            this.model.clear();
        }

        if (useFilter && this.wantUnique) {
            final Predicate<Entry<T, Integer>> filterForPool = Predicates.compose(this.filterPredicate, Entry::getKey);
            final Iterable<Entry<T, Integer>> items = getUnique(Iterables.filter(this.pool, filterForPool));
            this.model.addItems(items);
        }
        else if (useFilter) {
            final Predicate<Entry<T, Integer>> pred = Predicates.compose(this.filterPredicate, Entry::getKey);
            this.model.addItems(Iterables.filter(this.pool, pred));
        }
        else if (this.wantUnique) {
            final Iterable<Entry<T, Integer>> items = getUnique(this.pool);
            this.model.addItems(items);
        }
        else if (!useFilter && forceFilter) {
            this.model.addItems(this.pool);
        }

        this.currentView.refresh(itemsToSelect, this.getSelectedIndex(), forceFilter ? 0 : this.currentView.getScrollValue());

        for (final ItemFilter<? extends T> filter : this.orderedFilters) {
            filter.afterFiltersApplied();
        }

        //update ratio of # in filtered pool / # in total pool
        int total;
        if (!useFilter) {
            total = this.getFilteredItems().countAll();
        }
        else if (this.wantUnique) {
            total = 0;
            final Iterable<Entry<T, Integer>> items = Aggregates.uniqueByLast(this.pool, from -> from.getKey().getName());
            for (final Entry<T, Integer> entry : items) {
                total += entry.getValue();
            }
        }
        else {
            total = this.pool.countAll();
        }
        this.lblRatio.setText("(" + this.getFilteredItems().countAll() + " / " + total + ")");
    }

    /**
     *
     * getPnlButtons.
     *
     * @return panel to put any custom buttons on
     */
    public JPanel getPnlButtons() {
        return this.pnlButtons;
    }

    public FComboBox getCbxSection() {
        return this.cbxSection;
    }

    /**
     *
     * isIncrementalSearchActive.
     *
     * @return true if an incremental search is currently active
     */
    public boolean isIncrementalSearchActive() {
        return this.currentView.isIncrementalSearchActive();
    }

    /**
     *
     * getWantUnique.
     *
     * @return true if the editor is in "unique item names only" mode.
     */
    public boolean getWantUnique() {
        return this.wantUnique && !this.alwaysNonUnique;
    }

    /**
     *
     * setWantUnique.
     *
     * @param unique - if true, the editor will be set to the "unique item names only" mode.
     */
    public void setWantUnique(final boolean unique) {
        this.wantUnique = !this.alwaysNonUnique && unique;
    }

    /**
     *
     * getAlwaysNonUnique.
     *
     * @return if true, this editor must always show non-unique items (e.g. quest editor).
     */
    public boolean getAlwaysNonUnique() {
        return this.alwaysNonUnique;
    }

    /**
     *
     * setAlwaysNonUnique.
     *
     * @param nonUniqueOnly - if true, this editor must always show non-unique items (e.g. quest editor).
     */
    public void setAlwaysNonUnique(final boolean nonUniqueOnly) {
        this.alwaysNonUnique = nonUniqueOnly;
    }

    /**
     *
     * getAllowMultipleSelections.
     *
     * @return if true, multiple items can be selected at once
     */
    public boolean getAllowMultipleSelections() {
        return this.allowMultipleSelections;
    }

    /**
     *
     * setAllowMultipleSelections.
     *
     * @return allowMultipleSelections0 - if true, multiple items can be selected at once
     */
    public void setAllowMultipleSelections(final boolean allowMultipleSelections0) {
        if (this.allowMultipleSelections == allowMultipleSelections0) { return; }
        this.allowMultipleSelections = allowMultipleSelections0;
        for (final ItemView<T> view : views) {
            view.setAllowMultipleSelections(allowMultipleSelections0);
        }
    }

    /**
     *
     * isInfinite.
     *
     * @return whether item manager's pool of items is in infinite supply
     */
    public boolean isInfinite() {
        return this.model.isInfinite();
    }

    /**
     *
     * focus.
     *
     */
    public void focus() {
        this.currentView.focus();
    }

    /**
     *
     * focusSearch.
     *
     */
    public void focusSearch() {
        this.setHideFilters(false); //ensure filters shown
        this.mainSearchFilter.getMainComponent().requestFocusInWindow();
    }

    public void addSelectionListener(final ListSelectionListener listener) {
        selectionListeners.remove(listener); //ensure listener not added multiple times
        selectionListeners.add(listener);
    }

    public Iterable<ListSelectionListener> getSelectionListeners() {
        return selectionListeners;
    }

    public void setItemActivateCommand(final UiCommand itemActivateCommand0) {
        this.itemActivateCommand = itemActivateCommand0;
    }

    public void activateSelectedItems() {
        if (this.itemActivateCommand != null) {
            this.itemActivateCommand.run();
        }
    }

    public void setContextMenuBuilder(final ContextMenuBuilder contextMenuBuilder0) {
        this.contextMenuBuilder = contextMenuBuilder0;
    }

    public void showContextMenu(final MouseEvent e) {
        showContextMenu(e, null);
    }
    public void showContextMenu(final MouseEvent e, final Runnable onClose) {
        //ensure the item manager has focus
        this.focus();

        //if item under the cursor is not selected, select it
        final int index = this.currentView.getIndexAtPoint(e.getPoint());
        boolean needSelection = true;
        for (final Integer selectedIndex : this.getSelectedIndices()) {
            if (selectedIndex == index) {
                needSelection = false;
                break;
            }
        }
        if (needSelection) {
            this.setSelectedIndex(index);
        }

        if (this.contextMenuBuilder == null) {
            if (onClose != null) {
                onClose.run(); //run onClose immediately even if no context menu shown
            }
            return;
        }

        final JPopupMenu menu = new JPopupMenu("ItemManagerContextMenu");
        this.contextMenuBuilder.buildContextMenu(menu);

        if (onClose != null) {
            menu.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuCanceled(final PopupMenuEvent arg0) {
                    onClose.run();
                }

                @Override
                public void popupMenuWillBecomeInvisible(final PopupMenuEvent arg0) {
                    onClose.run();
                }

                @Override
                public void popupMenuWillBecomeVisible(final PopupMenuEvent arg0) {
                }
            });
        }

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    protected List<String> filteredSetCodesInCatalog = null;
    protected List<String> getFilteredSetCodesInCatalog(){
        if (filteredSetCodesInCatalog == null) {
            ItemManager<? extends InventoryItem> cardsManager = VCardCatalog.SINGLETON_INSTANCE.getItemManager();
            if (cardsManager == null)
                return null;
            try {
                ItemPool<PaperCard> cardsPool = (ItemPool<PaperCard>) cardsManager.getPool();
                Set<String> uniqueSetCodes = new HashSet<>();  // init
                for (Entry<PaperCard, Integer> entry : cardsPool)
                    uniqueSetCodes.add(entry.getKey().getEdition());
                filteredSetCodesInCatalog = new ArrayList<>(uniqueSetCodes);
            } catch (ClassCastException ex) {
                return null;
            }
        }
        return filteredSetCodesInCatalog;
    }
}
