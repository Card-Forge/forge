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
package forge.gui.deckeditor.controllers;

import java.util.Map.Entry;

import javax.swing.SwingUtilities;

import forge.Command;
import forge.deck.DeckBase;
import forge.deck.DeckSection;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.framework.SRearrangingUtil;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.InventoryItem;
import forge.util.ItemPool;
import forge.view.FView;

/**
 * Maintains a generically typed architecture for various editing
 * environments.  A basic editor instance requires a card catalog, the
 * current deck being edited, and optional filters on the catalog.
 * <br><br>
 * These requirements are collected in this class and manipulated
 * in subclasses for different environments. There are two generic
 * types for all card display and filter predicates.
 * 
 * <br><br><i>(A at beginning of class name denotes an abstract class.)</i>
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 * @param <TItem> extends {@link forge.item.InventoryItem}
 * @param <TModel> extends {@link forge.deck.DeckBase}
 */
public abstract class ACEditorBase<TItem extends InventoryItem, TModel extends DeckBase> {
    public interface ContextMenuBuilder {
        /**
         * Adds move-related items to the context menu
         * 
         * @param verb Examples: "Sell", "Add"
         * @param nounSingular Examples: "item", "card"
         * @param nounPlural Examples: "items", "cards"
         * @param destination Examples: null, "to deck", "to sideboard"
         */
        public void addMoveItems (String verb, String nounSingular, String nounPlural, String destination);
        public void addMoveAlternateItems (String verb, String nounSingular, String nounPlural, String destination);
    }

    public boolean listenersHooked;
    private final FScreen screen;
    private ItemManager<TItem> catalogManager;
    private ItemManager<TItem> deckManager;
    protected DeckSection sectionMode = DeckSection.Main;

