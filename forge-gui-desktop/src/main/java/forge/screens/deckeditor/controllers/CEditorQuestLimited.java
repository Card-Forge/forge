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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Supplier;

import forge.UiCommand;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.views.ItemTableColumn;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.QuestEventDraft;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.deckeditor.views.VAllDecks;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.deckeditor.views.VDeckgen;
import forge.screens.home.quest.CSubmenuQuestDecks;
import forge.screens.match.controllers.CDetailPicture;

/**
 * Child controller for quest deck editor UI.
 * <br><br>
 * Card catalog and decks are drawn from a QuestController object.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 * @author Forge
 * @version $Id: CEditorQuest.java 24868 2014-02-17 05:08:05Z drdev $
 */
public final class CEditorQuestLimited extends ACEditorBase<PaperCard, DeckGroup> {
    private final QuestController questData;
    private final DeckController<DeckGroup> controller;
    private final List<DeckSection> allSections = new ArrayList<DeckSection>();
    private DragCell allDecksParent = null;
    private DragCell deckGenParent = null;

    private Map<PaperCard, Integer> decksUsingMyCards;

    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnDeckCompare = new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
        @Override
        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = decksUsingMyCards.get(from.getKey());
            return iValue == null ? Integer.valueOf(0) : iValue;
        }
    };

    private final Function<Entry<? extends InventoryItem, Integer>, Object> fnDeckGet = new Function<Entry<? extends InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<? extends InventoryItem, Integer> from) {
            final Integer iValue = decksUsingMyCards.get(from.getKey());
            return iValue == null ? "" : iValue.toString();
        }
    };

    /**
     * Child controller for quest deck editor UI.
     * <br><br>
     * Card catalog and decks are drawn from a QuestController object.
     *
     * @param questData0 &emsp; {@link forge.quest.QuestController}
     */
    @SuppressWarnings("serial")
    public CEditorQuestLimited(final QuestController questData0, final CDetailPicture cDetailPicture) {
        super(FScreen.DECK_EDITOR_QUEST_TOURNAMENT, cDetailPicture);

        allSections.add(DeckSection.Main);
        allSections.add(DeckSection.Sideboard);

        this.questData = questData0;

        final CardManager catalogManager = new CardManager(cDetailPicture, false);
        final CardManager deckManager = new CardManager(cDetailPicture, false);

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

        this.controller = new DeckController<DeckGroup>(questData0.getDraftDecks(), this, newCreator);
        controller.getView().getDeckManager().setup(ItemManagerConfig.DRAFT_POOL);
        controller.setModel(questData0.getDraftDecks().get(QuestEventDraft.DECK_NAME));

        getBtnAddBasicLands().setCommand(new UiCommand() {
            @Override
            public void run() {
                CEditorLimited.addBasicLands(CEditorQuestLimited.this);
            }
        });
    }

    // fills number of decks using each card
    private Map<PaperCard, Integer> countDecksForEachCard() {
        final Map<PaperCard, Integer> result = new HashMap<PaperCard, Integer>();
        for (final Deck deck : this.questData.getMyDecks()) {
            for (final Entry<PaperCard, Integer> e : deck.getMain()) {
                final PaperCard card = e.getKey();
                final Integer amount = result.get(card);
                result.put(card, Integer.valueOf(amount == null ? 1 : 1 + amount.intValue()));
            }
        }
        return result;
    }

    //=========== Overridden from ACEditorBase

    @Override
    protected CardLimit getCardLimit() {
        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            return CardLimit.Default;
        }
        return CardLimit.None; //if not enforcing deck legality, don't enforce default limit
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onAddItems()
     */
    @Override
    protected void onAddItems(final Iterable<Entry<PaperCard, Integer>> items, final boolean toAlternate) {
        if (toAlternate) {
            return;
        }

        // update view
        this.getDeckManager().addItems(items);
        this.getCatalogManager().removeItems(items);
        this.getDeckController().notifyModelChanged();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onRemoveItems()
     */
    @Override
    protected void onRemoveItems(final Iterable<Entry<PaperCard, Integer>> items, final boolean toAlternate) {
        if (toAlternate) {
            return;
        }

        // update view
        this.getCatalogManager().addItems(items);
        this.getDeckManager().removeItems(items);
        this.getDeckController().notifyModelChanged();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#buildAddContextMenu()
     */
    @Override
    protected void buildAddContextMenu(final EditorContextMenuBuilder cmb) {
        cmb.addMoveItems("Move", "to deck");
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#buildRemoveContextMenu()
     */
    @Override
    protected void buildRemoveContextMenu(final EditorContextMenuBuilder cmb) {
        cmb.addMoveItems("Move", "to sideboard");
    }

    private Deck getSelectedDeck(final DeckGroup model) {
        return model.getHumanDeck();
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

    //=========== Overridden from ACEditorBase

    /*
     * (non-Javadoc)
     *
     * @see forge.gui.deckeditor.ACEditorBase#getController()
     */
    @Override
    public DeckController<DeckGroup> getDeckController() {
        return this.controller;
    }

    @Override
    public void update() {
        this.getCatalogManager().setup(getScreen() == FScreen.DECK_EDITOR_DRAFT ? ItemManagerConfig.DRAFT_POOL : ItemManagerConfig.SEALED_POOL);
        this.getDeckManager().setup(ItemManagerConfig.DECK_EDITOR);

        this.decksUsingMyCards = this.countDecksForEachCard();

        final Map<ColumnDef, ItemTableColumn> colOverridesCatalog = new HashMap<ColumnDef, ItemTableColumn>();
        final Map<ColumnDef, ItemTableColumn> colOverridesDeck = new HashMap<ColumnDef, ItemTableColumn>();

        ItemTableColumn.addColOverride(ItemManagerConfig.QUEST_EDITOR_POOL, colOverridesCatalog, ColumnDef.NEW, this.questData.getCards().getFnNewCompare(), this.questData.getCards().getFnNewGet());
        ItemTableColumn.addColOverride(ItemManagerConfig.QUEST_DECK_EDITOR, colOverridesDeck, ColumnDef.NEW, this.questData.getCards().getFnNewCompare(), this.questData.getCards().getFnNewGet());
        ItemTableColumn.addColOverride(ItemManagerConfig.QUEST_DECK_EDITOR, colOverridesDeck, ColumnDef.DECKS, this.fnDeckCompare, this.fnDeckGet);

        this.getCatalogManager().setup(ItemManagerConfig.QUEST_EDITOR_POOL, colOverridesCatalog);
        this.getDeckManager().setup(ItemManagerConfig.QUEST_DECK_EDITOR, colOverridesDeck);

        resetUI();

        VCurrentDeck.SINGLETON_INSTANCE.getBtnSave().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnImport().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setEnabled(false);

        VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().setVisible(false);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().setVisible(false);

        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        allDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);

        if (this.controller.getModel() == null) {
            throw new RuntimeException("Expected deck group but found none!");
        }
        else {
            this.controller.refreshModel();
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#canSwitchAway()
     */
    @Override
    public boolean canSwitchAway(final boolean isClosing) {
        if (SEditorIO.confirmSaveChanges(FScreen.DECK_EDITOR_QUEST_TOURNAMENT, isClosing)) {
            FModel.getQuest().save();
            return true;
        }
        FModel.getQuest().save();
        return false;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#resetUIChanges()
     */
    @Override
    public void resetUIChanges() {
        CSubmenuQuestDecks.SINGLETON_INSTANCE.update();
        //Re-add tabs
        if (deckGenParent != null) {
            deckGenParent.addDoc(VDeckgen.SINGLETON_INSTANCE);
        }
        if (allDecksParent != null) {
            allDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
        }
    }

}
