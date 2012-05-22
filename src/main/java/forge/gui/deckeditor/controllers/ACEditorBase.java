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

import forge.deck.DeckBase;
import forge.gui.deckeditor.tables.DeckController;
import forge.gui.deckeditor.tables.TableView;
import forge.item.InventoryItem;

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

    private TableView<TItem> tblCatalog;
    private TableView<TItem> tblDeck;
    /** 
     * Operation to add one of selected card to current deck.
     */
    public abstract void addCard();

    /**
     * Operation to remove one of selected card from current deck.
     */
    public abstract void removeCard();

    /**
     * Resets the cards in the catalog table and current deck table.
     */
    public abstract void resetTables();

    /**
     * Gets controller responsible for the current deck being edited.
     *
     * @return {@link forge.gui.deckeditor.tables.DeckController}
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
    public abstract void init();

    /**
     * Gets the TableView holding the cards in the current deck.
     * 
     * @return {@link forge.gui.deckeditor.tables.TableView}
     */
    public TableView<TItem> getTableDeck() {
        return this.tblDeck;
    }

    /**
     * Sets the TableView holding the cards in the current deck.
     * 
     * @param table0 &emsp; {@link forge.gui.deckeditor.tables.TableView}
     */
    public void setTableDeck(final TableView<TItem> table0) {
        this.tblDeck = table0;
    }

    /**
     * Gets the TableView holding the cards in the current catalog.
     * 
     * @return {@link forge.gui.deckeditor.tables.TableView}
     */
    public TableView<TItem> getTableCatalog() {
        return this.tblCatalog;
    }

    /**
     * Sets the TableView holding the cards in the current catalog.
     * 
     * @param table0 &emsp; {@link forge.gui.deckeditor.tables.TableView}
     */
    public void setTableCatalog(final TableView<TItem> table0) {
        this.tblCatalog = table0;
    }

}
