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

import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.limited.BoosterDraft;
import forge.limited.IBoosterDraft;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.views.*;
import forge.screens.home.sanctioned.CSubmenuDraft;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FOptionPane;
import forge.util.ItemPool;
import forge.util.Localizer;

import java.util.HashSet;
import java.util.Map.Entry;

/**
 * Updates the deck editor UI as necessary draft selection mode.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 * @author Forge
 * @version $Id: CEditorDraftingProcess.java 24872 2014-02-17 07:35:47Z drdev $
 */
public class CEditorDraftingProcess extends ACEditorBase<PaperCard, DeckGroup> {
    private IBoosterDraft boosterDraft;

    private String ccAddLabel = Localizer.getInstance().getMessage("lblAddcard");
    private DragCell constructedDecksParent = null;
    private DragCell commanderDecksParent = null;
    private DragCell oathbreakerDecksParent = null;
    private DragCell brawlDecksParent = null;
    private DragCell tinyLeadersDecksParent = null;
    private DragCell deckGenParent = null;
    private boolean saved = false;
    private final Localizer localizer = Localizer.getInstance();

    //========== Constructor

    /**
     * Updates the deck editor UI as necessary draft selection mode.
     */
    public CEditorDraftingProcess(final CDetailPicture cDetailPicture0) {
        super(FScreen.DRAFTING_PROCESS, cDetailPicture0, GameType.Draft);

        final CardManager catalogManager = new CardManager(cDetailPicture0, false, false);
        final CardManager deckManager = new CardManager(cDetailPicture0, false, false);

        //hide filters and options panel so more of pack is visible by default
        catalogManager.setHideViewOptions(1, true);

        deckManager.setCaption(localizer.getMessage("lblDraftPicks"));

        catalogManager.setAlwaysNonUnique(true);
        deckManager.setAlwaysNonUnique(true);

        this.setCatalogManager(catalogManager);
        this.setDeckManager(deckManager);
    }

