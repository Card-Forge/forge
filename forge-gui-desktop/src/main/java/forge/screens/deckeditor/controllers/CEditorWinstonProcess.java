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
import forge.UiCommand;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.views.VAllDecks;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.deckeditor.views.VDeckgen;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.screens.home.sanctioned.CSubmenuWinston;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FOptionPane;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.item.PaperCard;
import forge.limited.BoosterDraft;
import forge.limited.IBoosterDraft;
import forge.limited.WinstonDraft;
import forge.properties.ForgePreferences.FPref;
import forge.util.ItemPool;
import forge.util.MyRandom;

import java.util.Map.Entry;

/**
 * Updates the deck editor UI as necessary draft selection mode.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 * @author Forge
 * @version $Id: CEditorDraftingProcess.java 24872 2014-02-17 07:35:47Z drdev $
 */
public class CEditorWinstonProcess extends ACEditorBase<PaperCard, DeckGroup> {
    private IBoosterDraft boosterDraft;

    private String ccAddLabel = "Add card";
    private String ccAdd4Label = "Add card";
    private Runnable ccAddCommand = null;
    private Runnable ccAdd4Command = null;

    private String ccTakeLabel = "Take pile";
    private String ccPassLabel = "Pass pile";

    private DragCell allDecksParent = null;
    private DragCell deckGenParent = null;
    private boolean saved = false;

    //========== Constructor

    /**
     * Updates the deck editor UI as necessary draft selection mode.
     */
    public CEditorWinstonProcess(final CDetailPicture cDetailPicture) {
        super(FScreen.DRAFTING_PROCESS, cDetailPicture);

        final CardManager catalogManager = new CardManager(cDetailPicture, false);
        final CardManager deckManager = new CardManager(cDetailPicture, false);

        //hide filters and options panel so more of pack is visible by default
        catalogManager.setHideViewOptions(1, true);

        deckManager.setCaption("Draft Picks");

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
        // This doesn't actually do anything, need to use buttons to interact
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onRemoveItems()
     */
    @Override
    protected void onRemoveItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
    }

    @Override
    protected void buildAddContextMenu(EditorContextMenuBuilder cmb) {
        cmb.addMoveItems("Draft", null);
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

        StringBuilder sb = new StringBuilder();
        sb.append("Remaining: ");
        sb.append(((WinstonDraft)boosterDraft).getDeckSize());
        sb.append("    AI Cards: ");
        sb.append(((WinstonDraft)boosterDraft).getAIDraftSize());
        sb.append("    Pile");
        sb.append(" ");
        sb.append(packNumber);
        sb.append(" - Cards");

        this.getCatalogManager().setCaption(sb.toString());
        this.getCatalogManager().setPool(list);

        this.getDeckManager().refresh();
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

        final String landSet = IBoosterDraft.LAND_SET_CODE[0].getCode();
        final boolean isZendikarSet = landSet.equals("ZEN"); // we want to generate one kind of Zendikar lands at a time only
        final boolean zendikarSetMode = MyRandom.getRandom().nextBoolean();

        final int landsCount = 10;

        for(String landName : MagicColor.Constant.BASIC_LANDS) {
            int numArt = FModel.getMagicDb().getCommonCards().getArtCount(landName, landSet);
            int minArtIndex = isZendikarSet ? (zendikarSetMode ? 1 : 5) : 1;
            int maxArtIndex = isZendikarSet ? minArtIndex + 3 : numArt;

            if (FModel.getPreferences().getPrefBoolean(FPref.UI_RANDOM_ART_IN_POOLS)) {

                for (int i = minArtIndex; i <= maxArtIndex; i++) {
                    deck.get(DeckSection.Sideboard).add(landName, landSet, i, numArt > 1 ? landsCount : 30);
                }
            } else {
                deck.get(DeckSection.Sideboard).add(landName, landSet, 30);
            }
        }

        return deck;
    } // getPlayersDeck()

