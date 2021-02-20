package forge.deck;

import forge.FThreads;
import forge.Forge;
import forge.GuiBase;
import forge.assets.ImageCache;
import forge.deck.FDeckEditor.EditorType;
import forge.deck.io.DeckPreferences;
import forge.error.BugReporter;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletUtil;
import forge.interfaces.IGuiGame;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.ItemFilter;
import forge.match.HostedMatch;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestUtil;
import forge.screens.FScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.NewGameMenu.NewGameScreen;
import forge.screens.match.MatchController;
import forge.toolbox.FButton;
import forge.toolbox.FComboBox;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.GuiChoose;
import forge.toolbox.ListChooser;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FOptionPane;
import forge.util.Callback;
import forge.util.Localizer;
import forge.util.Utils;
import forge.util.storage.IStorage;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.utils.Align;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FDeckChooser extends FScreen {
    public static final float PADDING = Utils.scale(5);

    private FComboBox<DeckType> cmbDeckTypes;
    private DeckType selectedDeckType;
    private boolean needRefreshOnActivate;
    private Callback<Deck> callback;
    private NetDeckCategory netDeckCategory;
    private boolean refreshingDeckType;
    private boolean firstactivation = true;

    private final DeckManager lstDecks;
    private final FButton btnNewDeck = new FButton(Localizer.getInstance().getMessage("lblNewDeck"));
    private final FButton btnEditDeck = new FButton(Localizer.getInstance().getMessage("btnEditDeck"));
    private final FButton btnViewDeck = new FButton(Localizer.getInstance().getMessage("lblViewDeck"));
    private final FButton btnRandom = new FButton(Localizer.getInstance().getMessage("lblRandomDeck"));

    private RegisteredPlayer player;
    private boolean isAi;

    private final ForgePreferences prefs = FModel.getPreferences();
    private final Localizer localizer = Localizer.getInstance();
    private FPref stateSetting = null;
    private FOptionPane optionPane;

    //Show dialog to select a deck
    public static void promptForDeck(String title, GameType gameType, boolean forAi, final Callback<Deck> callback) {
        FThreads.assertExecutedByEdt(true);

        final FDeckChooser deckChooser = new FDeckChooser(gameType, forAi, null);

        //use container to contain both combo box and deck list
        final FContainer container = new FContainer() {
            @Override
            protected void doLayout(final float width, final float height) {
                float x = 0;
                float y = ItemFilter.PADDING;
                float fieldHeight = deckChooser.cmbDeckTypes.getHeight();
                deckChooser.cmbDeckTypes.setBounds(x, y, width, fieldHeight);
                y += fieldHeight + 1;
                deckChooser.lstDecks.setBounds(x, y, width, height - y);
            }
        };
        container.add(deckChooser.cmbDeckTypes);
        container.add(deckChooser.lstDecks);
        container.setHeight(FOptionPane.getMaxDisplayObjHeight());

        deckChooser.optionPane = new FOptionPane(null, null, title, null, container, ImmutableList.of(Localizer.getInstance().getMessage("lblOK"), Localizer.getInstance().getMessage("lblCancel")), 0, new Callback<Integer>() {
            @Override
            public void run(Integer result) {
                if (result == 0) {
                    if (callback != null) {
                        callback.run(deckChooser.getDeck());
                    }
                }
            }
        }) {
            @Override
            protected boolean padAboveAndBelow() {
                return false; //allow list to go straight up against buttons
            }
        };
        deckChooser.optionPane.show();
    }

    public FDeckChooser(GameType gameType0, boolean isAi0, FEventHandler selectionChangedHandler) {
        super("");
        lstDecks = new DeckManager(gameType0);
        isAi = isAi0;

        lstDecks.setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (lstDecks.getGameType() == GameType.DeckManager) {
                    //for Deck Editor, edit deck instead of accepting
                    editSelectedDeck();
                    return;
                }
                accept();
            }
        });
        btnNewDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                createNewDeck();
            }
        });
        btnEditDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editSelectedDeck();
            }
        });
        btnViewDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (selectedDeckType != DeckType.STANDARD_COLOR_DECK && selectedDeckType != DeckType.STANDARD_CARDGEN_DECK
                        && selectedDeckType != DeckType.PIONEER_CARDGEN_DECK
                        && selectedDeckType != DeckType.MODERN_CARDGEN_DECK && selectedDeckType != DeckType.LEGACY_CARDGEN_DECK
                        && selectedDeckType != DeckType.VINTAGE_CARDGEN_DECK && selectedDeckType != DeckType.MODERN_COLOR_DECK &&
                        selectedDeckType != DeckType.COLOR_DECK && selectedDeckType != DeckType.THEME_DECK
                        && selectedDeckType != DeckType.RANDOM_COMMANDER_DECK && selectedDeckType != DeckType.RANDOM_CARDGEN_COMMANDER_DECK) {
                    FDeckViewer.show(getDeck());
                }
            }
        });
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (lstDecks.getGameType() == GameType.DeckManager) {
                    //for Deck Editor, test deck instead of randomly selecting deck
                    testSelectedDeck();
                    return;
                }
                if (selectedDeckType == DeckType.COLOR_DECK || selectedDeckType == DeckType.STANDARD_COLOR_DECK
                        || selectedDeckType == DeckType.MODERN_COLOR_DECK) {
                    DeckgenUtil.randomSelectColors(lstDecks);
                }
                else if (selectedDeckType == DeckType.STANDARD_CARDGEN_DECK){
                    DeckgenUtil.randomSelect(lstDecks);
                }
                else if (selectedDeckType == DeckType.PIONEER_CARDGEN_DECK){
                    DeckgenUtil.randomSelect(lstDecks);
                }
                else if (selectedDeckType == DeckType.MODERN_CARDGEN_DECK){
                    DeckgenUtil.randomSelect(lstDecks);
                }
                else if (selectedDeckType == DeckType.LEGACY_CARDGEN_DECK){
                    DeckgenUtil.randomSelect(lstDecks);
                }
                else if (selectedDeckType == DeckType.VINTAGE_CARDGEN_DECK){
                    DeckgenUtil.randomSelect(lstDecks);
                }
                else {
                    DeckgenUtil.randomSelect(lstDecks);
                }
                accept();
            }
        });
        switch (lstDecks.getGameType()) {
        case Constructed:
            break; //delay initialize for constructed until saved decks can be reloaded
        case Commander:
        case Oathbreaker:
        case TinyLeaders:
        case Brawl:
        case Gauntlet:
            initialize(null, DeckType.CUSTOM_DECK);
            break;
        case DeckManager:
            initialize(null, DeckPreferences.getSelectedDeckType());
            break;
        default:
            initialize(null, DeckType.RANDOM_DECK);
            break;
        }
        lstDecks.setSelectionChangedHandler(selectionChangedHandler);
    }

    private void accept() {
        if (optionPane == null) {
            Forge.back();
            if (callback != null) {
                callback.run(getDeck());
            }
        }
        else {
            optionPane.setResult(0);
        }
    }

    @Override
    public void onActivate() {
        //somehow a loaded deck state from startup don't refresh accordingly for imageview so refresh it on first activation
        if(firstactivation) {
            needRefreshOnActivate = true;
            firstactivation = false;
        }
        if (needRefreshOnActivate) {
            needRefreshOnActivate = false;
            refreshDecksList(selectedDeckType, true, null);
            switch (lstDecks.getGameType()) {
            case Commander:
                lstDecks.setSelectedString(DeckPreferences.getCommanderDeck());
                break;
            case Oathbreaker:
                lstDecks.setSelectedString(DeckPreferences.getOathbreakerDeck());
                break;
            case TinyLeaders:
                lstDecks.setSelectedString(DeckPreferences.getTinyLeadersDeck());
                break;
            case Brawl:
                lstDecks.setSelectedString(DeckPreferences.getBrawlDeck());
                break;
            case Archenemy:
                lstDecks.setSelectedString(DeckPreferences.getSchemeDeck());
                break;
            case Planechase:
                lstDecks.setSelectedString(DeckPreferences.getPlanarDeck());
                break;
            case DeckManager:
                switch (selectedDeckType) {
                case COMMANDER_DECK:
                    lstDecks.setSelectedString(DeckPreferences.getCommanderDeck());
                    break;
                case OATHBREAKER_DECK:
                    lstDecks.setSelectedString(DeckPreferences.getOathbreakerDeck());
                    break;
                case TINY_LEADERS_DECK:
                    lstDecks.setSelectedString(DeckPreferences.getTinyLeadersDeck());
                    break;
                case BRAWL_DECK:
                    lstDecks.setSelectedString(DeckPreferences.getBrawlDeck());
                    break;
                case SCHEME_DECK:
                    lstDecks.setSelectedString(DeckPreferences.getSchemeDeck());
                    break;
                case PLANAR_DECK:
                    lstDecks.setSelectedString(DeckPreferences.getPlanarDeck());
                    break;
                case DRAFT_DECK:
                    lstDecks.setSelectedString(DeckPreferences.getDraftDeck());
                    break;
                case SEALED_DECK:
                    lstDecks.setSelectedString(DeckPreferences.getSealedDeck());
                    break;
                default:
                    lstDecks.setSelectedString(DeckPreferences.getCurrentDeck());
                    break;
                }
                break;
            default:
                lstDecks.setSelectedString(DeckPreferences.getCurrentDeck());
                break;
            }
        }
    }

    private boolean isGeneratedDeck(DeckType deckType) {
        switch (deckType) {
            case COLOR_DECK:
            case STANDARD_COLOR_DECK:
            case STANDARD_CARDGEN_DECK:
            case RANDOM_CARDGEN_COMMANDER_DECK:
            case RANDOM_COMMANDER_DECK:
            case MODERN_CARDGEN_DECK:
            case PIONEER_CARDGEN_DECK:
            case LEGACY_CARDGEN_DECK:
            case VINTAGE_CARDGEN_DECK:
            case MODERN_COLOR_DECK:
            case THEME_DECK:
            case RANDOM_DECK:
                return true;
            default:
                return false;
        }
    }
    private void createNewDeck() {
        final FDeckEditor[] editor = new FDeckEditor[1];
        final DeckProxy deck = lstDecks.getSelectedItem();
        String overlayText = localizer.getMessage("lblLoading");;
        if (selectedDeckType == DeckType.DRAFT_DECK) {
            NewGameScreen.BoosterDraft.open();
            return;
        }
        if (selectedDeckType == DeckType.SEALED_DECK) {
            NewGameScreen.SealedDeck.open();
            return;
        }
        if (isGeneratedDeck(selectedDeckType)) {
            overlayText += localizer.getMessage("lblDeck");
            if (deck == null) {
                FOptionPane.showErrorDialog(localizer.getMessage("lblMustSelectGenerateNewDeck"));
                return;
            }
        } else {
            overlayText += localizer.getMessage("lblCatalog");
        }
        String finalOverlay = overlayText;
        FThreads.invokeInEdtLater(new Runnable() {
            @Override
            public void run() {
                LoadingOverlay.show(finalOverlay, new Runnable() {
                    @Override
                    public void run() {
                        if (isGeneratedDeck(selectedDeckType)) {
                            Deck generatedDeck = deck.getDeck();
                            if (generatedDeck == null) { return; }
                            generatedDeck = (Deck)generatedDeck.copyTo(""); //prevent deck having a name by default
                            editor[0] = new FDeckEditor(getEditorType(), generatedDeck, true);
                        } else {
                            editor[0] = new FDeckEditor(getEditorType(), "", false);
                        }
                        editor[0].setSaveHandler(new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                //ensure user returns to proper deck type and that list is refreshed if new deck is saved
                                if (!needRefreshOnActivate) {
                                    needRefreshOnActivate = true;
                                    if (lstDecks.getGameType() == GameType.DeckManager) {
                                        switch (selectedDeckType) {
                                            case COMMANDER_DECK:
                                            case OATHBREAKER_DECK:
                                            case TINY_LEADERS_DECK:
                                            case BRAWL_DECK:
                                            case SCHEME_DECK:
                                            case PLANAR_DECK:
                                            case DRAFT_DECK:
                                            case SEALED_DECK:
                                                break;
                                            default:
                                                setSelectedDeckType(DeckType.CONSTRUCTED_DECK);
                                                break;
                                        }
                                    }
                                    else {
                                        setSelectedDeckType(DeckType.CUSTOM_DECK);
                                    }
                                }
                            }
                        });
                        Forge.openScreen(editor[0]);
                    }
                });
            }
        });
    }

    private void editSelectedDeck() {
        final DeckProxy deck = lstDecks.getSelectedItem();
        if (deck == null) { return; }

        switch (selectedDeckType) {
        case CUSTOM_DECK:
        case CONSTRUCTED_DECK:
        case COMMANDER_DECK:
        case OATHBREAKER_DECK:
        case TINY_LEADERS_DECK:
        case BRAWL_DECK:
        case SCHEME_DECK:
        case PLANAR_DECK:
        case DRAFT_DECK:
        case SEALED_DECK:
            editDeck(deck);
            break;
        default:
            final DeckType fallbackType = lstDecks.getGameType() == GameType.DeckManager ? DeckType.CONSTRUCTED_DECK : DeckType.CUSTOM_DECK;

            //see if deck with selected name exists already
            final IStorage<Deck> decks = FModel.getDecks().getConstructed();
            Deck existingDeck = decks.get(deck.getName());
            if (existingDeck != null) {
                setSelectedDeckType(fallbackType);
                editDeck(new DeckProxy(existingDeck, "Constructed", lstDecks.getGameType(), decks));
                return;
            }


            //prompt to duplicate deck if deck doesn't exist already
            FOptionPane.showConfirmDialog(selectedDeckType + " " + localizer.getMessage("lblCannotEditDuplicateCustomDeck").replace("%s", deck.getName()),
                    localizer.getMessage("lblDuplicateDeck"), localizer.getMessage("lblDuplicate"), localizer.getMessage("lblCancel"), new Callback<Boolean>() {
                @Override
                public void run(Boolean result) {
                    if (result) {
                        Deck copiedDeck = (Deck)deck.getDeck().copyTo(deck.getName());
                        decks.add(copiedDeck);
                        setSelectedDeckType(fallbackType);
                        editDeck(new DeckProxy(copiedDeck, "Constructed", lstDecks.getGameType(), decks));
                    }
                }
            });
            break;
        }
    }

    private EditorType getEditorType() {
        switch (lstDecks.getGameType()) {
        case DeckManager:
            switch (selectedDeckType) {
            case COMMANDER_DECK:
                return EditorType.Commander;
            case OATHBREAKER_DECK:
                return EditorType.Oathbreaker;
            case TINY_LEADERS_DECK:
                return EditorType.TinyLeaders;
            case BRAWL_DECK:
                return EditorType.Brawl;
            case SCHEME_DECK:
                return EditorType.Archenemy;
            case PLANAR_DECK:
                return EditorType.Planechase;
            case DRAFT_DECK:
                return EditorType.Draft;
            case SEALED_DECK:
                return EditorType.Sealed;
            default:
                return EditorType.Constructed;
            }
        case Commander:
            return EditorType.Commander;
        case Oathbreaker:
            return EditorType.Oathbreaker;
        case TinyLeaders:
            return EditorType.TinyLeaders;
        case Brawl:
            return EditorType.Brawl;
        case Archenemy:
            return EditorType.Archenemy;
        case Planechase:
            return EditorType.Planechase;
        default:
            return EditorType.Constructed;
        }
    }

    private void editDeck(DeckProxy deck) {
        EditorType editorType = getEditorType();
        switch (editorType) {
        case Commander:
            DeckPreferences.setCommanderDeck(deck.getName());
            break;
        case Oathbreaker:
            DeckPreferences.setOathbreakerDeck(deck.getName());
            break;
        case TinyLeaders:
            DeckPreferences.setTinyLeadersDeck(deck.getName());
            break;
        case Archenemy:
            DeckPreferences.setSchemeDeck(deck.getName());
            break;
        case Planechase:
            DeckPreferences.setPlanarDeck(deck.getName());
            break;
        case Draft:
            DeckPreferences.setDraftDeck(deck.getName());
            break;
        case Sealed:
            DeckPreferences.setSealedDeck(deck.getName());
            break;
        case Constructed:
            DeckPreferences.setCurrentDeck(deck.getName());
            break;
        default:
            break;
        }
        needRefreshOnActivate = true;
        /*preload deck to cache*/
        ImageCache.preloadCache(deck.getDeck());
        Forge.openScreen(new FDeckEditor(editorType, deck, true));
    }

    public void initialize(FPref savedStateSetting, DeckType defaultDeckType) {
        stateSetting = savedStateSetting;
        selectedDeckType = defaultDeckType;

        if (cmbDeckTypes == null) { //initialize components with delayed initialization the first time this is populated
            cmbDeckTypes = new FComboBox<>();
            switch (lstDecks.getGameType()) {
            case Constructed:
            case Gauntlet:
                cmbDeckTypes.addItem(DeckType.CUSTOM_DECK);
                cmbDeckTypes.addItem(DeckType.PRECONSTRUCTED_DECK);
                cmbDeckTypes.addItem(DeckType.QUEST_OPPONENT_DECK);
                cmbDeckTypes.addItem(DeckType.COLOR_DECK);
                cmbDeckTypes.addItem(DeckType.STANDARD_COLOR_DECK);
                if(FModel.isdeckGenMatrixLoaded()) {
                    cmbDeckTypes.addItem(DeckType.STANDARD_CARDGEN_DECK);
                    cmbDeckTypes.addItem(DeckType.PIONEER_CARDGEN_DECK);
                    cmbDeckTypes.addItem(DeckType.MODERN_CARDGEN_DECK);
                    cmbDeckTypes.addItem(DeckType.LEGACY_CARDGEN_DECK);
                    cmbDeckTypes.addItem(DeckType.VINTAGE_CARDGEN_DECK);
                }
                cmbDeckTypes.addItem(DeckType.MODERN_COLOR_DECK);
                cmbDeckTypes.addItem(DeckType.THEME_DECK);
                cmbDeckTypes.addItem(DeckType.RANDOM_DECK);
                cmbDeckTypes.addItem(DeckType.NET_DECK);
                break;
            case Commander:
            case Oathbreaker:
            case TinyLeaders:
            case Brawl:
                cmbDeckTypes.addItem(DeckType.CUSTOM_DECK);
                cmbDeckTypes.addItem(DeckType.PRECON_COMMANDER_DECK);
                cmbDeckTypes.addItem(DeckType.RANDOM_DECK);
                if(FModel.isdeckGenMatrixLoaded()) {
                    cmbDeckTypes.addItem(DeckType.RANDOM_CARDGEN_COMMANDER_DECK);
                }
                cmbDeckTypes.addItem(DeckType.RANDOM_COMMANDER_DECK);
                cmbDeckTypes.addItem(DeckType.NET_DECK);
                break;
            case DeckManager:
                cmbDeckTypes.addItem(DeckType.CONSTRUCTED_DECK);
                cmbDeckTypes.addItem(DeckType.COMMANDER_DECK);
                cmbDeckTypes.addItem(DeckType.OATHBREAKER_DECK);
                cmbDeckTypes.addItem(DeckType.TINY_LEADERS_DECK);
                cmbDeckTypes.addItem(DeckType.BRAWL_DECK);
                cmbDeckTypes.addItem(DeckType.SCHEME_DECK);
                cmbDeckTypes.addItem(DeckType.PLANAR_DECK);
                cmbDeckTypes.addItem(DeckType.DRAFT_DECK);
                cmbDeckTypes.addItem(DeckType.SEALED_DECK);
                cmbDeckTypes.addItem(DeckType.PRECONSTRUCTED_DECK);
                cmbDeckTypes.addItem(DeckType.PRECON_COMMANDER_DECK);
                cmbDeckTypes.addItem(DeckType.QUEST_OPPONENT_DECK);
                cmbDeckTypes.addItem(DeckType.NET_DECK);
                cmbDeckTypes.addItem(DeckType.NET_COMMANDER_DECK);
                break;
            default:
                cmbDeckTypes.addItem(DeckType.CUSTOM_DECK);
                cmbDeckTypes.addItem(DeckType.RANDOM_DECK);
                break;
            }
            cmbDeckTypes.setAlignment(Align.center);
            restoreSavedState();
            cmbDeckTypes.setChangedHandler(new FEventHandler() {
                @Override
                public void handleEvent(final FEvent e) {
                    final DeckType deckType = cmbDeckTypes.getSelectedItem();
                    if (!refreshingDeckType && (deckType == DeckType.NET_DECK || deckType == DeckType.NET_COMMANDER_DECK)) {
                        FThreads.invokeInBackgroundThread(new Runnable() { //needed for loading net decks
                            @Override
                            public void run() {
                                GameType gameType = lstDecks.getGameType();
                                if (gameType == GameType.DeckManager) {
                                    gameType = deckType == DeckType.NET_COMMANDER_DECK ? GameType.Commander : GameType.Constructed;
                                }
                                final NetDeckCategory category = NetDeckCategory.selectAndLoad(gameType);

                                FThreads.invokeInEdtLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (category == null) {
                                            cmbDeckTypes.setSelectedItem(selectedDeckType); //restore old selection if user cancels
                                            if (selectedDeckType == deckType && netDeckCategory != null) {
                                                cmbDeckTypes.setText(netDeckCategory.getDeckType());
                                            }
                                            return;
                                        }

                                        netDeckCategory = category;
                                        refreshDecksList(deckType, true, e);
                                    }
                                });
                            }
                        });
                        return;
                    }
                    refreshDecksList(deckType, false, e);
                }
            });
            add(cmbDeckTypes);
            add(lstDecks);
            add(btnNewDeck);
            add(btnEditDeck);
            add(btnViewDeck);
            add(btnRandom);
        }
        else {
            restoreSavedState(); //ensure decks refreshed and state restored in case any deleted or added since last loaded
        }
    }

    public void refreshDeckListForAI(){
        //remember current deck by name, refresh decklist for AI/Human then reselect if possible
        String currentName= lstDecks.getSelectedItem().getName();
        refreshDecksList(selectedDeckType,true,null);
        lstDecks.setSelectedString(currentName);
        saveState();
    }

    private void refreshDecksList(DeckType deckType, boolean forceRefresh, FEvent e) {
        if (selectedDeckType == deckType && !forceRefresh) { return; }
        selectedDeckType = deckType;

        if (e == null) {
            refreshingDeckType = true;
            cmbDeckTypes.setSelectedItem(deckType);
            refreshingDeckType = false;
        }
        if (deckType == null) { return; }

        int maxSelections = 1;
        Iterable<DeckProxy> pool;
        ItemManagerConfig config;

        switch (deckType) {
        case CUSTOM_DECK:
            switch (lstDecks.getGameType()) {
            case Commander:
                pool = DeckProxy.getAllCommanderDecks();
                config = ItemManagerConfig.COMMANDER_DECKS;
                break;
            case Oathbreaker:
                pool = DeckProxy.getAllOathbreakerDecks();
                config = ItemManagerConfig.COMMANDER_DECKS;
                break;
            case TinyLeaders:
                pool = DeckProxy.getAllTinyLeadersDecks();
                config = ItemManagerConfig.COMMANDER_DECKS;
                break;
            case Brawl:
                pool = DeckProxy.getAllBrawlDecks();
                config = ItemManagerConfig.COMMANDER_DECKS;
                break;
            case Archenemy:
                pool = DeckProxy.getAllSchemeDecks();
                config = ItemManagerConfig.SCHEME_DECKS;
                break;
            case Planechase:
                pool = DeckProxy.getAllPlanarDecks();
                config = ItemManagerConfig.PLANAR_DECKS;
                break;
            default:
                pool = DeckProxy.getAllConstructedDecks();
                config = ItemManagerConfig.CONSTRUCTED_DECKS;
                break;
            }
            break;
        case CONSTRUCTED_DECK:
            pool = DeckProxy.getAllConstructedDecks();
            config = ItemManagerConfig.CONSTRUCTED_DECKS;
            break;
        case COMMANDER_DECK:
            pool = DeckProxy.getAllCommanderDecks();
            config = ItemManagerConfig.COMMANDER_DECKS;
            break;
        case PRECON_COMMANDER_DECK:
            pool = DeckProxy.getAllCommanderPreconDecks();
            config = ItemManagerConfig.COMMANDER_DECKS;
            break;
        case OATHBREAKER_DECK:
            pool = DeckProxy.getAllOathbreakerDecks();
            config = ItemManagerConfig.COMMANDER_DECKS;
            break;
        case TINY_LEADERS_DECK:
            pool = DeckProxy.getAllTinyLeadersDecks();
            config = ItemManagerConfig.COMMANDER_DECKS;
            break;
        case BRAWL_DECK:
            pool = DeckProxy.getAllBrawlDecks();
            config = ItemManagerConfig.COMMANDER_DECKS;
            break;
        case RANDOM_COMMANDER_DECK:
            pool = CommanderDeckGenerator.getCommanderDecks(lstDecks.getGameType().getDeckFormat(),isAi, false);
            config = ItemManagerConfig.STRING_ONLY;
            break;
        case RANDOM_CARDGEN_COMMANDER_DECK:
            pool= new ArrayList<>();
            if(FModel.isdeckGenMatrixLoaded()) {
                pool = CommanderDeckGenerator.getCommanderDecks(lstDecks.getGameType().getDeckFormat(), isAi, true);
            }
            config = ItemManagerConfig.STRING_ONLY;
            break;
        case SCHEME_DECK:
            pool = DeckProxy.getAllSchemeDecks();
            config = ItemManagerConfig.SCHEME_DECKS;
            break;
        case PLANAR_DECK:
            pool = DeckProxy.getAllPlanarDecks();
            config = ItemManagerConfig.PLANAR_DECKS;
            break;
        case DRAFT_DECK:
            pool = DeckProxy.getAllDraftDecks();
            config = ItemManagerConfig.DRAFT_DECKS;
            break;
        case SEALED_DECK:
            pool = DeckProxy.getAllSealedDecks();
            config = ItemManagerConfig.SEALED_DECKS;
            break;
        case COLOR_DECK:
            maxSelections = 3;
            pool = ColorDeckGenerator.getColorDecks(lstDecks, null, isAi);
            config = ItemManagerConfig.STRING_ONLY;
            break;
        case STANDARD_COLOR_DECK:
            maxSelections = 3;
            pool = ColorDeckGenerator.getColorDecks(lstDecks, FModel.getFormats().getStandard().getFilterPrinted(), isAi);
            config = ItemManagerConfig.STRING_ONLY;
            break;
        case STANDARD_CARDGEN_DECK:
            maxSelections = 1;
            pool= new ArrayList<>();
            if(FModel.isdeckGenMatrixLoaded()) {
                pool = ArchetypeDeckGenerator.getMatrixDecks(FModel.getFormats().getStandard(), isAi);
            }
            config = ItemManagerConfig.STRING_ONLY;
            break;
        case PIONEER_CARDGEN_DECK:
            maxSelections = 1;
            pool= new ArrayList<>();
            if(FModel.isdeckGenMatrixLoaded()) {
                pool = ArchetypeDeckGenerator.getMatrixDecks(FModel.getFormats().getPioneer(), isAi);
            }
            config = ItemManagerConfig.STRING_ONLY;
            break;
        case MODERN_CARDGEN_DECK:
            maxSelections = 1;
            pool= new ArrayList<>();
            if(FModel.isdeckGenMatrixLoaded()) {
                pool = ArchetypeDeckGenerator.getMatrixDecks(FModel.getFormats().getModern(), isAi);
            }
            config = ItemManagerConfig.STRING_ONLY;
            break;
        case LEGACY_CARDGEN_DECK:
            maxSelections = 1;
            pool= new ArrayList<>();
            if(FModel.isdeckGenMatrixLoaded()) {
                pool = ArchetypeDeckGenerator.getMatrixDecks(FModel.getFormats().get("Legacy"), isAi);
            }
            config = ItemManagerConfig.STRING_ONLY;
            break;
        case VINTAGE_CARDGEN_DECK:
            maxSelections = 1;
            pool= new ArrayList<>();
            if(FModel.isdeckGenMatrixLoaded()) {
                pool = ArchetypeDeckGenerator.getMatrixDecks(FModel.getFormats().get("Vintage"), isAi);
            }
            config = ItemManagerConfig.STRING_ONLY;
            break;
        case MODERN_COLOR_DECK:
            maxSelections = 3;
            pool = ColorDeckGenerator.getColorDecks(lstDecks, FModel.getFormats().getModern().getFilterPrinted(), isAi);
            config = ItemManagerConfig.STRING_ONLY;
            break;
        case THEME_DECK:
            pool = DeckProxy.getAllThemeDecks();
            config = ItemManagerConfig.STRING_ONLY;
            break;
        case QUEST_OPPONENT_DECK:
            pool = DeckProxy.getAllQuestEventAndChallenges();
            config = ItemManagerConfig.QUEST_EVENT_DECKS;
            break;
        case PRECONSTRUCTED_DECK:
            pool = DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons());
            config = ItemManagerConfig.PRECON_DECKS;
            break;
        case RANDOM_DECK:
            pool = RandomDeckGenerator.getRandomDecks(lstDecks, isAi);
            config = ItemManagerConfig.STRING_ONLY;
            break;
        case NET_DECK:
        case NET_COMMANDER_DECK:
            if (netDeckCategory != null) {
                cmbDeckTypes.setText(netDeckCategory.getDeckType());
            }
            pool = DeckProxy.getNetDecks(netDeckCategory);
            config = ItemManagerConfig.NET_DECKS;
            break;
        default:
            BugReporter.reportBug("Unsupported deck type: " + deckType);
            return;
        }

        lstDecks.setSelectionSupport(1, maxSelections);
        lstDecks.setPool(pool);
        lstDecks.setup(config);

        if (config == ItemManagerConfig.STRING_ONLY) {
            //hide edit/view buttons for string-only lists
            if (Forge.isLandscapeMode()) {
                btnNewDeck.setWidth((getWidth() - 3 * PADDING) / 2);
            }
            else {
                btnNewDeck.setWidth(getWidth() - 2 * PADDING);
            }
            btnEditDeck.setVisible(false);
            btnViewDeck.setVisible(false);
            btnRandom.setWidth(btnNewDeck.getWidth());

            btnNewDeck.setText(localizer.getMessage("lblGenerateNewDeck"));
            switch (deckType) {
            case COLOR_DECK:
                btnRandom.setText(localizer.getMessage("lblRandomColors"));
                break;
            case THEME_DECK:
                btnRandom.setText(localizer.getMessage("lblRandomTheme"));
                break;
            default:
                btnRandom.setText(localizer.getMessage("lblRandomDeck"));
                break;
            }
        }
        else {
            btnNewDeck.setWidth(btnEditDeck.getWidth());
            btnEditDeck.setVisible(true);
            btnViewDeck.setVisible(true);
            btnRandom.setWidth(btnNewDeck.getWidth());

            btnNewDeck.setText(localizer.getMessage("lblNewDeck"));

            if (lstDecks.getGameType() == GameType.DeckManager) {
                //handle special case of Deck Editor screen where this button will start a game with the deck
                btnRandom.setText(localizer.getMessage("lblTestDeck"));

                switch (selectedDeckType) {
                case SCHEME_DECK:
                case PLANAR_DECK: //don't allow testing secondary decks this way
                    btnRandom.setEnabled(false);
                    break;
                default:
                    btnRandom.setEnabled(true);
                    break;
                }
            }
            else {
                btnRandom.setText(localizer.getMessage("lblRandomDeck"));
            }
        }

        btnRandom.setLeft(getWidth() - PADDING - btnRandom.getWidth());

        if (e != null) { //set default list selection if from combo box change event
            if (deckType == DeckType.COLOR_DECK) {
                // default selection = basic two color deck
                lstDecks.setSelectedIndices(new Integer[]{0, 1});
            }
            else {
                lstDecks.setSelectedIndex(0);
            }
            if (lstDecks.getGameType() == GameType.DeckManager) {
                DeckPreferences.setSelectedDeckType(deckType); //update saved Deck Manager type
            }
        }
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        width -= 2 * x;

        float fieldHeight = cmbDeckTypes.getHeight();
        float totalButtonHeight;
        boolean landscapeMode = Forge.isLandscapeMode();
        if (landscapeMode) {
            totalButtonHeight = fieldHeight;
        }
        else {
            totalButtonHeight = 2 * fieldHeight + PADDING;
        }

        cmbDeckTypes.setBounds(x, y, width, fieldHeight);
        y += cmbDeckTypes.getHeight() + 1;
        lstDecks.setBounds(x, y, width, height - y - totalButtonHeight - 2 * PADDING); //leave room for buttons at bottom

        y += lstDecks.getHeight() + PADDING;
        float buttonWidth;
        if (landscapeMode) {
            buttonWidth = (width - 3 * PADDING) / 4;
        }
        else {
            buttonWidth = (width - PADDING) / 2;
        }

        if (btnEditDeck.isVisible()) {
            btnNewDeck.setBounds(x, y, buttonWidth, fieldHeight);
        }
        else if (landscapeMode) {
            btnNewDeck.setBounds(x, y, 2 * buttonWidth + PADDING, fieldHeight);
        }
        else {
            btnNewDeck.setBounds(x, y, width, fieldHeight);
        }
        btnEditDeck.setBounds(x + buttonWidth + PADDING, y, buttonWidth, fieldHeight);
        if (landscapeMode) {
            x += 2 * (buttonWidth + PADDING);
        }
        else {
            y += fieldHeight + PADDING;
        }

        btnViewDeck.setBounds(x, y, buttonWidth, fieldHeight);
        if (btnViewDeck.isVisible()) {
            btnRandom.setBounds(x + buttonWidth + PADDING, y, buttonWidth, fieldHeight);
        }
        else if (landscapeMode) {
            btnRandom.setBounds(x, y, 2 * buttonWidth + PADDING, fieldHeight);
        }
        else {
            btnRandom.setBounds(x, y, width, fieldHeight);
        }
    }

    public DeckType getSelectedDeckType() { return selectedDeckType; }
    public void setSelectedDeckType(DeckType selectedDeckType0) {
        refreshDecksList(selectedDeckType0, false, null);
    }

    public DeckManager getLstDecks() { return lstDecks; }

    public Deck getDeck() {
        /*if(selectedDeckType.equals(DeckType.STANDARD_CARDGEN_DECK)){
            return DeckgenUtil.buildCardGenDeck(lstDecks.getSelectedItem().getName());
        }*/
        //ensure a deck is selected first
        if(lstDecks.getSelectedIndex() == -1){
            lstDecks.setSelectedIndex(0);
        }
        DeckProxy proxy = lstDecks.getSelectedItem();
        if (proxy == null) { return null; }
        return proxy.getDeck();
    }

    /** Generates deck from current list selection(s). */
    public RegisteredPlayer getPlayer(boolean forceRefresh) {
        if (player != null && !forceRefresh) {
            return player;
        }

        if (lstDecks.getSelectedIndex() < 0) {
            player = null;
        }
        else if (selectedDeckType == DeckType.QUEST_OPPONENT_DECK) {
            //Special branch for quest events
            QuestEvent event = DeckgenUtil.getQuestEvent(lstDecks.getSelectedItem().getName());
            player = new RegisteredPlayer(event.getEventDeck());
            if (event instanceof QuestEventChallenge) {
                player.setStartingLife(((QuestEventChallenge) event).getAiLife());
            }
            player.setCardsOnBattlefield(QuestUtil.getComputerStartingCards(event));
        }
        else {
            player = new RegisteredPlayer(getDeck());
        }
        return player;
    }

    public final boolean isAi() {
        return isAi;
    }

    public void setIsAi(boolean isAiDeck) {
        isAi = isAiDeck;
    }

    private final String SELECTED_DECK_DELIMITER = "::";

    public void saveState() {
        if (stateSetting == null) {
            throw new NullPointerException("State setting missing. Specify first using the initialize() method.");
        }
        prefs.setPref(stateSetting, getState());
        prefs.save();
    }

    private String getState() {
        StringBuilder state = new StringBuilder();
        if (cmbDeckTypes.getSelectedItem() == null || cmbDeckTypes.getSelectedItem() == DeckType.NET_DECK) {
            //handle special case of net decks
            if (netDeckCategory == null) { return ""; }
            state.append(NetDeckCategory.PREFIX).append(netDeckCategory.getName());
        }
        else {
            state.append(cmbDeckTypes.getSelectedItem().name());
        }
        state.append(";");
        joinSelectedDecks(state, SELECTED_DECK_DELIMITER);
        return state.toString();
    }

    private void joinSelectedDecks(StringBuilder state, String delimiter) {
        Iterable<DeckProxy> selectedDecks = lstDecks.getSelectedItems();
        boolean isFirst = true;
        if (selectedDecks != null) {
            for (DeckProxy deck : selectedDecks) {
                if (isFirst) {
                    isFirst = false;
                }
                else {
                    state.append(delimiter);
                }
                state.append(deck.toString());
            }
        }
    }

    private void restoreSavedState() {
        DeckType oldDeckType = selectedDeckType;
        if (stateSetting == null) {
            //if can't restore saved state, just refresh deck list
            refreshDecksList(oldDeckType, true, null);
            return;
        }

        String savedState = prefs.getPref(stateSetting);
        refreshDecksList(getDeckTypeFromSavedState(savedState), true, null);
        if (!lstDecks.setSelectedStrings(getSelectedDecksFromSavedState(savedState))) {
            //if can't select old decks, just refresh deck list
            refreshDecksList(oldDeckType, true, null);
        }
    }

    private DeckType getDeckTypeFromSavedState(String savedState) {
        try {
            if (StringUtils.isBlank(savedState)) {
                return selectedDeckType;
            }
            else {
                String deckType = savedState.split(";")[0];
                if (deckType.startsWith(NetDeckCategory.PREFIX)) {
                    netDeckCategory = NetDeckCategory.selectAndLoad(lstDecks.getGameType(), deckType.substring(NetDeckCategory.PREFIX.length()));
                    return DeckType.NET_DECK;
                }
                return DeckType.valueOf(deckType);
            }
        }
        catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage() + ". Using default : " + selectedDeckType);
            return selectedDeckType;
        }
    }

    private List<String> getSelectedDecksFromSavedState(String savedState) {
        try {
            if (StringUtils.isBlank(savedState)) {
                return new ArrayList<>();
            }
            else {
                return Arrays.asList(savedState.split(";")[1].split(SELECTED_DECK_DELIMITER));
            }
        }
        catch (Exception ex) {
            System.err.println(ex + " [savedState=" + savedState + "]");
            return new ArrayList<>();
        }
    }

    public FComboBox<DeckType> getDecksComboBox() {
        return cmbDeckTypes;
    }

    //create quick gauntlet for testing deck
    private void testSelectedDeck() {
        final DeckProxy deckProxy = lstDecks.getSelectedItem();
        if (deckProxy == null) { return; }
        final Deck userDeck = deckProxy.getDeck();
        if (userDeck == null) { return; }

        if (selectedDeckType == DeckType.COMMANDER_DECK || selectedDeckType == DeckType.NET_COMMANDER_DECK) {
            //cannot create gauntlet for commander decks, so just start single match
            testVariantDeck(userDeck, GameType.Commander);
            return;
        }

        if (selectedDeckType == DeckType.OATHBREAKER_DECK) {
            //cannot create gauntlet for oathbreaker decks, so just start single match
            testVariantDeck(userDeck, GameType.Oathbreaker);
            return;
        }

        if (selectedDeckType == DeckType.TINY_LEADERS_DECK) {
            //cannot create gauntlet for tiny leaders decks, so just start single match
            testVariantDeck(userDeck, GameType.TinyLeaders);
            return;
        }

        if (selectedDeckType == DeckType.BRAWL_DECK) {
            //cannot create gauntlet for tiny leaders decks, so just start single match
            testVariantDeck(userDeck, GameType.Brawl);
            return;
        }

        GuiChoose.getInteger(localizer.getMessage("lblHowManyOpponents"), 1, 50, new Callback<Integer>() {
            @Override
            public void run(final Integer numOpponents) {
                if (numOpponents == null) { return; }
                List<DeckType> deckTypes = Arrays.asList(
                        DeckType.CUSTOM_DECK,
                        DeckType.PRECONSTRUCTED_DECK,
                        DeckType.QUEST_OPPONENT_DECK,
                        DeckType.COLOR_DECK,
                        DeckType.STANDARD_COLOR_DECK,
                        DeckType.STANDARD_CARDGEN_DECK,
                        DeckType.MODERN_COLOR_DECK,
                        DeckType.PIONEER_CARDGEN_DECK,
                        DeckType.MODERN_CARDGEN_DECK,
                        DeckType.LEGACY_CARDGEN_DECK,
                        DeckType.VINTAGE_CARDGEN_DECK,
                        DeckType.THEME_DECK,
                        DeckType.NET_DECK
                );
                if (!FModel.isdeckGenMatrixLoaded()) {
                    deckTypes.remove(DeckType.STANDARD_CARDGEN_DECK);
                    deckTypes.remove(DeckType.PIONEER_CARDGEN_DECK);
                    deckTypes.remove(DeckType.MODERN_CARDGEN_DECK);
                    deckTypes.remove(DeckType.LEGACY_CARDGEN_DECK);
                    deckTypes.remove(DeckType.VINTAGE_CARDGEN_DECK);
                }

                ListChooser<DeckType> chooser = new ListChooser<>(
                        localizer.getMessage("lblChooseAllowedDeckTypeOpponents"), 0, deckTypes.size(), deckTypes, null, new Callback<List<DeckType>>() {
                    @Override
                    public void run(final List<DeckType> allowedDeckTypes) {
                        if (allowedDeckTypes == null || allowedDeckTypes.isEmpty()) {
                            return;
                        }

                        FThreads.invokeInBackgroundThread(new Runnable() { //needed for loading net decks
                            @Override
                            public void run() {
                                final NetDeckCategory netCat;
                                if (allowedDeckTypes.contains(DeckType.NET_DECK)) {
                                    netCat = NetDeckCategory.selectAndLoad(GameType.Constructed);
                                } else {
                                    netCat = null;
                                }

                                FThreads.invokeInEdtLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        LoadingOverlay.show(localizer.getMessage("lblLoadingNewGame"), new Runnable() {
                                            @Override
                                            public void run() {
                                                GauntletData gauntlet = GauntletUtil.createQuickGauntlet(userDeck, numOpponents, allowedDeckTypes, netCat);
                                                FModel.setGauntletData(gauntlet);

                                                List<RegisteredPlayer> players = new ArrayList<>();
                                                RegisteredPlayer humanPlayer = new RegisteredPlayer(userDeck).setPlayer(GamePlayerUtil.getGuiPlayer());
                                                players.add(humanPlayer);
                                                players.add(new RegisteredPlayer(gauntlet.getDecks().get(gauntlet.getCompleted())).setPlayer(GamePlayerUtil.createAiPlayer()));

                                                gauntlet.startRound(players, humanPlayer);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
                chooser.show(null, false); /*setting selectMax to true will select all available option*/
            }
        });
    }

    private void testVariantDeck(final Deck userDeck, final GameType variant) {
        promptForDeck(localizer.getMessage("lblSelectOpponentDeck"), variant, true, new Callback<Deck>() {
            @Override
            public void run(final Deck aiDeck) {
                if (aiDeck == null) { return; }

                LoadingOverlay.show(localizer.getMessage("lblLoadingNewGame"), new Runnable() {
                    @Override
                    public void run() {
                        Set<GameType> appliedVariants = new HashSet<>();
                        appliedVariants.add(variant);

                        List<RegisteredPlayer> players = new ArrayList<>();
                        RegisteredPlayer humanPlayer = RegisteredPlayer.forVariants(2, appliedVariants, userDeck, null, false, null, null);
                        humanPlayer.setPlayer(GamePlayerUtil.getGuiPlayer());
                        RegisteredPlayer aiPlayer = RegisteredPlayer.forVariants(2, appliedVariants, aiDeck, null, false, null, null);
                        aiPlayer.setPlayer(GamePlayerUtil.createAiPlayer());
                        players.add(humanPlayer);
                        players.add(aiPlayer);

                        final Map<RegisteredPlayer, IGuiGame> guiMap = new HashMap<>();
                        guiMap.put(humanPlayer, MatchController.instance);

                        final HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
                        hostedMatch.startMatch(GameType.Constructed, appliedVariants, players, guiMap);
                    }
                });
            }
        });
    }

    @Override
    protected boolean allowBackInLandscapeMode() {
        return true;
    }
}
