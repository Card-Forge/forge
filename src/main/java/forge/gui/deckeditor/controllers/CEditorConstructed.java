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

import java.util.List;

import com.google.common.base.Supplier;
import forge.Command;

import forge.Singletons;
import forge.deck.Deck;
import forge.gui.deckeditor.SEditorIO;
import forge.gui.deckeditor.SEditorIO.EditorPreference;
import forge.gui.deckeditor.SEditorUtil;
import forge.gui.deckeditor.tables.DeckController;
import forge.gui.deckeditor.tables.SColumnUtil;
import forge.gui.deckeditor.tables.SColumnUtil.ColumnName;
import forge.gui.deckeditor.tables.TableColumnInfo;
import forge.gui.deckeditor.tables.EditorTableView;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.framework.EDocID;
import forge.gui.toolbox.FLabel;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.properties.ForgePreferences.FPref;

/**
 * Child controller for constructed deck editor UI.
 * This is the least restrictive mode;
 * all cards are available.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 * 
 * @author Forge
 * @version $Id$
 */
public final class CEditorConstructed extends ACEditorBase<CardPrinted, Deck> {
    private final DeckController<Deck> controller;
    private boolean sideboardMode = false;

    //=========== Constructor
    /**
     * Child controller for constructed deck editor UI.
     * This is the least restrictive mode;
     * all cards are available.
     */
    public CEditorConstructed() {
        super();

        boolean wantUnique = SEditorIO.getPref(EditorPreference.display_unique_only);

        final EditorTableView<CardPrinted> tblCatalog = new EditorTableView<CardPrinted>(wantUnique, CardPrinted.class);
        final EditorTableView<CardPrinted> tblDeck = new EditorTableView<CardPrinted>(wantUnique, CardPrinted.class);

        VCardCatalog.SINGLETON_INSTANCE.setTableView(tblCatalog.getTable());
        VCurrentDeck.SINGLETON_INSTANCE.setTableView(tblDeck.getTable());

        this.setTableCatalog(tblCatalog);
        this.setTableDeck(tblDeck);

        final Supplier<Deck> newCreator = new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        };
        this.controller = new DeckController<Deck>(Singletons.getModel().getDecks().getConstructed(), this, newCreator);
    }

    //=========== Overridden from ACEditorBase

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#addCard()
     */
    @Override
    public void addCard(InventoryItem item) {
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
        this.getTableDeck().addCard(card);
        if (sideboardMode) {
            this.getTableCatalog().removeCard(card);
        }
        this.controller.notifyModelChanged();
        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText("*Current Deck");
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#removeCard()
     */
    @Override
    public void removeCard(InventoryItem item) {
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
        this.getTableDeck().removeCard(card);
        if (sideboardMode) {
            this.getTableCatalog().addCard(card);
        }
        this.controller.notifyModelChanged();
        VCurrentDeck.SINGLETON_INSTANCE.getTabLabel().setText("*Current Deck");
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#updateView()
     */
    @Override
    public void resetTables() {
        // Constructed mode can use all cards, no limitations.
        this.getTableCatalog().setDeck(ItemPool.createFrom(CardDb.instance().getAllTraditionalCards(), CardPrinted.class, true));
        this.getTableDeck().setDeck(this.controller.getModel().getMain());
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#getController()
     */
    @Override
    public DeckController<Deck> getDeckController() {
        return this.controller;
    }

    /**
     * Switch between the main deck and the sideboard editor.
     */
    public void switchEditorMode(boolean isSideboarding) {
        final List<TableColumnInfo<InventoryItem>> lstCatalogCols = SColumnUtil.getCatalogDefaultColumns();

        if (isSideboarding) {

            this.getTableCatalog().setAvailableColumns(lstCatalogCols);
            this.getTableCatalog().setDeck(this.controller.getModel().getMain());
            this.getTableDeck().setDeck(this.controller.getModel().getSideboard());
        } else {
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));
            this.getTableCatalog().setAvailableColumns(lstCatalogCols);
            this.getTableCatalog().setDeck(ItemPool.createFrom(CardDb.instance().getAllTraditionalCards(), CardPrinted.class, true));
            this.getTableDeck().setDeck(this.controller.getModel().getMain());
        }

        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText(isSideboarding ? "Main Deck" : "Card Catalog");
        VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().setVisible(!isSideboarding);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().setVisible(!isSideboarding);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSave().setVisible(!isSideboarding);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().setVisible(!isSideboarding);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnPrintProxies().setVisible(!isSideboarding);
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setVisible(!isSideboarding);
        VCurrentDeck.SINGLETON_INSTANCE.getLblTitle().setText(isSideboarding ? "Sideboard" : "Title:");

        this.controller.notifyModelChanged();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#show(forge.Command)
     */
    @Override
    public void init() {
        final List<TableColumnInfo<InventoryItem>> lstCatalogCols = SColumnUtil.getCatalogDefaultColumns();
        lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));

        this.getTableCatalog().setup(VCardCatalog.SINGLETON_INSTANCE, lstCatalogCols);
        this.getTableDeck().setup(VCurrentDeck.SINGLETON_INSTANCE, SColumnUtil.getDeckDefaultColumns());

        SEditorUtil.resetUI();

        VCurrentDeck.SINGLETON_INSTANCE.getBtnDoSideboard().setVisible(true);
        ((FLabel) VCurrentDeck.SINGLETON_INSTANCE.getBtnDoSideboard())
            .setCommand(new Command() { private static final long serialVersionUID = 586398459150416750L;

            @Override
                public void execute() {
                    sideboardMode = !sideboardMode;
                    switchEditorMode(sideboardMode);
            } });

        this.controller.newModel();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#exit()
     */
    @Override
    public boolean exit() {
        // Override the submenu save choice - tell it to go to "constructed".
        Singletons.getModel().getPreferences().setPref(FPref.SUBMENU_CURRENTMENU, EDocID.HOME_CONSTRUCTED.toString());

        return SEditorIO.confirmSaveChanges();
    }
}