    /**
     * Show GuiBase.getInterface().
     *
     * @param inBoosterDraft
     *            the in_booster draft
     */
    public final void showGui(final IBoosterDraft inBoosterDraft) {
        this.boosterDraft = inBoosterDraft;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onAddItems()
     */
    @Override
    protected void onAddItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        if (toAlternate) { return; }

        // can only draft one at a time, regardless of the requested quantity
        PaperCard card = items.iterator().next().getKey();
        this.getDeckManager().addItem(card, 1);

        // get next booster pack
        this.boosterDraft.setChoice(card);

        boolean nextChoice = this.boosterDraft.hasNextChoice();
        ItemPool<PaperCard> pool = null;
        if (nextChoice) {
            pool = this.boosterDraft.nextChoice();
            nextChoice = pool != null && !pool.isEmpty();
        }

        if (nextChoice) {
            this.showChoices(pool);
        }
        else {
            this.saveDraft();
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onRemoveItems()
     */
    @Override
    protected void onRemoveItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
    }

    @Override
    protected void buildAddContextMenu(EditorContextMenuBuilder cmb) {
        cmb.addMoveItems(localizer.getMessage("lblDraft"), null);
    }

    @Override
    protected void buildRemoveContextMenu(EditorContextMenuBuilder cmb) {
        // no valid remove options
    }

    /**
     * <p>
     * showChoices.
     * </p>
     *
     * @param list
     *            a {@link ItemPool<PaperCard>} object.
     */
    private void showChoices(final ItemPool<PaperCard> list) {
        int packNumber = ((BoosterDraft) boosterDraft).getCurrentBoosterIndex() + 1;

        this.getCatalogManager().setCaption(localizer.getMessage("lblPackNCards", String.valueOf(packNumber)));
        this.getCatalogManager().setPool(list);
    } // showChoices()

    /**
     * <p>
     * getPlayersDeck.
     * </p>
     *
     * @return a {@link forge.deck.Deck} object.
     */
    private Deck getPlayersDeck() {
        final Deck deck = new Deck();

        // add sideboard to deck
        deck.getOrCreate(DeckSection.Sideboard).addAll(this.getDeckManager().getPool());

        return deck;
    } // getPlayersDeck()

    /**
     * <p>
     * saveDraft.
     * </p>
     */
    private void saveDraft() {
        String s;
        HashSet<String> names = new HashSet<>();

        for (DeckGroup d : FModel.getDecks().getDraft()) {
            names.add(d.getName());
        }

        do{
            // Cancel button will be null; OK will return string.
            // Must check for null value first, then string length.
            s = FOptionPane.showInputDialog(localizer.getMessage("lblSaveDraftAs") + ":", localizer.getMessage("lblSaveDraft"), FOptionPane.QUESTION_ICON);

            if (s == null && FOptionPane.showConfirmDialog(
            		localizer.getMessage("lblQuitWithoutSaving") + "?",
            				localizer.getMessage("lblQuitDraft") + "?", false)) {
                FScreen.DRAFTING_PROCESS.close();
                return;
            }

            // Overwrite same name?
            else if (names.contains(s) && !FOptionPane.showConfirmDialog(
            		localizer.getMessage("lblAlreadyDeckName") + s + localizer.getMessage("lblOverwriteConfirm"),
            		localizer.getMessage("lblOverwriteDeck"), false)) {
                s = "";
            }

        } while(s == null || s.isEmpty());

        saved = true;

        // Construct computer's decks and save draft
        final Deck[] computer = this.boosterDraft.getDecks();

        final DeckGroup finishedDraft = new DeckGroup(s);
        finishedDraft.setHumanDeck((Deck) this.getPlayersDeck().copyTo(s));
        finishedDraft.addAiDecks(computer);

        FModel.getDecks().getDraft().add(finishedDraft);
        CSubmenuDraft.SINGLETON_INSTANCE.update();
        FScreen.DRAFTING_PROCESS.close();

        //open draft pool in Draft Deck Editor right away
        Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_DRAFT);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorLimited(FModel.getDecks().getDraft(), FScreen.DECK_EDITOR_DRAFT, getCDetailPicture()));
        CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController().load(null, s);
    }

    //========== Overridden from ACEditorBase

    @Override
    protected CardLimit getCardLimit() {
        return CardLimit.None;
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.gui.deckeditor.ACEditorBase#getController()
     */
    @Override
    public DeckController<DeckGroup> getDeckController() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.gui.deckeditor.ACEditorBase#updateView()
     */
    @Override
    public void resetTables() {
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.gui.deckeditor.ACEditorBase#show(forge.Command)
     */
    @Override
    public void update() {
        this.getCatalogManager().setup(ItemManagerConfig.DRAFT_PACK);
        this.getDeckManager().setup(ItemManagerConfig.DRAFT_POOL);

        ccAddLabel = this.getBtnAdd().getText();

        if (this.getDeckManager().getPool() == null) { //avoid showing next choice or resetting pool if just switching back to Draft screen
            this.showChoices(this.boosterDraft.nextChoice());
            this.getDeckManager().setPool((Iterable<PaperCard>) null);
        }
        else {
            this.showChoices(this.getCatalogManager().getPool());
        }

        //Remove buttons
        this.getBtnAdd().setVisible(false);
        this.getBtnAdd4().setVisible(false);
        this.getBtnRemove().setVisible(false);
        this.getBtnRemove4().setVisible(false);

        this.getCbxSection().setVisible(false);

        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(false);

        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        constructedDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);
        commanderDecksParent = removeTab(VCommanderDecks.SINGLETON_INSTANCE);
        oathbreakerDecksParent = removeTab(VOathbreakerDecks.SINGLETON_INSTANCE);
        brawlDecksParent = removeTab(VBrawlDecks.SINGLETON_INSTANCE);
        tinyLeadersDecksParent = removeTab(VTinyLeadersDecks.SINGLETON_INSTANCE);

        // set catalog table to single-selection only mode
        getCatalogManager().setAllowMultipleSelections(false);

        // this appears to be needed to actually show the available draft choices
        getCatalogManager().refresh();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#canSwitchAway()
     */
    @Override
    public boolean canSwitchAway(boolean isClosing) {
        if (isClosing && !saved) {
            String userPrompt = localizer.getMessage("lblEndDraftConfirm");
            return FOptionPane.showConfirmDialog(userPrompt, localizer.getMessage("lblLeaveDraft"), localizer.getMessage("lblLeave"), localizer.getMessage("lblCancel"), false);
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#resetUIChanges()
     */
    @Override
    public void resetUIChanges() {
        //Re-rename buttons
        this.getBtnAdd().setText(ccAddLabel);

        //Re-add buttons
        this.getBtnAdd4().setVisible(true);
        this.getBtnRemove().setVisible(true);
        this.getBtnRemove4().setVisible(true);

        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(true);

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

        // set catalog table back to free-selection mode
        getCatalogManager().setAllowMultipleSelections(true);
    }
}
