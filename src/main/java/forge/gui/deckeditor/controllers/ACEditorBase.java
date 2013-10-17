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

import javax.swing.SwingUtilities;

import forge.deck.DeckBase;
import forge.gui.framework.DragCell;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.framework.SRearrangingUtil;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.InventoryItem;
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
        public void addTextFilterItem ();
    }

    public boolean listenersHooked;
    private ItemManager<TItem> catalogManager;
    private ItemManager<TItem> deckManager;
    
    /** 
     * Operation to add one of selected card to current deck.
     */
    public abstract void addCard(InventoryItem item, boolean toAlternate, int qty);

    /**
     * Operation to remove one of selected card from current deck.
     */
    public abstract void removeCard(InventoryItem item, boolean toAlternate, int qty);

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
     * Called when an editor wants to exit. Should confirm save options,
     * update next UI screen, etc.
     * 
     * @return boolean &emsp; true if safe to exit
     */
    public abstract boolean exit();

    /**
     * Resets and initializes the current editor.
     */
    public abstract void update();

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
    public void setDeckManager(final ItemManager<TItem> itemManager) {
        this.deckManager = itemManager;
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
    public void setCatalogManager(final ItemManager<TItem> itemManager) {
        this.catalogManager = itemManager;
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
}
