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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicates;
import com.google.common.base.Supplier;

import forge.Command;
import forge.Singletons;
import forge.card.CardRulesPredicates;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gui.deckeditor.SEditorIO;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.framework.FScreen;
import forge.gui.toolbox.itemmanager.CardManager;
import forge.gui.toolbox.itemmanager.SItemManagerIO;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.gui.toolbox.itemmanager.SItemManagerIO.EditorPreference;
import forge.gui.toolbox.itemmanager.views.SColumnUtil;
import forge.gui.toolbox.itemmanager.views.TableColumnInfo;
import forge.gui.toolbox.itemmanager.views.SColumnUtil.ColumnName;
import forge.item.PaperCard;
import forge.item.InventoryItem;
import forge.util.ItemPool;
import forge.util.ItemPoolView;

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
public final class CEditorConstructed extends ACEditorBase<PaperCard, Deck> {
    private final DeckController<Deck> controller;
    //private boolean sideboardMode = false;
    
    private List<DeckSection> allSections = new ArrayList<DeckSection>();
    private DeckSection sectionMode = DeckSection.Main;
    
    private final ItemPoolView<PaperCard> avatarPool;
    private final ItemPoolView<PaperCard> planePool;
    private final ItemPoolView<PaperCard> schemePool;
    
    //=========== Constructor
    /**
     * Child controller for constructed deck editor UI.
     * This is the least restrictive mode;
     * all cards are available.
     */
    public CEditorConstructed() {
        super(FScreen.DECK_EDITOR_CONSTRUCTED);
        
        allSections.add(DeckSection.Main);
        allSections.add(DeckSection.Sideboard);
        allSections.add(DeckSection.Avatar);
        allSections.add(DeckSection.Schemes);
        allSections.add(DeckSection.Planes);
        //allSections.add(DeckSection.Commander);

        avatarPool = ItemPool.createFrom(Singletons.getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_VANGUARD, PaperCard.FN_GET_RULES)),PaperCard.class);
        planePool = ItemPool.createFrom(Singletons.getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_PLANE_OR_PHENOMENON, PaperCard.FN_GET_RULES)),PaperCard.class);
        schemePool = ItemPool.createFrom(Singletons.getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_SCHEME, PaperCard.FN_GET_RULES)),PaperCard.class);
        
        boolean wantUnique = SItemManagerIO.getPref(EditorPreference.display_unique_only);

        this.setCatalogManager(new CardManager(wantUnique));
        this.setDeckManager(new CardManager(wantUnique));

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
    public void addCard(InventoryItem item, boolean toAlternate, int qty) {
        if ((item == null) || !(item instanceof PaperCard)) {
            return;
        }

        if (sectionMode == DeckSection.Avatar) {
            for(Map.Entry<PaperCard, Integer> cp : getDeckManager().getPool()) {
                getDeckManager().removeItem(cp.getKey(), cp.getValue());
            }
        }

        final PaperCard card = (PaperCard) item;
        if (toAlternate) {
            if (sectionMode != DeckSection.Sideboard) {
                controller.getModel().getOrCreate(DeckSection.Sideboard).add(card, qty);
            }
        } else {
            getDeckManager().addItem(card, qty);
        }
        // if not in sideboard mode, "remove" 0 cards in order to re-show the selected card
        this.getCatalogManager().removeItem(card, sectionMode == DeckSection.Sideboard ? qty : 0);
        
        this.controller.notifyModelChanged();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#removeCard()
     */
    @Override
    public void removeCard(InventoryItem item, boolean toAlternate, int qty) {
        if ((item == null) || !(item instanceof PaperCard)) {
            return;
        }

        final PaperCard card = (PaperCard) item;
        if (toAlternate) {
            if (sectionMode != DeckSection.Sideboard) {
                controller.getModel().getOrCreate(DeckSection.Sideboard).add(card, qty);
            } else {
                // "added" to library, but library will be recalculated when it is shown again
            }
        } else if (sectionMode == DeckSection.Sideboard) {
            this.getCatalogManager().addItem(card, qty);
        }
        this.getDeckManager().removeItem(card, qty);
        this.controller.notifyModelChanged();
    }

    @Override
    public void buildAddContextMenu(ContextMenuBuilder cmb) {
        cmb.addMoveItems(sectionMode == DeckSection.Sideboard ? "Move" : "Add", "card", "cards", sectionMode == DeckSection.Sideboard ? "to sideboard" : "to deck");
        cmb.addMoveAlternateItems(sectionMode == DeckSection.Sideboard ? "Remove" : "Add", "card", "cards", sectionMode == DeckSection.Sideboard ? "from deck" : "to sideboard");
        cmb.addTextFilterItem();
    }
    
    @Override
    public void buildRemoveContextMenu(ContextMenuBuilder cmb) {
        cmb.addMoveItems(sectionMode == DeckSection.Sideboard ? "Move" : "Remove", "card", "cards", sectionMode == DeckSection.Sideboard ? "to deck" : "from deck");
        cmb.addMoveAlternateItems(sectionMode == DeckSection.Sideboard ? "Remove" : "Move", "card", "cards", sectionMode == DeckSection.Sideboard ? "from sideboard" : "to sideboard");
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#resetTables()
     */
    @Override
    public void resetTables() {
        // Constructed mode can use all cards, no limitations.
        this.getCatalogManager().setPool(ItemPool.createFrom(Singletons.getMagicDb().getCommonCards().getAllCards(), PaperCard.class), true);
        this.getDeckManager().setPool(this.controller.getModel().getMain());
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
    public void cycleEditorMode() {
        int curindex = allSections.indexOf(sectionMode);

        curindex = curindex == (allSections.size()-1) ? 0 : curindex+1;
        sectionMode = allSections.get(curindex);
        
        final List<TableColumnInfo<InventoryItem>> lstCatalogCols = SColumnUtil.getCatalogDefaultColumns();

        String title = "";
        String tabtext = "";
        Boolean showOptions = true;
        switch(sectionMode) {
        case Main:
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));
            this.getCatalogManager().getTable().setAvailableColumns(lstCatalogCols);
            this.getCatalogManager().setPool(ItemPool.createFrom(Singletons.getMagicDb().getCommonCards().getAllCards(), PaperCard.class), true);
            this.getDeckManager().setPool(this.controller.getModel().getMain());
            showOptions = true;
            title = "Title: ";
            tabtext = "Main Deck";
            break;
        case Sideboard:
            this.getCatalogManager().getTable().setAvailableColumns(lstCatalogCols);
            this.getCatalogManager().setPool(this.controller.getModel().getMain());
            this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Sideboard));
            showOptions = false;
            title = "Sideboard";
            tabtext = "Card Catalog";
            break;
        case Avatar:
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_COST));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_COLOR));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_CMC));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_POWER));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_TOUGHNESS));
            this.getCatalogManager().getTable().setAvailableColumns(lstCatalogCols);
            this.getCatalogManager().setPool(avatarPool, true);
            this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Avatar));
            showOptions = false;
            title = "Vanguard";
            tabtext = "Card Catalog";
            break;
        case Planes:
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_COST));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_CMC));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_COLOR));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_POWER));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_TOUGHNESS));
            this.getCatalogManager().getTable().setAvailableColumns(lstCatalogCols);
            this.getCatalogManager().setPool(planePool,true);
            this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Planes));
            showOptions = false;
            title = "Planar";
            tabtext = "Card Catalog";
            break;
        case Schemes:
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_CMC));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_COST));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_COLOR));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_POWER));
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_TOUGHNESS));
            this.getCatalogManager().getTable().setAvailableColumns(lstCatalogCols);
            this.getCatalogManager().setPool(schemePool,true);
            this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Schemes));
            showOptions = false;
            title = "Scheme";
            tabtext = "Card Catalog";
            break;
        case Commander:
            break; //do nothing for Commander here
        }

        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText(tabtext);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().setVisible(showOptions);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().setVisible(showOptions);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSave().setVisible(showOptions);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().setVisible(showOptions);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnPrintProxies().setVisible(showOptions);
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setVisible(showOptions);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnImport().setVisible(showOptions);
        VCurrentDeck.SINGLETON_INSTANCE.getLblTitle().setText(title);

        this.controller.notifyModelChanged();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#show(forge.Command)
     */
    @SuppressWarnings("serial")
    @Override
    public void update() {
        final List<TableColumnInfo<InventoryItem>> lstCatalogCols = SColumnUtil.getCatalogDefaultColumns();
        lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));

        this.getCatalogManager().getTable().setup(lstCatalogCols);
        this.getDeckManager().getTable().setup(SColumnUtil.getDeckDefaultColumns());

        SItemManagerUtil.resetUI(this);

        this.getBtnCycleSection().setVisible(true);
        this.getBtnCycleSection().setCommand(new Command() {
            @Override
            public void run() {
                cycleEditorMode();
            }
        });

        this.controller.refreshModel();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#canSwitchAway()
     */
    @Override
    public boolean canSwitchAway(boolean isClosing) {
        return SEditorIO.confirmSaveChanges(FScreen.DECK_EDITOR_CONSTRUCTED);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#resetUIChanges()
     */
    @Override
    public void resetUIChanges() {
    }
}
