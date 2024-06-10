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

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.IBoosterDraft;
import forge.gamemodes.quest.QuestEventDraft;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.screens.deckeditor.views.*;
import forge.screens.home.quest.CSubmenuQuestDraft;
import forge.screens.home.quest.VSubmenuQuestDraft;
import forge.screens.match.controllers.CDetailPicture;
import forge.util.ItemPool;
import forge.util.Localizer;

import java.util.Map.Entry;

/**
 * Updates the deck editor UI as necessary draft selection mode.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 * @author Forge
 * @version $Id: CEditorDraftingProcess.java 24872 2014-02-17 07:35:47Z drdev $
 */
public class CEditorQuestDraftingProcess extends ACEditorBase<PaperCard, DeckGroup> {

    private CSubmenuQuestDraft draftQuest;

    public void setDraftQuest(CSubmenuQuestDraft draftQuest0) {
        draftQuest = draftQuest0;
    }

    private IBoosterDraft boosterDraft;

    private String ccAddLabel = Localizer.getInstance().getMessage("lblAddcard");
    private DragCell constructedDecksParent = null;
    private DragCell commanderDecksParent = null;
    private DragCell oathbreakerDecksParent = null;
    private DragCell brawlDecksParent = null;
    private DragCell tinyLeadersDecksParent = null;
    private DragCell deckGenParent = null;
    private boolean saved = false;

    //========== Constructor

    /**
     * Updates the deck editor UI as necessary draft selection mode.
     */
    public CEditorQuestDraftingProcess(final CDetailPicture cDetailPicture0) {
        super(FScreen.DRAFTING_PROCESS, cDetailPicture0, GameType.QuestDraft);

        final CardManager catalogManager = new CardManager(cDetailPicture0, false, false, true);
        final CardManager deckManager = new CardManager(cDetailPicture0, false, false, true);

        //hide filters and options panel so more of pack is visible by default
        catalogManager.setHideViewOptions(1, true);

        deckManager.setCaption(Localizer.getInstance().getMessage("lblDraftPicks"));

        catalogManager.setAlwaysNonUnique(true);
        deckManager.setAlwaysNonUnique(true);

        getBtnAddBasicLands().setVisible(false);

        setCatalogManager(catalogManager);
        setDeckManager(deckManager);
    }

    /**
     * Show GuiBase.getInterface().
     *
     * @param inBoosterDraft
     *            the in_booster draft
     */
    public final void showGui(final IBoosterDraft inBoosterDraft) {
        boosterDraft = inBoosterDraft;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onAddItems()
     */
    @Override
    protected void onAddItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        if (toAlternate) { return; }

        // can only draft one at a time, regardless of the requested quantity
        PaperCard card = items.iterator().next().getKey();
        getDeckManager().addItem(card, 1);

        // get next booster pack
        boosterDraft.setChoice(card);

        boolean nextChoice = this.boosterDraft.hasNextChoice();
        ItemPool<PaperCard> pool = null;
        if (nextChoice) {
            pool = this.boosterDraft.nextChoice();
            nextChoice = !pool.isEmpty();
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
        cmb.addMoveItems(Localizer.getInstance().getMessage("lblDraft"), null);
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

        this.getCatalogManager().setCaption(Localizer.getInstance().getMessage("lblPackNCards", String.valueOf(packNumber)));
        getCatalogManager().setPool(list);
    } // showChoices()

    /**
     * <p>
     * getPlayersDeck.
     * </p>
     *
     * @return a {@link forge.deck.Deck} object.
     */
    public Deck getPlayersDeck() {
        final Deck deck = new Deck();

        // add sideboard to deck
        deck.getOrCreate(DeckSection.Sideboard).addAll(getDeckManager().getPool());

        // No need to add basic lands now that Add Basic Lands button exists
        /*final String landSet = IBoosterDraft.LAND_SET_CODE[0].getCode();
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
        */

        return deck;
    } // getPlayersDeck()

    /**
     * <p>
     * saveDraft.
     * </p>
     */
    private void saveDraft() {
        // This should be inheriting more from CEditorDraftingProcess

        saved = true;

        // Construct computer's decks and save draft
        final Deck[] computer = boosterDraft.getDecks();

        final DeckGroup finishedDraft = new DeckGroup(QuestEventDraft.DECK_NAME);
        finishedDraft.setHumanDeck((Deck) getPlayersDeck().copyTo(QuestEventDraft.DECK_NAME));
        finishedDraft.addAiDecks(computer);

        CSubmenuQuestDraft.SINGLETON_INSTANCE.update();
        FScreen.DRAFTING_PROCESS.close();

        draftQuest.setCompletedDraft(finishedDraft);

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
        getCatalogManager().setup(ItemManagerConfig.DRAFT_PACK);
        getDeckManager().setup(ItemManagerConfig.DRAFT_POOL);

        ccAddLabel = getBtnAdd().getText();

        if (getDeckManager().getPool() == null) { //avoid showing next choice or resetting pool if just switching back to Draft screen
            showChoices(boosterDraft.nextChoice());
            getDeckManager().setPool((Iterable<PaperCard>) null);
        }
        else {
            showChoices(getCatalogManager().getPool());
        }

        //Remove buttons
        getBtnAdd().setVisible(false);
        getBtnAdd4().setVisible(false);
        getBtnRemove().setVisible(false);
        getBtnRemove4().setVisible(false);

        getCbxSection().setVisible(false);

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
            if (draftQuest.cancelDraft()) {
                CSubmenuQuestDraft.SINGLETON_INSTANCE.update();
                VSubmenuQuestDraft.SINGLETON_INSTANCE.populate();
                return true;
            }
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#resetUIChanges()
     */
    @Override
    public void resetUIChanges() {
        //Re-rename buttons
        getBtnAdd().setText(ccAddLabel);

        //Re-add buttons
        getBtnAdd4().setVisible(true);
        getBtnRemove().setVisible(true);
        getBtnRemove4().setVisible(true);

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
