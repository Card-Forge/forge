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

import com.google.common.base.Supplier;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.deckeditor.views.VAllDecks;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.deckeditor.views.VDeckgen;
import forge.screens.home.sanctioned.CSubmenuDraft;
import forge.screens.home.sanctioned.CSubmenuSealed;
import forge.screens.match.controllers.CDetailPicture;
import forge.util.storage.IStorage;

import java.util.Map.Entry;

/**
 * Child controller for limited deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 * 
 * @author Forge
 * @version $Id: DeckEditorCommon.java 12850 2011-12-26 14:55:09Z slapshot5 $
 */
public final class CEditorLimited extends ACEditorBase<PaperCard, DeckGroup> {

    private final DeckController<DeckGroup> controller;
    private DragCell allDecksParent = null;
    private DragCell deckGenParent = null;

    //========== Constructor

    /**
     * Child controller for limited deck editor UI.
     *
     * @param deckMap0 &emsp; {@link forge.deck.DeckGroup}<{@link forge.util.storage.IStorage}>
     */
    public CEditorLimited(final IStorage<DeckGroup> deckMap0, final FScreen screen0, final CDetailPicture cDetailPicture) {
        super(screen0, cDetailPicture);

        final CardManager catalogManager = new CardManager(getCDetailPicture(), false);
        final CardManager deckManager = new CardManager(getCDetailPicture(), false);

        catalogManager.setCaption("Sideboard");

        catalogManager.setAlwaysNonUnique(true);
        deckManager.setAlwaysNonUnique(true);

        this.setCatalogManager(catalogManager);
        this.setDeckManager(deckManager);

        final Supplier<DeckGroup> newCreator = new Supplier<DeckGroup>() {
            @Override
            public DeckGroup get() {
                return new DeckGroup("");
            }
        };
        this.controller = new DeckController<DeckGroup>(deckMap0, this, newCreator);
    }

    /**
     * @param model
     * @return
     */
    private Deck getSelectedDeck(final DeckGroup model) {
        return model.getHumanDeck();
    }

    //========== Overridden from ACEditorBase

    @Override
    protected CardLimit getCardLimit() {
        return CardLimit.None;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onAddItems()
     */
    @Override
    protected void onAddItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        if (toAlternate) { return; }

        // update view
        this.getDeckManager().addItems(items);
        this.getCatalogManager().removeItems(items);
        this.getDeckController().notifyModelChanged();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onRemoveItems()
     */
    @Override
    protected void onRemoveItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        if (toAlternate) { return; }

        // update view
        this.getCatalogManager().addItems(items);
        this.getDeckManager().removeItems(items);
        this.getDeckController().notifyModelChanged();
    }

    @Override
    protected void buildAddContextMenu(EditorContextMenuBuilder cmb) {
        cmb.addMoveItems("Move", "to deck");
    }

    @Override
    protected void buildRemoveContextMenu(EditorContextMenuBuilder cmb) {
        cmb.addMoveItems("Move", "to sideboard");
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#updateView()
     */
    @Override
    public void resetTables() {
        final Deck toEdit = this.getSelectedDeck(this.controller.getModel());
        this.getCatalogManager().setPool(toEdit.getOrCreate(DeckSection.Sideboard));
        this.getDeckManager().setPool(toEdit.getMain());
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#getController()
     */
    @Override
    public DeckController<DeckGroup> getDeckController() {
        return this.controller;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#show(forge.Command)
     */
    @Override
    public void update() {
        this.getCatalogManager().setup(getScreen() == FScreen.DECK_EDITOR_DRAFT ? ItemManagerConfig.DRAFT_POOL : ItemManagerConfig.SEALED_POOL);
        this.getDeckManager().setup(ItemManagerConfig.DECK_EDITOR);

        resetUI();

        VCurrentDeck.SINGLETON_INSTANCE.getBtnPrintProxies().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setEnabled(false);

        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        allDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#canSwitchAway()
     */
    @Override
    public boolean canSwitchAway(boolean isClosing) {
        return SEditorIO.confirmSaveChanges(getScreen(), isClosing);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#resetUIChanges()
     */
    @Override
    public void resetUIChanges() {
        CSubmenuDraft.SINGLETON_INSTANCE.update();
        CSubmenuSealed.SINGLETON_INSTANCE.update();

        //Re-add tabs
        if (deckGenParent != null) {
            deckGenParent.addDoc(VDeckgen.SINGLETON_INSTANCE);
        }
        if (allDecksParent != null) {
            allDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
        }
    }
}
