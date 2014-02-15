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

import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import forge.UiCommand;
import forge.Singletons;
import forge.card.CardRulesPredicates;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gui.deckeditor.SEditorIO;
import forge.gui.framework.FScreen;
import forge.gui.toolbox.itemmanager.CardManager;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.gui.toolbox.itemmanager.views.ColumnDef;
import forge.gui.toolbox.itemmanager.views.GroupDef;
import forge.gui.toolbox.itemmanager.views.ItemColumn;
import forge.gui.toolbox.itemmanager.views.SColumnUtil;
import forge.item.PaperCard;
import forge.properties.ForgePreferences.FPref;
import forge.util.ItemPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    private final ItemPool<PaperCard> normalPool, avatarPool, planePool, schemePool;

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

        normalPool = ItemPool.createFrom(Singletons.getMagicDb().getCommonCards().getAllCards(), PaperCard.class);
        avatarPool = ItemPool.createFrom(Singletons.getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_VANGUARD, PaperCard.FN_GET_RULES)),PaperCard.class);
        planePool = ItemPool.createFrom(Singletons.getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_PLANE_OR_PHENOMENON, PaperCard.FN_GET_RULES)),PaperCard.class);
        schemePool = ItemPool.createFrom(Singletons.getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_SCHEME, PaperCard.FN_GET_RULES)),PaperCard.class);

        CardManager catalogManager = new CardManager(false); // TODO: restore the functionality of the "want uniques only" toggle
        CardManager deckManager = new CardManager(false); // IMPORTANT: must *always* show all cards in the deck, otherwise cards with different art get ignored!

        catalogManager.setCaption("Catalog");

        this.setCatalogManager(catalogManager);
        this.setDeckManager(deckManager);

        final Supplier<Deck> newCreator = new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        };

        this.controller = new DeckController<Deck>(Singletons.getModel().getDecks().getConstructed(), this, newCreator);
    }

    //=========== Overridden from ACEditorBase

    @Override
    protected CardLimit getCardLimit() {
        if (sectionMode == DeckSection.Avatar) {
            return CardLimit.Singleton;
        }
        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            return CardLimit.Default;
        }
        return CardLimit.None; //if not enforcing deck legality, don't enforce default limit
    }

    public static void onAddItems(ACEditorBase<PaperCard, Deck> editor, Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        DeckSection sectionMode = editor.sectionMode;
        DeckController<Deck> controller = editor.getDeckController();

        if (sectionMode == DeckSection.Commander || sectionMode == DeckSection.Avatar) {
            editor.getDeckManager().removeAllItems();
        }

        ItemPool<PaperCard> itemsToAdd = editor.getAllowedAdditions(items);
        if (itemsToAdd.isEmpty()) { return; }

        if (toAlternate) {
            switch (sectionMode) {
            case Main:
                controller.getModel().getOrCreate(DeckSection.Sideboard).addAll(itemsToAdd);
                break;
            default:
                return; //no other sections should support toAlternate
            }
        }
        else {
            editor.getDeckManager().addItems(itemsToAdd);
        }

        if (editor.getCatalogManager().isInfinite()) {
            //select all added cards in Catalog if infinite
            editor.getCatalogManager().selectItemEntrys(itemsToAdd);
        }
        else {
            //remove all added cards from Catalog if not infinite
            editor.getCatalogManager().removeItems(items);
        }

        controller.notifyModelChanged();
    }

    public static void onRemoveItems(ACEditorBase<PaperCard, Deck> editor, Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        DeckSection sectionMode = editor.sectionMode;
        DeckController<Deck> controller = editor.getDeckController();

        if (toAlternate) {
            switch (sectionMode) {
            case Main:
                controller.getModel().getOrCreate(DeckSection.Sideboard).addAll(items);
                break;
            case Sideboard:
                controller.getModel().get(DeckSection.Main).addAll(items);
                break;
            default:
                break; //no other sections should support toAlternate
            }
        }
        else {
            editor.getCatalogManager().addItems(items);
        }
        editor.getDeckManager().removeItems(items);

        controller.notifyModelChanged();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onAddItems()
     */
    @Override
    protected void onAddItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        onAddItems(this, items, toAlternate);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onRemoveItems()
     */
    @Override
    protected void onRemoveItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        onRemoveItems(this, items, toAlternate);
    }

    public static void buildAddContextMenu(EditorContextMenuBuilder cmb, DeckSection sectionMode) {
        switch (sectionMode) {
        case Main:
            cmb.addMoveItems("Add", "to deck");
            cmb.addMoveAlternateItems("Add", "to sideboard");
            break;
        case Sideboard:
            cmb.addMoveItems("Add", "to sideboard");
            break;
        case Commander:
            cmb.addMoveItems("Set", "as commander");
            break;
        case Avatar:
            cmb.addMoveItems("Set", "as avatar");
            break;
        case Schemes:
            cmb.addMoveItems("Add", "to scheme deck");
            break;
        case Planes:
            cmb.addMoveItems("Add", "to planar deck");
            break;
        }
    }

    public static void buildRemoveContextMenu(EditorContextMenuBuilder cmb, DeckSection sectionMode) {
        switch (sectionMode) {
        case Main:
            cmb.addMoveItems("Remove", "from deck");
            cmb.addMoveAlternateItems("Move", "to sideboard");
            break;
        case Sideboard:
            cmb.addMoveItems("Remove", "from sideboard");
            cmb.addMoveAlternateItems("Move", "to deck");
            break;
        case Commander:
            cmb.addMoveItems("Remove", "as commander");
            break;
        case Avatar:
            cmb.addMoveItems("Remove", "as avatar");
            break;
        case Schemes:
            cmb.addMoveItems("Remove", "from scheme deck");
            break;
        case Planes:
            cmb.addMoveItems("Remove", "from planar deck");
            break;
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#buildAddContextMenu()
     */
    @Override
    protected void buildAddContextMenu(EditorContextMenuBuilder cmb) {
        buildAddContextMenu(cmb, sectionMode);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#buildRemoveContextMenu()
     */
    @Override
    protected void buildRemoveContextMenu(EditorContextMenuBuilder cmb) {
        buildRemoveContextMenu(cmb, sectionMode);
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
        curindex = (curindex + 1) % allSections.size();
        sectionMode = allSections.get(curindex);

        final Map<ColumnDef, ItemColumn> lstCatalogCols = SColumnUtil.getCatalogDefaultColumns();

        switch(sectionMode) {
        case Main:
            lstCatalogCols.remove(ColumnDef.QUANTITY);
            this.getCatalogManager().setup(lstCatalogCols, true);
            this.getCatalogManager().setPool(normalPool, true);
            this.getDeckManager().setPool(this.controller.getModel().getMain());
            break;
        case Sideboard:
            lstCatalogCols.remove(ColumnDef.QUANTITY);
            this.getCatalogManager().setup(lstCatalogCols, true);
            this.getCatalogManager().setPool(normalPool, true);
            this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Sideboard));
            break;
        case Avatar:
            lstCatalogCols.remove(ColumnDef.QUANTITY);
            lstCatalogCols.remove(ColumnDef.COST);
            lstCatalogCols.remove(ColumnDef.COLOR);
            lstCatalogCols.remove(ColumnDef.CMC);
            lstCatalogCols.remove(ColumnDef.POWER);
            lstCatalogCols.remove(ColumnDef.TOUGHNESS);
            this.getCatalogManager().setup(lstCatalogCols, true);
            this.getCatalogManager().setPool(avatarPool, true);
            this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Avatar));
            break;
        case Planes:
            lstCatalogCols.remove(ColumnDef.QUANTITY);
            lstCatalogCols.remove(ColumnDef.COST);
            lstCatalogCols.remove(ColumnDef.CMC);
            lstCatalogCols.remove(ColumnDef.COLOR);
            lstCatalogCols.remove(ColumnDef.POWER);
            lstCatalogCols.remove(ColumnDef.TOUGHNESS);
            this.getCatalogManager().setup(lstCatalogCols, true);
            this.getCatalogManager().setPool(planePool,true);
            this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Planes));
            break;
        case Schemes:
            lstCatalogCols.remove(ColumnDef.QUANTITY);
            lstCatalogCols.remove(ColumnDef.CMC);
            lstCatalogCols.remove(ColumnDef.COST);
            lstCatalogCols.remove(ColumnDef.COLOR);
            lstCatalogCols.remove(ColumnDef.POWER);
            lstCatalogCols.remove(ColumnDef.TOUGHNESS);
            this.getCatalogManager().setup(lstCatalogCols, true);
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
        final Map<ColumnDef, ItemColumn> lstCatalogCols = SColumnUtil.getCatalogDefaultColumns();
        lstCatalogCols.remove(ColumnDef.QUANTITY);

        this.getCatalogManager().setup(lstCatalogCols, true);
        this.getDeckManager().setup(SColumnUtil.getDeckDefaultColumns(), GroupDef.CREATURE_SPELL_LAND, ColumnDef.CMC, 1);

        SItemManagerUtil.resetUI(this);

        this.getBtnCycleSection().setVisible(true);
        this.getBtnCycleSection().setCommand(new UiCommand() {
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
        return SEditorIO.confirmSaveChanges(FScreen.DECK_EDITOR_CONSTRUCTED, false); //ignore isClosing since screen can't close
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#resetUIChanges()
     */
    @Override
    public void resetUIChanges() {
    }
}