    // card transfer buttons
    private final FLabel btnAdd = new FLabel.Builder()
            .fontSize(14)
            .text("Add card")
            .tooltip("Add selected card to current deck (or double click the row or hit the spacebar)")
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_PLUS))
            .iconScaleAuto(false).hoverable().build();
    private final FLabel btnAdd4 = new FLabel.Builder()
            .fontSize(14)
            .text("Add 4 of card")
            .tooltip("Add up to 4 of selected card to current deck")
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_PLUS))
            .iconScaleAuto(false).hoverable().build();

    private final FLabel btnRemove = new FLabel.Builder()
            .fontSize(14)
            .text("Remove card")
            .tooltip("Remove selected card from current deck (or double click the row or hit the spacebar)")
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_MINUS))
            .iconScaleAuto(false).hoverable().build();

    private final FLabel btnRemove4 = new FLabel.Builder()
            .fontSize(14)
            .text("Remove 4 of card")
            .tooltip("Remove up to 4 of selected card to current deck")
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_MINUS))
            .iconScaleAuto(false).hoverable().build();

    private final FLabel btnCycleSection = new FLabel.Builder()
            .fontSize(14)
            .text("Change Section")
            .tooltip("Toggle between editing the deck and the sideboard/planar/scheme/vanguard parts of this deck")
            .icon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_EDIT))
            .iconScaleAuto(false).hoverable().build();
    
    protected ACEditorBase(FScreen screen0) {
        this.screen = screen0;
    }
    
    public FScreen getScreen() {
        return this.screen;
    }

    public DeckSection getSectionMode() {
        return this.sectionMode;
    }

    public final void addItem(TItem item) {
        onAddItems(createPoolForItem(item, 1), false);
    }
    public final void addItem(TItem item, int qty) {
        onAddItems(createPoolForItem(item, qty), false);
    }
    public final void addItem(TItem item, int qty, boolean toAlternate) {
        onAddItems(createPoolForItem(item, qty), toAlternate);
    }

    public final void removeItem(TItem item) {
        onRemoveItems(createPoolForItem(item, 1), false);
    }
    public final void removeItem(TItem item, int qty) {
        onRemoveItems(createPoolForItem(item, qty), false);
    }
    public final void removeItem(TItem item, int qty, boolean toAlternate) {
        onRemoveItems(createPoolForItem(item, qty), toAlternate);
    }

    @SuppressWarnings("unchecked")
    private ItemPool<TItem> createPoolForItem(final TItem item, final int qty) {
        if (item == null || qty <= 0) { return null; }

        ItemPool<TItem> pool = new ItemPool<TItem>((Class<TItem>)item.getClass());
        pool.add(item, qty);
        return pool;
    }

    public final void addItems(Iterable<Entry<TItem, Integer>> items, boolean toAlternate) {
        if (items == null || !items.iterator().hasNext()) { return; } //do nothing if no items
        onAddItems(items, toAlternate);
    }

    public final void removeItems(Iterable<Entry<TItem, Integer>> items, boolean toAlternate) {
        if (items == null || !items.iterator().hasNext()) { return; } //do nothing if no items
        onRemoveItems(items, toAlternate);
    }

    /** 
     * Operation to add selected items to current deck.
     */
    protected abstract void onAddItems(Iterable<Entry<TItem, Integer>> items, boolean toAlternate);

    /**
     * Operation to remove selected item from current deck.
     */
    protected abstract void onRemoveItems(Iterable<Entry<TItem, Integer>> items, boolean toAlternate);

    public abstract void buildAddContextMenu(ContextMenuBuilder cmb);
    public abstract void buildRemoveContextMenu(ContextMenuBuilder cmb);
    
    /**
     * Resets the cards in the catalog table and current deck table.
     */
    public abstract void resetTables();

    /**
     * Gets controller responsible for the current deck being edited.
     *
     * @return {@link forge.gui.deckeditor.controllers.DeckController}
     */
    public abstract DeckController<TModel> getDeckController();

    /**
     * Called when switching away from or closing the editor wants to exit. Should confirm save options.
     * 
     * @return boolean &emsp; true if safe to exit
     */
    public abstract boolean canSwitchAway(boolean isClosing);

    /**
     * Resets and initializes the current editor.
     */
    public abstract void update();

    /**
     * Reset UI changes made in update
     */
    public abstract void resetUIChanges();

    /**
     * Gets the ItemManager holding the cards in the current deck.
     * 
     * @return {@link forge.gui.toolbox.itemmanager.ItemManager}
     */
    public ItemManager<TItem> getDeckManager() {
        return this.deckManager;
    }

    /**
     * Sets the ItemManager holding the cards in the current deck.
     * 
     * @param itemManager &emsp; {@link forge.gui.toolbox.itemmanager.ItemManager}
     */
    @SuppressWarnings("serial")
    public void setDeckManager(final ItemManager<TItem> itemManager) {
        this.deckManager = itemManager;

        btnRemove.setCommand(new Command() {
            @Override
            public void run() {
                CDeckEditorUI.SINGLETON_INSTANCE.removeSelectedCards(false, 1);
            }
        });
        btnRemove4.setCommand(new Command() {
            @Override
            public void run() {
                CDeckEditorUI.SINGLETON_INSTANCE.removeSelectedCards(false, 4);
            }
        });
        itemManager.getPnlButtons().add(btnRemove, "w 30%!, h 30px!, gapx 5");
        itemManager.getPnlButtons().add(btnRemove4, "w 30%!, h 30px!, gapx 5");
        itemManager.getPnlButtons().add(btnCycleSection, "w 30%!, h 30px!, gapx 5");
    }

    /**
     * Gets the ItemManager holding the cards in the current catalog.
     * 
     * @return {@link forge.gui.toolbox.itemmanager.ItemManager}
     */
    public ItemManager<TItem> getCatalogManager() {
        return this.catalogManager;
    }

    /**
     * Sets the ItemManager holding the cards in the current catalog.
     * 
     * @param itemManager &emsp; {@link forge.gui.toolbox.itemmanager.ItemManager}
     */
    @SuppressWarnings("serial")
    public void setCatalogManager(final ItemManager<TItem> itemManager) {
        this.catalogManager = itemManager;
        itemManager.setCaption("Catalog");

        btnAdd.setCommand(new Command() {
            @Override
            public void run() {
                CDeckEditorUI.SINGLETON_INSTANCE.addSelectedCards(false, 1);
            }
        });
        btnAdd4.setCommand(new Command() {
            @Override
            public void run() {
                CDeckEditorUI.SINGLETON_INSTANCE.addSelectedCards(false, 4);
            }
        });
        itemManager.getPnlButtons().add(btnAdd, "w 30%!, h 30px!, h 30px!, gapx 5");
        itemManager.getPnlButtons().add(btnAdd4, "w 30%!, h 30px!, h 30px!, gapx 5");
    }

    /**
     * Removes the specified tab and returns its parent for later re-adding
     */
    protected DragCell removeTab (IVDoc<? extends ICDoc> tab) {
        final DragCell parent;
        if (tab.getParentCell() == null) {
            parent = null;
        } else {
            parent = tab.getParentCell();
            parent.removeDoc(tab);
            tab.setParentCell(null);

            if (parent.getDocs().size() > 0) {
                // if specified tab was first child of its parent, the new first tab needs re-selecting.
                parent.setSelected(parent.getDocs().get(0));
            } else {
                // if the parent is now childless, fill in the resultant gap
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        SRearrangingUtil.fillGap(parent);
                        FView.SINGLETON_INSTANCE.removeDragCell(parent);
                    }
                });
            }
        }

        return parent;
    }

    public FLabel getBtnAdd()     { return btnAdd; }
    public FLabel getBtnAdd4()    { return btnAdd4; }
    public FLabel getBtnRemove()  { return btnRemove; }
    public FLabel getBtnRemove4() { return btnRemove4; }
    public FLabel getBtnCycleSection() { return btnCycleSection; }
}
