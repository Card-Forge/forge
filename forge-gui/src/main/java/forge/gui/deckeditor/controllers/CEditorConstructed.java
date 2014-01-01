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
import java.util.Map.Entry;

import com.google.common.base.Predicates;
import com.google.common.base.Supplier;

import forge.Command;
import forge.Singletons;
import forge.card.CardRulesPredicates;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gui.deckeditor.SEditorIO;
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
    private final List<DeckSection> allSections = new ArrayList<DeckSection>();
    private final ItemPoolView<PaperCard> normalPool, avatarPool, planePool, schemePool;
    
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

        normalPool = ItemPool.createFrom(Singletons.getMagicDb().getCommonCards().getAllCards(), PaperCard.class);
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
     * @see forge.gui.deckeditor.ACEditorBase#onAddItems()
     */
    @Override
    protected void onAddItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        if (sectionMode == DeckSection.Avatar) {
            getDeckManager().removeItems(getDeckManager().getPool());
        }

        if (toAlternate) {
            if (sectionMode != DeckSection.Sideboard) {
                controller.getModel().getOrCreate(DeckSection.Sideboard).addAll(items);
            }
        }
        else {
            getDeckManager().addItems(items);
        }
        if (sectionMode == DeckSection.Sideboard) {
            this.getCatalogManager().removeItems(items);
        }
        else { //if not in sideboard mode, just select all added cards in Catalog
            List<PaperCard> cards = new ArrayList<PaperCard>();
            for (Entry<PaperCard, Integer> itemEntry : items) {
                cards.add(itemEntry.getKey());
            }
            this.getCatalogManager().setSelectedItems(cards);
        }
        this.controller.notifyModelChanged();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onRemoveItems()
     */
    @Override
    protected void onRemoveItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        if (toAlternate) {
            if (sectionMode != DeckSection.Sideboard) {
                controller.getModel().getOrCreate(DeckSection.Sideboard).addAll(items);
            }
            else {
                // "added" to library, but library will be recalculated when it is shown again
            }
        }
        else if (sectionMode == DeckSection.Sideboard) {
            this.getCatalogManager().addItems(items);
        }
        else { //if not in sideboard mode, just select all removed cards in Catalog
            this.getCatalogManager().selectItemEntrys(items);
        }
        this.getDeckManager().removeItems(items);
        this.controller.notifyModelChanged();
    }

    @Override
    public void buildAddContextMenu(ContextMenuBuilder cmb) {
        cmb.addMoveItems(sectionMode == DeckSection.Sideboard ? "Move" : "Add", "card", "cards", sectionMode == DeckSection.Sideboard ? "to sideboard" : "to deck");
        cmb.addMoveAlternateItems(sectionMode == DeckSection.Sideboard ? "Remove" : "Add", "card", "cards", sectionMode == DeckSection.Sideboard ? "from deck" : "to sideboard");
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
        this.sectionMode = DeckSection.Main;
        this.getCatalogManager().setPool(normalPool, true);
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

        switch(sectionMode) {
        case Main:
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));
            this.getCatalogManager().getTable().setAvailableColumns(lstCatalogCols);
            this.getCatalogManager().setPool(normalPool, true);
            this.getDeckManager().setPool(this.controller.getModel().getMain());
            break;
        case Sideboard:
            lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));
            this.getCatalogManager().getTable().setAvailableColumns(lstCatalogCols);
            this.getCatalogManager().setPool(normalPool, true);
            this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Sideboard));
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
            break;
        case Commander:
            break; //do nothing for Commander here
        }

        this.controller.updateCaptions();
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
