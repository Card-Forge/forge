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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.KeyStroke;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.data.DeckConstructionRules;
import forge.gui.GuiUtils;
import forge.gui.UiCommand;
import forge.gui.framework.DragCell;
import forge.gui.framework.FScreen;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.views.ItemTableColumn;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.deckeditor.AddBasicLandsDialog;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.deckeditor.views.VAllDecks;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.deckeditor.views.VDeckgen;
import forge.screens.home.quest.CSubmenuQuestDecks;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FComboBox;
import forge.util.ItemPool;

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
public final class CEditorQuest extends CDeckEditor<Deck> {
    private final QuestController questData;
    private final DeckController<Deck> controller;
    private final List<DeckSection> allSections = new ArrayList<>();
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
     * @param questData0 &emsp; {@link forge.gamemodes.quest.QuestController}
     */
    @SuppressWarnings("serial")
    public CEditorQuest(final QuestController questData0, final CDetailPicture cDetailPicture0) {
        super(FScreen.DECK_EDITOR_QUEST, cDetailPicture0, GameType.Quest);

        allSections.add(DeckSection.Main);
        allSections.add(DeckSection.Sideboard);

        //Add sub-format specific sections
        switch(FModel.getQuest().getDeckConstructionRules()){
            case Default: break;
            case Commander:
                allSections.add(DeckSection.Commander);
                break;
        }

        this.questData = questData0;

        final CardManager catalogManager = new CardManager(cDetailPicture0, false, true);
        final CardManager deckManager = new CardManager(cDetailPicture0, false, true);

        catalogManager.setCaption("Quest Inventory");

        catalogManager.setAlwaysNonUnique(true);
        deckManager.setAlwaysNonUnique(true);

        this.setCatalogManager(catalogManager);
        this.setDeckManager(deckManager);

        final Supplier<Deck> newCreator = new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        };

        this.controller = new DeckController<>(questData0.getMyDecks(), this, newCreator);

