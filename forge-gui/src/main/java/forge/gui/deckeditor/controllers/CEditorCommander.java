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
import java.util.Arrays;
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
import forge.gui.deckeditor.views.VAllDecks;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.deckeditor.views.VDeckgen;
import forge.gui.framework.DragCell;
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
 * @version $Id: CEditorCommander.java 18430 2012-11-27 22:42:36Z Hellfish $
 */
public final class CEditorCommander extends ACEditorBase<PaperCard, Deck> {
    private final DeckController<Deck> controller;
    private DragCell allDecksParent = null;
    private DragCell deckGenParent = null;

    private List<DeckSection> allSections = new ArrayList<DeckSection>();
    private DeckSection sectionMode = DeckSection.Main;
    private final ItemPoolView<PaperCard> commanderPool;
    private final ItemPoolView<PaperCard> normalPool;

    //=========== Constructor
    /**
     * Child controller for constructed deck editor UI.
     * This is the least restrictive mode;
     * all cards are available.
     */
    public CEditorCommander() {
        super(FScreen.DECK_EDITOR_COMMANDER);
        allSections.add(DeckSection.Main);
        allSections.add(DeckSection.Sideboard);
        allSections.add(DeckSection.Commander);
        
        commanderPool = ItemPool.createFrom(Singletons.getMagicDb().getCommonCards().getAllCards(Predicates.compose(Predicates.and(CardRulesPredicates.Presets.IS_CREATURE,CardRulesPredicates.Presets.IS_LEGENDARY), PaperCard.FN_GET_RULES)),PaperCard.class);
        normalPool = ItemPool.createFrom(Singletons.getMagicDb().getCommonCards().getAllCards(), PaperCard.class);
        
        boolean wantUnique = SItemManagerIO.getPref(EditorPreference.display_unique_only);

        this.setCatalogManager(new CardManager(wantUnique));
        this.setDeckManager(new CardManager(wantUnique));

        final Supplier<Deck> newCreator = new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        };
        this.controller = new DeckController<Deck>(Singletons.getModel().getDecks().getCommander(), this, newCreator);
    }

    //=========== Overridden from ACEditorBase

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onAddItems()
     */
    @Override
    protected void onAddItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        if (toAlternate) { return; }

        List<String> limitExceptions = Arrays.asList(new String[]{"Relentless Rats", "Shadowborn Apostle"});
        
        List<Entry<PaperCard, Integer>> itemList = new ArrayList<Entry<PaperCard, Integer>>();
        
        for (Entry<PaperCard, Integer> item : items) {
            PaperCard card = item.getKey();
            if ((controller.getModel().getMain().contains(card)
                    || controller.getModel().getOrCreate(DeckSection.Sideboard).contains(card)
                    || controller.getModel().getOrCreate(DeckSection.Commander).contains(card))
                    && !(card.getRules().getType().isBasic() || limitExceptions.contains(card.getName()))) {
                continue;
            }
            itemList.add(item);
        }

        if (itemList.isEmpty()) { return; }

        if (sectionMode == DeckSection.Commander) {
            this.getDeckManager().removeItems(getDeckManager().getPool());
        }

        this.getDeckManager().addItems(itemList);
        this.getCatalogManager().selectItemEntrys(itemList); //just select all removed cards in Catalog
        this.controller.notifyModelChanged();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onRemoveItems()
     */
    @Override
    protected void onRemoveItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        if (toAlternate) { return; }

        this.getDeckManager().removeItems(items);
        this.getCatalogManager().selectItemEntrys(items); //just select all removed cards in Catalog
        this.controller.notifyModelChanged();
    }

    @Override
    public void buildAddContextMenu(ContextMenuBuilder cmb) {
        cmb.addMoveItems("Move", "card", "cards", "to deck");
    }
    
    @Override
    public void buildRemoveContextMenu(ContextMenuBuilder cmb) {
        cmb.addMoveItems("Move", "card", "cards", "to sideboard");
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#updateView()
     */
    @Override
    public void resetTables() {
        this.getCatalogManager().setPool(normalPool,false);
        this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Main));
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

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#show(forge.Command)
     */
    @Override
    public void update() {
        final List<TableColumnInfo<InventoryItem>> lstCatalogCols = SColumnUtil.getCatalogDefaultColumns();
        lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));

        this.getCatalogManager().getTable().setup(lstCatalogCols);
        this.getDeckManager().getTable().setup(SColumnUtil.getDeckDefaultColumns());

        SItemManagerUtil.resetUI(this);
        
        this.getBtnRemove4().setVisible(false);
        this.getBtnAdd4().setVisible(false);
        this.getBtnCycleSection().setVisible(true);
        this.getBtnCycleSection().setCommand(new Command() {
            private static final long serialVersionUID = -9082606944024479599L;

            @Override
            public void run() {
                cycleEditorMode();
            }
        });

        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        allDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);
        
        this.controller.refreshModel();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#canSwitchAway()
     */
    @Override
    public boolean canSwitchAway(boolean isClosing) {
        return SEditorIO.confirmSaveChanges(FScreen.DECK_EDITOR_COMMANDER);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#resetUIChanges()
     */
    @Override
    public void resetUIChanges() {
        //Re-add tabs
        if (deckGenParent != null) {
            deckGenParent.addDoc(VDeckgen.SINGLETON_INSTANCE);
        }
        if (allDecksParent != null) {
            allDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
        }
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
        switch(sectionMode)
        {
            case Main:
                lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));
                this.getCatalogManager().getTable().setAvailableColumns(lstCatalogCols);
                this.getCatalogManager().setPool(normalPool, false);
                this.getDeckManager().setPool(this.controller.getModel().getMain());
                showOptions = true;
                title = "Title: ";
                tabtext = "Main Deck";
                break;
            case Sideboard:
                lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));
                this.getCatalogManager().getTable().setAvailableColumns(lstCatalogCols);
                this.getCatalogManager().setPool(normalPool, false);
                this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Sideboard));
                showOptions = false;
                title = "Sideboard";
                tabtext = "Card Catalog";
                break;
            case Commander:
                lstCatalogCols.remove(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));
                this.getCatalogManager().getTable().setAvailableColumns(lstCatalogCols);
                this.getCatalogManager().setPool(commanderPool, true);
                this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Commander));
                showOptions = false;
                title = "Commander";
                tabtext = "Card Catalog";
                break;
            default:
                break;
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
}
