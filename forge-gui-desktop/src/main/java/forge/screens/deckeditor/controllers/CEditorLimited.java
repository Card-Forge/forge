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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Supplier;

import forge.card.CardEdition;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.gui.UiCommand;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.deckeditor.AddBasicLandsDialog;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.deckeditor.views.VAllDecks;
import forge.screens.deckeditor.views.VBrawlDecks;
import forge.screens.deckeditor.views.VCommanderDecks;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.deckeditor.views.VDeckgen;
import forge.screens.deckeditor.views.VOathbreakerDecks;
import forge.screens.deckeditor.views.VTinyLeadersDecks;
import forge.screens.home.sanctioned.CSubmenuDraft;
import forge.screens.home.sanctioned.CSubmenuSealed;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FComboBox;
import forge.util.storage.IStorage;

/**
 * Child controller for limited deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 * 
 * @author Forge
 * @version $Id: DeckEditorCommon.java 12850 2011-12-26 14:55:09Z slapshot5 $
 */
public final class CEditorLimited extends CDeckEditor<DeckGroup> {

    private final DeckController<DeckGroup> controller;
    private DragCell constructedDecksParent = null;
    private DragCell commanderDecksParent = null;
    private DragCell oathbreakerDecksParent = null;
    private DragCell brawlDecksParent = null;
    private DragCell tinyLeadersDecksParent = null;
    private DragCell deckGenParent = null;
    private final List<DeckSection> allSections = new ArrayList<>();

    //========== Constructor

    /**
     * Child controller for limited deck editor UI.
     *
     * @param deckMap0 &emsp; {@link forge.deck.DeckGroup}<{@link forge.util.storage.IStorage}>
     */
    @SuppressWarnings("serial")
    public CEditorLimited(final IStorage<DeckGroup> deckMap0, final FScreen screen0, final CDetailPicture cDetailPicture0) {
        super(screen0, cDetailPicture0, GameType.Sealed);

        final CardManager catalogManager = new CardManager(cDetailPicture0, false, false);
        final CardManager deckManager = new CardManager(cDetailPicture0, false, false);

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
        this.controller = new DeckController<>(deckMap0, this, newCreator);

        getBtnAddBasicLands().setCommand(new UiCommand() {
            @Override
            public void run() {
                CEditorLimited.addBasicLands(CEditorLimited.this);
            }
        });

        allSections.add(DeckSection.Main);
        allSections.add(DeckSection.Conspiracy);

        this.getCbxSection().removeAllItems();
        for (DeckSection section : allSections) {
            this.getCbxSection().addItem(section);
        }
        this.getCbxSection().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                FComboBox cb = (FComboBox)actionEvent.getSource();
                DeckSection ds = (DeckSection)cb.getSelectedItem();
                setEditorMode(ds);
            }
        });
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
        this.getCatalogManager().setPool(getHumanDeck().getOrCreate(DeckSection.Sideboard));
        this.getDeckManager().setPool(getHumanDeck().getMain());
    }

    @Override
    public Boolean isSectionImportable(DeckSection section) {
        return section != DeckSection.Sideboard && allSections.contains(section);
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

    public static void addBasicLands(ACEditorBase<PaperCard, DeckGroup> editor) {
        Deck deck = editor.getHumanDeck();
        if (deck == null) { return; }

        Set<CardEdition> availableEditionCodes = new HashSet<>();
        for (PaperCard p : deck.getAllCardsInASinglePool().toFlatList()) {
            availableEditionCodes.add(FModel.getMagicDb().getEditions().get(p.getEdition()));
        }

        CardEdition defaultLandSet = CardEdition.Predicates.getRandomSetWithAllBasicLands(availableEditionCodes);

        AddBasicLandsDialog dialog = new AddBasicLandsDialog(deck, defaultLandSet);
        CardPool landsToAdd = dialog.show();
        if (landsToAdd != null) {
            editor.onAddItems(landsToAdd, false);
        }
    }

    public void setEditorMode(DeckSection sectionMode) {
        switch(sectionMode) {
            case Conspiracy:
                this.getCatalogManager().setup(ItemManagerConfig.DRAFT_CONSPIRACY);
                this.getDeckManager().setPool(getHumanDeck().getOrCreate(DeckSection.Conspiracy));
                break;
            case Main:
                this.getCatalogManager().setup(getScreen() == FScreen.DECK_EDITOR_DRAFT ? ItemManagerConfig.DRAFT_POOL : ItemManagerConfig.SEALED_POOL);
                this.getDeckManager().setPool(getHumanDeck().getOrCreate(DeckSection.Main));
                break;
            default:
                break;
        }

        this.sectionMode = sectionMode;
        this.controller.updateCaptions();
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
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setEnabled(false);
        this.getCbxSection().setVisible(true);

        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        constructedDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);
        commanderDecksParent = removeTab(VCommanderDecks.SINGLETON_INSTANCE);
        oathbreakerDecksParent = removeTab(VOathbreakerDecks.SINGLETON_INSTANCE);
        brawlDecksParent = removeTab(VBrawlDecks.SINGLETON_INSTANCE);
        tinyLeadersDecksParent = removeTab(VTinyLeadersDecks.SINGLETON_INSTANCE);
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
        if (constructedDecksParent != null) {
            constructedDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
        }
        if (commanderDecksParent != null) {
            commanderDecksParent.addDoc(VCommanderDecks.SINGLETON_INSTANCE);
        }
        if (oathbreakerDecksParent != null) {
            oathbreakerDecksParent.addDoc(VOathbreakerDecks.SINGLETON_INSTANCE);
        }
        if (brawlDecksParent!= null) {
            brawlDecksParent.addDoc(VBrawlDecks.SINGLETON_INSTANCE);
        }
        if (tinyLeadersDecksParent != null) {
            tinyLeadersDecksParent.addDoc(VTinyLeadersDecks.SINGLETON_INSTANCE);
        }
    }
}
