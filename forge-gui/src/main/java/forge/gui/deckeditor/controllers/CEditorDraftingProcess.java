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

import java.util.Map;
import java.util.Map.Entry;

import forge.Singletons;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.views.VAllDecks;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.deckeditor.views.VDeckgen;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.gui.home.sanctioned.CSubmenuDraft;
import forge.gui.toolbox.FOptionPane;
import forge.gui.toolbox.itemmanager.CardManager;
import forge.gui.toolbox.itemmanager.views.ColumnDef;
import forge.gui.toolbox.itemmanager.views.GroupDef;
import forge.gui.toolbox.itemmanager.views.ItemColumn;
import forge.gui.toolbox.itemmanager.views.ItemColumn.SortState;
import forge.gui.toolbox.itemmanager.views.SColumnUtil;
import forge.item.PaperCard;
import forge.limited.BoosterDraft;
import forge.limited.IBoosterDraft;
import forge.properties.ForgePreferences.FPref;
import forge.util.ItemPool;
import forge.util.MyRandom;

/**
 * Updates the deck editor UI as necessary draft selection mode.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 * 
 * @author Forge
 * @version $Id$
 */
public class CEditorDraftingProcess extends ACEditorBase<PaperCard, DeckGroup> {
    private IBoosterDraft boosterDraft;

    private String ccAddLabel = "Add card";
    private DragCell allDecksParent = null;
    private DragCell deckGenParent = null;

    //========== Constructor

    /**
     * Updates the deck editor UI as necessary draft selection mode.
     */
    public CEditorDraftingProcess() {
        super(FScreen.DRAFTING_PROCESS);

        final CardManager catalogManager = new CardManager(false);
        final CardManager deckManager = new CardManager(false);

        //hide filters and options panel so more of pack is visible by default
        catalogManager.setHideFilters(true);
        catalogManager.setHideViewOptions(1, true);

        deckManager.setCaption("Draft Picks");

        catalogManager.setAlwaysNonUnique(true);
        deckManager.setAlwaysNonUnique(true);

        this.setCatalogManager(catalogManager);
        this.setDeckManager(deckManager);
    }

    /**
     * Show gui.
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

        if (this.boosterDraft.hasNextChoice()) {
            this.showChoices(this.boosterDraft.nextChoice());
        }
        else {
            this.boosterDraft.finishedDrafting();
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
     *            a {@link forge.CardList} object.
     */
    private void showChoices(final ItemPool<PaperCard> list) {
        int packNumber = ((BoosterDraft) boosterDraft).getCurrentBoosterIndex() + 1;

        this.getCatalogManager().setCaption("Pack " + packNumber + " - Cards");
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

        final String landSet = IBoosterDraft.LAND_SET_CODE[0].getCode();
        final boolean isZendikarSet = landSet.equals("ZEN"); // we want to generate one kind of Zendikar lands at a time only
        final boolean zendikarSetMode = MyRandom.getRandom().nextBoolean();

        final int landsCount = 10;

        for(String landName : MagicColor.Constant.BASIC_LANDS) {
            int numArt = Singletons.getMagicDb().getCommonCards().getArtCount(landName, landSet);
            int minArtIndex = isZendikarSet ? (zendikarSetMode ? 1 : 5) : 1;
            int maxArtIndex = isZendikarSet ? minArtIndex + 3 : numArt;

            if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_RANDOM_ART_IN_POOLS)) {

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
        if (s == null || s.length() == 0) {
            saveDraft();
            return;
        }

        // Check for overwrite case
        for (DeckGroup d : Singletons.getModel().getDecks().getDraft()) {
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

        // Construct computer's decks and save draft
        final Deck[] computer = this.boosterDraft.getDecks();

        final DeckGroup finishedDraft = new DeckGroup(s);
        finishedDraft.setHumanDeck((Deck) this.getPlayersDeck().copyTo(s));
        finishedDraft.addAiDecks(computer);

        Singletons.getModel().getDecks().getDraft().add(finishedDraft);
        CSubmenuDraft.SINGLETON_INSTANCE.update();

        Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_DRAFT);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorLimited(Singletons.getModel().getDecks().getDraft(), FScreen.DECK_EDITOR_DRAFT));
        CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController().load(null, s);
        FScreen.DRAFTING_PROCESS.close();
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
        Map<ColumnDef, ItemColumn> catalogColumns = SColumnUtil.getCatalogDefaultColumns();
        catalogColumns.get(ColumnDef.FAVORITE).setSortPriority(0);
        catalogColumns.get(ColumnDef.RARITY).setSortPriority(1); //sort rares to top
        catalogColumns.get(ColumnDef.RARITY).setSortState(SortState.DESC);
        catalogColumns.get(ColumnDef.COLOR).setSortPriority(2);
        catalogColumns.get(ColumnDef.NAME).setSortPriority(3);

        this.getCatalogManager().setup(catalogColumns, null, null, 1);
        this.getDeckManager().setup(SColumnUtil.getDeckDefaultColumns(), GroupDef.CREATURE_SPELL_LAND, ColumnDef.CMC, 1);

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

        this.getBtnCycleSection().setVisible(false);

        VCurrentDeck.SINGLETON_INSTANCE.getPnlHeader().setVisible(false);

        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        allDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);

        // set catalog table to single-selection only mode
        getCatalogManager().setAllowMultipleSelections(false);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#canSwitchAway()
     */
    @Override
    public boolean canSwitchAway(boolean isClosing) {
        if (isClosing) {
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
        if (allDecksParent != null) {
            allDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
        }

        // set catalog table back to free-selection mode
        getCatalogManager().setAllowMultipleSelections(true);
    }
}