    /**
     * <p>
     * saveDraft.
     * </p>
     */
    private void saveDraft() {
        String s = FOptionPane.showInputDialog("Save this draft as:", "Save Draft", FOptionPane.QUESTION_ICON);

        // Cancel button will be null; OK will return string.
        // Must check for null value first, then string length.
        // Recurse, if either null or empty string.
        if (s == null || s.isEmpty()) {
            saveDraft();
            return;
        }

        // Check for overwrite case
        for (DeckGroup d : FModel.getDecks().getWinston()) {
            if (s.equalsIgnoreCase(d.getName())) {
                if (!FOptionPane.showConfirmDialog(
                        "There is already a deck named '" + s + "'. Overwrite?",
                        "Overwrite Deck?", false)) {
                    // If no overwrite, recurse.
                    saveDraft();
                    return;
                }
                break;
            }
        }

        saved = true;

        // TODO For pile drafts, only one other draft deck is made

        // Construct computer's decks and save draft
        final Deck[] computer = this.boosterDraft.getDecks();

        final DeckGroup finishedDraft = new DeckGroup(s);
        finishedDraft.setHumanDeck((Deck) this.getPlayersDeck().copyTo(s));
        finishedDraft.addAiDecks(computer);

        FModel.getDecks().getWinston().add(finishedDraft);
        CSubmenuWinston.SINGLETON_INSTANCE.update();
        FScreen.DRAFTING_PROCESS.close();

        //open draft pool in Draft Deck Editor right away
        Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_DRAFT);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorLimited(FModel.getDecks().getWinston(), FScreen.DECK_EDITOR_DRAFT, getCDetailPicture()));
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
    @SuppressWarnings("serial")
    public void update() {
        this.getCatalogManager().setup(ItemManagerConfig.DRAFT_PACK);
        this.getDeckManager().setup(ItemManagerConfig.DRAFT_POOL);

        ccAddLabel = this.getBtnAdd().getText();

        if (this.getDeckManager().getPool() == null) { //avoid showing next choice or resetting pool if just switching back to Draft screen
            this.getDeckManager().setPool((Iterable<PaperCard>) null);
            this.showChoices(this.boosterDraft.nextChoice());
        }
        else {
            this.showChoices(this.getCatalogManager().getPool());
        }

        this.getBtnRemove().setVisible(false);
        this.getBtnRemove4().setVisible(false);

        this.getBtnAdd().setText(ccTakeLabel);
        this.getBtnAdd4().setText(ccPassLabel);
        // Link the buttons to the appropriate functions

        this.ccAddCommand = this.getBtnAdd().getCommand();
        this.ccAdd4Command = this.getBtnAdd4().getCommand();

        this.getBtnAdd().setCommand(new UiCommand() {
            @Override
            public void run() {
                CEditorWinstonProcess.this.takePile();
            }
        });
        this.getBtnAdd4().setCommand(new UiCommand() {
            @Override
            public void run() {
                CEditorWinstonProcess.this.passPile();
            }
        });

        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        allDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);

        // set catalog table to single-selection only mode
        //getCatalogManager().setAllowMultipleSelections(false);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#canSwitchAway()
     */
    @Override
    public boolean canSwitchAway(boolean isClosing) {
        if (isClosing && !saved) {
            String userPrompt =
                    "This will end the current draft and you will not be able to resume.\n\n" +
                            "Leave anyway?";
            return FOptionPane.showConfirmDialog(userPrompt, "Leave Draft?", "Leave", "Cancel", false);
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#resetUIChanges()
     */
    @Override
    public void resetUIChanges() {
        //Restore button names
        this.getBtnAdd().setText(ccAddLabel);
        this.getBtnAdd4().setText(ccAdd4Label);
        this.getBtnAdd().setCommand(this.ccAddCommand);
        this.getBtnAdd4().setCommand(this.ccAdd4Command);

        //Re-add buttons
        this.getBtnRemove().setVisible(true);
        this.getBtnRemove4().setVisible(true);

        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(true);

        //Re-add tabs
        if (deckGenParent != null) {
            deckGenParent.addDoc(VDeckgen.SINGLETON_INSTANCE);
        }
        if (allDecksParent != null) {
            allDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
        }

        // set catalog table back to free-selection mode
        getCatalogManager().setAllowMultipleSelections(true);
    }

    private void takePile() {
        CardPool pool = ((WinstonDraft)boosterDraft).takeActivePile(true);
        // Add pool to deck
        this.getDeckManager().getPool().addAll(pool);

        // get next booster pack
        if (this.boosterDraft.hasNextChoice()) {
            CardPool newPool = this.boosterDraft.nextChoice();
            if (newPool != null) {
                this.showChoices(newPool);
                return;
            }
        }
        // If we get here, there's no choices left. Finish the draft and then save it
        this.saveDraft();
    }

    private void passPile() {
        CardPool pool = ((WinstonDraft)boosterDraft).passActivePile(true);

        if (pool != null) {
            // Passed the third pile, draw the top card of the deck
            this.getDeckManager().getPool().addAll(pool);
        }
        CardPool newPool = this.boosterDraft.nextChoice();
        if (newPool != null) {
            this.showChoices(newPool);
        }
    }
}

