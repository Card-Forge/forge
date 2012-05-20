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

// import java.awt.Font;
import java.util.List;

import forge.AllZone;
import forge.Constant;
import forge.deck.Deck;
import forge.gui.deckeditor.SEditorIO;
import forge.gui.deckeditor.tables.DeckController;
import forge.gui.deckeditor.tables.SColumnUtil;
import forge.gui.deckeditor.tables.SColumnUtil.ColumnName;
import forge.gui.deckeditor.tables.TableColumnInfo;
import forge.gui.deckeditor.tables.TableView;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.home.quest.CSubmenuQuestDecks;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.quest.QuestController;
import forge.util.closures.Lambda0;

//import forge.quest.data.QuestBoosterPack;

/**
 * Child controller for quest deck editor UI.
 * <br><br>
 * Card catalog and decks are drawn from a QuestController object.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 * 
 * @author Forge
 * @version $Id$
 */
public final class CEditorQuest extends ACEditorBase<CardPrinted, Deck> {
    private final QuestController questData;
    private final DeckController<Deck> controller;

    /**
     * Child controller for quest deck editor UI.
     * <br><br>
     * Card catalog and decks are drawn from a QuestController object.
     * 
     * @param questData0 &emsp; {@link forge.quest.QuestController}
     */
    public CEditorQuest(final QuestController questData0) {
        this.questData = questData0;

        final TableView<CardPrinted> tblCatalog = new TableView<CardPrinted>(false, CardPrinted.class);
        final TableView<CardPrinted> tblDeck = new TableView<CardPrinted>(false, CardPrinted.class);

        VCardCatalog.SINGLETON_INSTANCE.setTableView(tblCatalog.getTable());
        VCurrentDeck.SINGLETON_INSTANCE.setTableView(tblDeck.getTable());

        this.setTableCatalog(tblCatalog);
        this.setTableDeck(tblDeck);

        final Lambda0<Deck> newCreator = new Lambda0<Deck>() {
            @Override
            public Deck apply() {
                return new Deck();
            }
        };
        this.controller = new DeckController<Deck>(questData0.getMyDecks(), this, newCreator);
    }

    /**
     * Adds any card to the catalog and data pool.
     * 
     * @param card {@link forge.item.CardPrinted}
     */
    public void addCheatCard(final CardPrinted card) {
        this.getTableCatalog().addCard(card);
        this.questData.getCards().getCardpool().add(card);
    }

    //=========== Overridden from ACEditorBase

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#addCard()
     */
    @Override
    public void addCard() {
        final InventoryItem item = this.getTableCatalog().getSelectedCard();
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
        this.getTableCatalog().removeCard(card);
        this.getTableDeck().addCard(card);
        this.controller.notifyModelChanged();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#removeCard()
     */
    @Override
    public void removeCard() {
        final InventoryItem item = this.getTableDeck().getSelectedCard();
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
        this.getTableCatalog().addCard(card);
        this.getTableDeck().removeCard(card);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#updateView()
     */
    @Override
    public void resetTables() {
        final Deck deck = this.controller.getModel();

        final ItemPool<CardPrinted> cardpool = new ItemPool<CardPrinted>(CardPrinted.class);
        cardpool.addAll(this.questData.getCards().getCardpool());
        // remove bottom cards that are in the deck from the card pool
        cardpool.removeAll(deck.getMain());
        // show cards, makes this user friendly
        this.getTableCatalog().setDeck(cardpool);
        this.getTableDeck().setDeck(deck.getMain());
    }

    //=========== Overridden from ACEditorBase

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#getController()
     */
    @Override
    public DeckController<Deck> getDeckController() {
        return this.controller;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#show(forge.Command)
     */
    @Override
    public void init() {
        final List<TableColumnInfo<InventoryItem>> columnsCatalog = SColumnUtil.getCatalogDefaultColumns();
        final List<TableColumnInfo<InventoryItem>> columnsDeck = SColumnUtil.getDeckDefaultColumns();

        // Add "new" column in catalog and deck
        columnsCatalog.add(SColumnUtil.getColumn(ColumnName.CAT_NEW));

        columnsCatalog.get(columnsCatalog.size() - 1).setSortAndDisplayFunctions(
                this.questData.getCards().getFnNewCompare(),
                this.questData.getCards().getFnNewGet());

        columnsDeck.add(SColumnUtil.getColumn(ColumnName.DECK_NEW));

        columnsDeck.get(columnsDeck.size() - 1).setSortAndDisplayFunctions(
                this.questData.getCards().getFnNewCompare(),
                this.questData.getCards().getFnNewGet());

        this.getTableCatalog().setup(VCardCatalog.SINGLETON_INSTANCE, columnsCatalog);
        this.getTableDeck().setup(VCurrentDeck.SINGLETON_INSTANCE, columnsDeck);

        Deck deck = Constant.Runtime.HUMAN_DECK[0] == null ? null : this.questData.getMyDecks().get(
                Constant.Runtime.HUMAN_DECK[0].getName());

        if (deck == null) {
            deck = new Deck();
        }

        this.getDeckController().setModel(deck);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#exit()
     */
    @Override
    public boolean exit() {
        final boolean okToExit = SEditorIO.confirmSaveChanges();
        if (okToExit) {
            AllZone.getQuest().save();
            CSubmenuQuestDecks.SINGLETON_INSTANCE.update();
        }
        return okToExit;
    }
}