        getBtnAddBasicLands().setCommand(new UiCommand() {
            @Override
            public void run() {
                Deck deck = getDeckController().getModel();
                if (deck == null) { return; }

                AddBasicLandsDialog dialog = new AddBasicLandsDialog(deck, questData.getDefaultLandSet());
                CardPool landsToAdd = dialog.show();
                if (landsToAdd != null) {
                    onAddItems(landsToAdd, false);
                }
            }
        });
    }

    // fills number of decks using each card
    private Map<PaperCard, Integer> countDecksForEachCard() {
        final Map<PaperCard, Integer> result = new HashMap<>();
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
            //If this is a commander quest, only allow single copies of cards
            if(FModel.getQuest().getDeckConstructionRules() == DeckConstructionRules.Commander){
                return CardLimit.Singleton;
            }
            return CardLimit.Default;
        }
        return CardLimit.None; //if not enforcing deck legality, don't enforce default limit
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onAddItems()
     */
    @Override
    protected void onAddItems(final Iterable<Entry<PaperCard, Integer>> items, final boolean toAlternate) {
        CEditorConstructed.onAddItems(this, items, toAlternate);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onRemoveItems()
     */
    @Override
    protected void onRemoveItems(final Iterable<Entry<PaperCard, Integer>> items, final boolean toAlternate) {
        CEditorConstructed.onRemoveItems(this, items, toAlternate);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#buildAddContextMenu()
     */
    @Override
    protected void buildAddContextMenu(final EditorContextMenuBuilder cmb) {
        CEditorConstructed.buildAddContextMenu(cmb, sectionMode, GameType.Quest);
        AddRatingItem(cmb, 1);
        AddRatingItem(cmb, 2);
        AddRatingItem(cmb, 3);
        AddRatingItem(cmb, 4);
        AddRatingItem(cmb, 5);
        AddRatingItem(cmb, 0);
    }

    public void AddRatingItem(final EditorContextMenuBuilder cmb, final int n) {
        if (n == 1) {
            cmb.getMenu().addSeparator();
        }
        String s;
        if (n == 0) {
            s = "Remove custom rating";
        } else {
            s = "Rate this card as " + n + " stars";
        }
        GuiUtils.addMenuItem(cmb.getMenu(), s,
                KeyStroke.getKeyStroke(48 + n, 0),
                new Runnable() {
                    @Override
                    public void run() {
                        SetRatingStars(n,cmb);
                    }
                });
    }

    public void SetRatingStars(int n, EditorContextMenuBuilder cmb) {
        ItemPool<PaperCard> selected = cmb.getItemManager().getSelectedItemPool();

        for (final Entry<PaperCard, Integer> itemEntry : selected) {
            // the card: itemEntry.getKey()
            questData.SetRating(itemEntry.getKey().getName(), itemEntry.getKey().getEdition(), n);
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#buildRemoveContextMenu()
     */
    @Override
    protected void buildRemoveContextMenu(final EditorContextMenuBuilder cmb) {
        CEditorConstructed.buildRemoveContextMenu(cmb, sectionMode, false);
        AddRatingItem(cmb, 1);
        AddRatingItem(cmb, 2);
        AddRatingItem(cmb, 3);
        AddRatingItem(cmb, 4);
        AddRatingItem(cmb, 5);
        AddRatingItem(cmb, 0);
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.gui.deckeditor.ACEditorBase#updateView()
     */
    @Override
    public void resetTables() {
        this.sectionMode = DeckSection.Main;

        // show cards, makes this user friendly
        this.getCatalogManager().setPool(getRemainingCardPool());
        this.getDeckManager().setPool(getDeck().getMain());
    }

    /***
     * Provides the pool of cards the player has available to add to his or her deck. Also manages showing available cards
     * to choose from for special deck construction rules, e.g.: Commander.
     * @return CardPool of cards available to add to the player's deck.
     */
    private CardPool getRemainingCardPool(){
        final CardPool cardpool = getInitialCatalog();

        // remove bottom cards that are in the deck from the card pool
        cardpool.removeAll(getDeck().getMain());

        // remove sideboard cards from the catalog
        cardpool.removeAll(getDeck().getOrCreate(DeckSection.Sideboard));

        switch(FModel.getQuest().getDeckConstructionRules()){
            case Default: break;
            case Commander:
                //remove this deck's currently selected commander(s) from the catalog
                cardpool.removeAll(getDeck().getOrCreate(DeckSection.Commander));

                //TODO: Only thin if deck conformance is being applied
                if(getDeck().getOrCreate(DeckSection.Commander).toFlatList().size() > 0) {
                    Predicate<PaperCard> identityPredicate = new MatchCommanderColorIdentity(getDeckColorIdentity());
                    CardPool filteredPool = cardpool.getFilteredPool(identityPredicate);

                    return filteredPool;
                }
                break;
        }

        return cardpool;
    }

    /**
     * Predicate that filters out based on a color identity provided upon instantiation. Used to filter the card
     * list when a commander is chosen so the user can more easily see what cards are available for his or her deck
     * and avoid making additions that are not legal.
     */
    public static class MatchCommanderColorIdentity implements Predicate<PaperCard> {
        private final ColorSet allowedColor;

        public MatchCommanderColorIdentity(ColorSet color) {
            allowedColor = color;
        }

        @Override
        public boolean apply(PaperCard subject) {
            CardRules cr = subject.getRules();
            ManaCost mc = cr.getManaCost();
            return allowedColor.containsAllColorsFrom(cr.getColorIdentity().getColor());
        }
    }

    /**
     * Compiles the color identity of the loaded deck based on the commanders.
     * @return A ColorSet containing the color identity of the currently loaded deck.
     */
    public ColorSet getDeckColorIdentity(){

        List<PaperCard> commanders = getDeck().getOrCreate(DeckSection.Commander).toFlatList();
        List<String> colors = new ArrayList<>();

        //Return early if there are no current commanders
        if(commanders.size() == 0){
            colors.add("c");
            return ColorSet.fromNames(colors);
        }

        //For each commander,add each color of its color identity if not already added
        for(PaperCard pc : commanders){
            if(!colors.contains("w") && pc.getRules().getColorIdentity().hasWhite()) colors.add("w");
            if(!colors.contains("u") && pc.getRules().getColorIdentity().hasBlue()) colors.add("u");
            if(!colors.contains("b") && pc.getRules().getColorIdentity().hasBlack()) colors.add("b");
            if(!colors.contains("r") && pc.getRules().getColorIdentity().hasRed()) colors.add("r");
            if(!colors.contains("g") && pc.getRules().getColorIdentity().hasGreen()) colors.add("g");
        }

        colors.add("c");

        return ColorSet.fromNames(colors);
    }

    /*
    Used to make the code more readable in game terms.
     */
    private Deck getDeck(){
        return this.controller.getModel();
    }

    private ItemPool<PaperCard> getCommanderCardPool(){
        Predicate<PaperCard> commanderPredicate = Predicates.compose(CardRulesPredicates.Presets.CAN_BE_COMMANDER, PaperCard.FN_GET_RULES);
        return getRemainingCardPool().getFilteredPool(commanderPredicate);
    }

    @Override
    protected CardPool getInitialCatalog() {
        return new CardPool(this.questData.getCards().getCardpool());
    }

    @Override
    public Boolean isSectionImportable(DeckSection section) {
        return allSections.contains(section);
    }

    //=========== Overridden from ACEditorBase

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
     * Switch between the main deck and the sideboard/Command Zone editor.
     */
    public void setEditorMode(DeckSection sectionMode) {
        //Fixes null pointer error on switching tabs while quest deck editor is open. TODO: Find source of bug possibly?
        if(sectionMode == null) sectionMode = DeckSection.Main;

        final Map<ColumnDef, ItemTableColumn> colOverridesCatalog = new HashMap<>();
        ItemTableColumn.addColOverride(ItemManagerConfig.QUEST_EDITOR_POOL, colOverridesCatalog, ColumnDef.NEW, this.questData.getCards().getFnNewCompare(), this.questData.getCards().getFnNewGet());

        //Based on which section the editor is in, display the remaining card pool (or applicable card pool if in
        //Commander) and the current section's cards
        switch(sectionMode){
            case Main :
                this.getCatalogManager().setup(ItemManagerConfig.QUEST_EDITOR_POOL, colOverridesCatalog);
                this.getCatalogManager().setPool(getRemainingCardPool());
                this.getDeckManager().setPool(this.controller.getModel().getMain());
                break;
            case Sideboard :
                this.getCatalogManager().setup(ItemManagerConfig.QUEST_EDITOR_POOL, colOverridesCatalog);
                this.getCatalogManager().setPool(getRemainingCardPool());
                this.getDeckManager().setPool(getDeck().getOrCreate(DeckSection.Sideboard));
                break;
            case Commander :
                this.getCatalogManager().setup(ItemManagerConfig.COMMANDER_POOL);
                this.getCatalogManager().setPool(getCommanderCardPool());
                this.getDeckManager().setPool(getDeck().getOrCreate(DeckSection.Commander));
                break;
        }

        this.sectionMode = sectionMode;
        this.controller.updateCaptions();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#show(forge.Command)
     */
    @SuppressWarnings("serial")
    @Override
    public void update() {
        this.decksUsingMyCards = this.countDecksForEachCard();

        final Map<ColumnDef, ItemTableColumn> colOverridesCatalog = new HashMap<>();
        final Map<ColumnDef, ItemTableColumn> colOverridesDeck = new HashMap<>();

        ItemTableColumn.addColOverride(ItemManagerConfig.QUEST_EDITOR_POOL, colOverridesCatalog, ColumnDef.NEW, this.questData.getCards().getFnNewCompare(), this.questData.getCards().getFnNewGet());
        ItemTableColumn.addColOverride(ItemManagerConfig.QUEST_DECK_EDITOR, colOverridesDeck, ColumnDef.NEW, this.questData.getCards().getFnNewCompare(), this.questData.getCards().getFnNewGet());
        ItemTableColumn.addColOverride(ItemManagerConfig.QUEST_DECK_EDITOR, colOverridesDeck, ColumnDef.DECKS, this.fnDeckCompare, this.fnDeckGet);

        this.getCatalogManager().setup(ItemManagerConfig.QUEST_EDITOR_POOL, colOverridesCatalog);
        this.getDeckManager().setup(ItemManagerConfig.QUEST_DECK_EDITOR, colOverridesDeck);

        resetUI();

        VCurrentDeck.SINGLETON_INSTANCE.getBtnSave().setVisible(true);

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
        this.getCbxSection().setVisible(true);

        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        allDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);

        if (this.controller.getModel() == null) {
            this.getDeckController().setModel(new Deck());
        } else {
            this.controller.refreshModel();
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#canSwitchAway()
     */
    @Override
    public boolean canSwitchAway(final boolean isClosing) {
        if (SEditorIO.confirmSaveChanges(FScreen.DECK_EDITOR_QUEST, isClosing)) {
            FModel.getQuest().save();
            return true;
        }
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
