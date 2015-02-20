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
package forge.screens.deckeditor.controllers;

import com.google.common.base.Predicates;
import com.google.common.base.Supplier;

import forge.UiCommand;
import forge.card.CardRulesPredicates;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.deckeditor.views.VAllDecks;
import forge.screens.deckeditor.views.VDeckgen;
import forge.screens.match.controllers.CDetailPicture;
import forge.util.ItemPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
    private final ItemPool<PaperCard> commanderPool;
    private final ItemPool<PaperCard> normalPool;

    //=========== Constructor
    /**
     * Child controller for constructed deck editor UI.
     * This is the least restrictive mode;
     * all cards are available.
     */
    public CEditorCommander(final CDetailPicture cDetailPicture) {
        super(FScreen.DECK_EDITOR_COMMANDER, cDetailPicture);
        allSections.add(DeckSection.Main);
        allSections.add(DeckSection.Sideboard);
        allSections.add(DeckSection.Commander);

        commanderPool = ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.CAN_BE_COMMANDER, PaperCard.FN_GET_RULES)),PaperCard.class);
        normalPool = ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getAllCards(), PaperCard.class);

        CardManager catalogManager = new CardManager(getCDetailPicture(), true);
        CardManager deckManager = new CardManager(getCDetailPicture(), true);

        catalogManager.setCaption("Catalog");

        this.setCatalogManager(catalogManager);
        this.setDeckManager(deckManager);

        final Supplier<Deck> newCreator = new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        };
        this.controller = new DeckController<Deck>(FModel.getDecks().getCommander(), this, newCreator);
    }

    //=========== Overridden from ACEditorBase

    @Override
    protected CardLimit getCardLimit() {
        return CardLimit.Singleton;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onAddItems()
     */
    @Override
    protected void onAddItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        CEditorConstructed.onAddItems(this, items, toAlternate);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onRemoveItems()
     */
    @Override
    protected void onRemoveItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        CEditorConstructed.onRemoveItems(this, items, toAlternate);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#buildAddContextMenu()
     */
    @Override
    protected void buildAddContextMenu(EditorContextMenuBuilder cmb) {
        CEditorConstructed.buildAddContextMenu(cmb, sectionMode);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#buildRemoveContextMenu()
     */
    @Override
    protected void buildRemoveContextMenu(EditorContextMenuBuilder cmb) {
        CEditorConstructed.buildRemoveContextMenu(cmb, sectionMode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#updateView()
     */
    @Override
    public void resetTables() {
        this.sectionMode = DeckSection.Main;
        this.getCatalogManager().setPool(normalPool, true);
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
        this.getCatalogManager().setup(ItemManagerConfig.CARD_CATALOG);
        this.getDeckManager().setup(ItemManagerConfig.DECK_EDITOR);

        resetUI();

        this.getBtnRemove4().setVisible(false);
        this.getBtnAdd4().setVisible(false);
        this.getBtnCycleSection().setVisible(true);
        this.getBtnCycleSection().setCommand(new UiCommand() {
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
        return SEditorIO.confirmSaveChanges(FScreen.DECK_EDITOR_COMMANDER, isClosing);
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
        curindex = (curindex + 1) % allSections.size();
        sectionMode = allSections.get(curindex);

        switch(sectionMode) {
            case Main:
                this.getCatalogManager().setup(ItemManagerConfig.CARD_CATALOG);
                this.getCatalogManager().setPool(normalPool, true);
                this.getDeckManager().setPool(this.controller.getModel().getMain());
                break;
            case Sideboard:
                this.getCatalogManager().setup(ItemManagerConfig.CARD_CATALOG);
                this.getCatalogManager().setPool(normalPool, true);
                this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Sideboard));
                break;
            case Commander:
                this.getCatalogManager().setup(ItemManagerConfig.COMMANDER_POOL);
                this.getCatalogManager().setPool(commanderPool, true);
                this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Commander));
                break;
            default:
                break;
        }

        this.controller.updateCaptions();
    }
}
