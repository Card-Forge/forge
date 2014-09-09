package forge.deck;

import forge.FThreads;
import forge.Forge;
import forge.GuiBase;
import forge.deck.Deck;
import forge.deck.FDeckEditor.EditorType;
import forge.deck.io.DeckPreferences;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestUtil;
import forge.screens.FScreen;
import forge.toolbox.FButton;
import forge.toolbox.FComboBox;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FOptionPane;
import forge.util.Callback;
import forge.util.Utils;
import forge.util.storage.IStorage;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FDeckChooser extends FScreen {
    public static final float PADDING = Utils.scaleMin(5);

    private FComboBox<DeckType> cmbDeckTypes;
    private DeckType selectedDeckType;
    private boolean needRefreshOnActivate;
    private Callback<Deck> callback;

    private final DeckManager lstDecks;
    private final FButton btnNewDeck = new FButton("New Deck");
    private final FButton btnEditDeck = new FButton("Edit Deck");
    private final FButton btnViewDeck = new FButton("View Deck");
    private final FButton btnRandom = new FButton("Random Deck");

    private boolean isAi;

    private final ForgePreferences prefs = FModel.getPreferences();
    private FPref stateSetting = null;

    //Show screen to select a deck
    private static FDeckChooser deckChooserForPrompt;
    public static void promptForDeck(String title, GameType gameType, boolean forAi, final Callback<Deck> callback) {
        FThreads.assertExecutedByEdt(GuiBase.getInterface(), true);
        if (deckChooserForPrompt == null) {
            deckChooserForPrompt = new FDeckChooser(gameType, forAi, null);
        }
        else { //reuse same deck chooser
            deckChooserForPrompt.setIsAi(forAi);
        }
        deckChooserForPrompt.setHeaderCaption(title);
        deckChooserForPrompt.callback = callback;
        Forge.openScreen(deckChooserForPrompt);
    }

    public FDeckChooser(GameType gameType0, boolean isAi0, FEventHandler selectionChangedHandler) {
        super("");
        lstDecks = new DeckManager(gameType0);
        isAi = isAi0;

        lstDecks.setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                accept();
            }
        });
        btnNewDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                EditorType editorType;
                switch (lstDecks.getGameType()) {
                case Commander:
                    editorType = EditorType.Commander;
                    break;
                case Archenemy:
                    editorType = EditorType.Archenemy;
                    break;
                case Planechase:
                    editorType = EditorType.Planechase;
                    break;
                default:
                    editorType = EditorType.Constructed;
                    break;
                }
                FDeckEditor editor;
                switch (selectedDeckType) {
                case COLOR_DECK:
                case THEME_DECK:
                case RANDOM_DECK:
                    final DeckProxy deck = lstDecks.getSelectedItem();
                    if (deck != null) {
                        Deck generatedDeck = deck.getDeck();
                        if (generatedDeck == null) { return; }

                        generatedDeck = (Deck)generatedDeck.copyTo(""); //prevent deck having a name by default
                        editor = new FDeckEditor(editorType, generatedDeck, true);
                    }
                    else {
                        FOptionPane.showErrorDialog("You must select something before you can generate a new deck.");
                        return;
                    }
                    break;
                default:
                    editor = new FDeckEditor(editorType, "", false);
                    break;
                }
                editor.setSaveHandler(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        //ensure user returns to custom user deck and that list is refreshed if new deck is saved
                        if (!needRefreshOnActivate) {
                            needRefreshOnActivate = true;
                            setSelectedDeckType(DeckType.CUSTOM_DECK);
                        }
                    }
                });
                Forge.openScreen(editor);
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
                if (selectedDeckType != DeckType.COLOR_DECK && selectedDeckType != DeckType.THEME_DECK) {
                    FDeckViewer.show(getDeck());
                }
            }
        });
        switch (lstDecks.getGameType()) {
        case Constructed:
            break; //delay initialize for constructed until saved decks can be reloaded
        case Commander:
        case Gauntlet:
            initialize(null, DeckType.CUSTOM_DECK);
            break;
        default:
            initialize(null, DeckType.RANDOM_DECK);
            break;
        }
        lstDecks.setSelectionChangedHandler(selectionChangedHandler);
    }

    private void accept() {
        Forge.back();
        if (callback != null) {
            callback.run(getDeck());
        }
    }

    @Override
    public void onActivate() {
        if (needRefreshOnActivate) {
            needRefreshOnActivate = false;
            updateCustom();
            switch (lstDecks.getGameType()) {
            case Commander:
                lstDecks.setSelectedString(DeckPreferences.getCommanderDeck());
                break;
            case Archenemy:
                lstDecks.setSelectedString(DeckPreferences.getSchemeDeck());
                break;
            case Planechase:
                lstDecks.setSelectedString(DeckPreferences.getPlanarDeck());
                break;
            default:
                lstDecks.setSelectedString(DeckPreferences.getCurrentDeck());
                break;
            }
        }
    }

    private void editSelectedDeck() {
        final DeckProxy deck = lstDecks.getSelectedItem();
        if (deck == null) { return; }

        if (selectedDeckType == DeckType.CUSTOM_DECK) {
            editDeck(deck);
            return;
        }

        //set if deck with selected name exists already
        final IStorage<Deck> decks = FModel.getDecks().getConstructed();
        Deck existingDeck = decks.get(deck.getName());
        if (existingDeck != null) {
            setSelectedDeckType(DeckType.CUSTOM_DECK);
            editDeck(new DeckProxy(existingDeck, "Constructed", lstDecks.getGameType(), decks));
            return;
        }

        //prompt to duplicate deck if deck doesn't exist already
        FOptionPane.showConfirmDialog(selectedDeckType + " cannot be edited directly. Would you like to duplicate " + deck.getName() + " for editing as a custom user deck?",
                "Duplicate Deck?", "Duplicate", "Cancel", new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    Deck copiedDeck = (Deck)deck.getDeck().copyTo(deck.getName());
                    decks.add(copiedDeck);
                    setSelectedDeckType(DeckType.CUSTOM_DECK);
                    editDeck(new DeckProxy(copiedDeck, "Constructed", lstDecks.getGameType(), decks));
                }
            }
        });
    }

    private void editDeck(DeckProxy deck) {
        EditorType editorType;
        switch (lstDecks.getGameType()) {
        case Commander:
            editorType = EditorType.Commander;
            DeckPreferences.setCommanderDeck(deck.getName());
            break;
        case Archenemy:
            editorType = EditorType.Archenemy;
            DeckPreferences.setSchemeDeck(deck.getName());
            break;
        case Planechase:
            editorType = EditorType.Planechase;
            DeckPreferences.setPlanarDeck(deck.getName());
            break;
        default:
            editorType = EditorType.Constructed;
            DeckPreferences.setCurrentDeck(deck.getName());
            break;
        }
        needRefreshOnActivate = true;
        Forge.openScreen(new FDeckEditor(editorType, deck, true));
    }

    public void initialize(FPref savedStateSetting, DeckType defaultDeckType) {
        stateSetting = savedStateSetting;
        selectedDeckType = defaultDeckType;

        if (cmbDeckTypes == null) { //initialize components with delayed initialization the first time this is populated
            cmbDeckTypes = new FComboBox<DeckType>();
            switch (lstDecks.getGameType()) {
            case Constructed:
            case Gauntlet:
                cmbDeckTypes.addItem(DeckType.CUSTOM_DECK);
                cmbDeckTypes.addItem(DeckType.PRECONSTRUCTED_DECK);
                cmbDeckTypes.addItem(DeckType.QUEST_OPPONENT_DECK);
                cmbDeckTypes.addItem(DeckType.COLOR_DECK);
                cmbDeckTypes.addItem(DeckType.THEME_DECK);
                cmbDeckTypes.addItem(DeckType.RANDOM_DECK);
                break;
            default:
                cmbDeckTypes.addItem(DeckType.CUSTOM_DECK);
                cmbDeckTypes.addItem(DeckType.RANDOM_DECK);
                break;
            }
            cmbDeckTypes.setAlignment(HAlignment.CENTER);
            restoreSavedState();
            cmbDeckTypes.setChangedHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    refreshDecksList(cmbDeckTypes.getSelectedItem(), false, e);
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

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        width -= 2 * x;

        float fieldHeight = cmbDeckTypes.getHeight();
        float totalButtonHeight = 2 * fieldHeight + PADDING;

        cmbDeckTypes.setBounds(x, y, width, fieldHeight);
        y += cmbDeckTypes.getHeight() + 1;
        lstDecks.setBounds(x, y, width, height - y - totalButtonHeight - 2 * PADDING); //leave room for buttons at bottom

        y += lstDecks.getHeight() + PADDING;
        float buttonWidth = (width - PADDING) / 2;

        if (btnEditDeck.isVisible()) {
            btnNewDeck.setBounds(x, y, buttonWidth, fieldHeight);
        }
        else {
            btnNewDeck.setBounds(x, y, width, fieldHeight);
        }
        btnEditDeck.setBounds(x + buttonWidth + PADDING, y, buttonWidth, fieldHeight);
        y += fieldHeight + PADDING;

        btnViewDeck.setBounds(x, y, buttonWidth, fieldHeight);
        if (btnViewDeck.isVisible()) {
            btnRandom.setBounds(x + buttonWidth + PADDING, y, buttonWidth, fieldHeight);
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

    private void updateCustom() {
        lstDecks.setSelectionSupport(1, 1);

        switch (lstDecks.getGameType()) {
        case Commander:
            lstDecks.setPool(DeckProxy.getAllCommanderDecks());
            lstDecks.setup(ItemManagerConfig.COMMANDER_DECKS);
            break;
        case Archenemy:
            lstDecks.setPool(DeckProxy.getAllSchemeDecks());
            lstDecks.setup(ItemManagerConfig.SCHEME_DECKS);
            break;
        case Planechase:
            lstDecks.setPool(DeckProxy.getAllPlanarDecks());
            lstDecks.setup(ItemManagerConfig.PLANAR_DECKS);
            break;
        default:
            lstDecks.setPool(DeckProxy.getAllConstructedDecks());
            lstDecks.setup(ItemManagerConfig.CONSTRUCTED_DECKS);
            break;
        }

        btnNewDeck.setText("New Deck");
        btnNewDeck.setWidth(btnEditDeck.getWidth());
        btnEditDeck.setVisible(true);

        btnViewDeck.setVisible(true);
        btnRandom.setText("Random Deck");
        btnRandom.setWidth(btnNewDeck.getWidth());
        btnRandom.setLeft(getWidth() - PADDING - btnRandom.getWidth());
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                DeckgenUtil.randomSelect(lstDecks);
                accept();
            }
        });
    }

    private void updateColors() {
        lstDecks.setSelectionSupport(1, 3); //TODO: Consider supporting more than 3 color random decks

        String[] colors = new String[] { "Random 1", "Random 2", "Random 3",
                "White", "Blue", "Black", "Red", "Green" };
        ArrayList<DeckProxy> decks = new ArrayList<DeckProxy>();
        for (int i = 0; i < colors.length; i++) {
            decks.add(new ColorDeckGenerator(GuiBase.getInterface(), colors[i], i, lstDecks, isAi));
        }

        lstDecks.setPool(decks);
        lstDecks.setup(ItemManagerConfig.STRING_ONLY);

        btnNewDeck.setText("Generate New Deck");
        btnNewDeck.setWidth(getWidth() - 2 * PADDING);
        btnEditDeck.setVisible(false);

        btnViewDeck.setVisible(false);
        btnRandom.setText("Random Colors");
        btnRandom.setWidth(btnNewDeck.getWidth());
        btnRandom.setLeft(getWidth() - PADDING - btnRandom.getWidth());
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                DeckgenUtil.randomSelectColors(lstDecks);
                accept();
            }
        });
    }

    private void updateThemes() {
        lstDecks.setSelectionSupport(1, 1);

        lstDecks.setPool(DeckProxy.getAllThemeDecks());
        lstDecks.setup(ItemManagerConfig.STRING_ONLY);

        btnNewDeck.setText("Generate New Deck");
        btnNewDeck.setWidth(getWidth() - 2 * PADDING);
        btnEditDeck.setVisible(false);

        btnViewDeck.setVisible(false);
        btnRandom.setText("Random Theme");
        btnRandom.setWidth(btnNewDeck.getWidth());
        btnRandom.setLeft(getWidth() - PADDING - btnRandom.getWidth());
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                DeckgenUtil.randomSelect(lstDecks);
                accept();
            }
        });
    }

    private void updateRandom() {
        lstDecks.setSelectionSupport(1, 1);

        ArrayList<DeckProxy> decks = new ArrayList<DeckProxy>();
        decks.add(new RandomDeckGenerator("Random Generated Deck", 0, lstDecks, isAi));
        decks.add(new RandomDeckGenerator("Random User Deck", 1, lstDecks, isAi));

        lstDecks.setPool(decks);
        lstDecks.setup(ItemManagerConfig.STRING_ONLY);

        btnNewDeck.setText("Generate New Deck");
        btnNewDeck.setWidth(getWidth() - 2 * PADDING);
        btnEditDeck.setVisible(false);

        btnViewDeck.setVisible(false);
        btnRandom.setText("Random Deck");
        btnRandom.setWidth(btnNewDeck.getWidth());
        btnRandom.setLeft(getWidth() - PADDING - btnRandom.getWidth());
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                DeckgenUtil.randomSelect(lstDecks);
                accept();
            }
        });
    }

    private void updatePrecons() {
        lstDecks.setSelectionSupport(1, 1);

        lstDecks.setPool(DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons()));
        lstDecks.setup(ItemManagerConfig.PRECON_DECKS);

        btnNewDeck.setText("New Deck");
        btnNewDeck.setWidth(btnEditDeck.getWidth());
        btnEditDeck.setVisible(true);

        btnViewDeck.setVisible(true);
        btnRandom.setText("Random Deck");
        btnRandom.setWidth(btnNewDeck.getWidth());
        btnRandom.setLeft(getWidth() - PADDING - btnRandom.getWidth());
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                DeckgenUtil.randomSelect(lstDecks);
                accept();
            }
        });
    }

    private void updateQuestEvents() {
        lstDecks.setSelectionSupport(1, 1);

        lstDecks.setPool(DeckProxy.getAllQuestEventAndChallenges());
        lstDecks.setup(ItemManagerConfig.QUEST_EVENT_DECKS);

        btnNewDeck.setText("New Deck");
        btnNewDeck.setWidth(btnEditDeck.getWidth());
        btnEditDeck.setVisible(true);

        btnViewDeck.setVisible(true);
        btnRandom.setText("Random Deck");
        btnRandom.setWidth(btnNewDeck.getWidth());
        btnRandom.setLeft(getWidth() - PADDING - btnRandom.getWidth());
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                DeckgenUtil.randomSelect(lstDecks);
                accept();
            }
        });
    }

    public Deck getDeck() {
        DeckProxy proxy = lstDecks.getSelectedItem();
        if (proxy == null) { return null; }
        return proxy.getDeck();
    }

    /** Generates deck from current list selection(s). */
    public RegisteredPlayer getPlayer() {
        if (lstDecks.getSelectedIndex() < 0) { return null; }

        // Special branch for quest events
        if (selectedDeckType == DeckType.QUEST_OPPONENT_DECK) {
            QuestEvent event = DeckgenUtil.getQuestEvent(lstDecks.getSelectedItem().getName());
            RegisteredPlayer result = new RegisteredPlayer(event.getEventDeck());
            if (event instanceof QuestEventChallenge) {
                result.setStartingLife(((QuestEventChallenge) event).getAiLife());
            }
            result.setCardsOnBattlefield(QuestUtil.getComputerStartingCards(event));
            return result;
        }

        return new RegisteredPlayer(getDeck());
    }

    public final boolean isAi() {
        return isAi;
    }

    public void setIsAi(boolean isAiDeck) {
        isAi = isAiDeck;
    }

    private void refreshDecksList(DeckType deckType, boolean forceRefresh, FEvent e) {
        if (selectedDeckType == deckType && !forceRefresh) { return; }
        selectedDeckType = deckType;

        if (e == null) {
            cmbDeckTypes.setSelectedItem(deckType);
        }
        if (deckType == null) { return; }

        switch (deckType) {
        case CUSTOM_DECK:
            updateCustom();
            break;
        case COLOR_DECK:
            updateColors();
            break;
        case THEME_DECK:
            updateThemes();
            break;
        case QUEST_OPPONENT_DECK:
            updateQuestEvents();
            break;
        case PRECONSTRUCTED_DECK:
            updatePrecons();
            break;
        case RANDOM_DECK:
            updateRandom();
            break;
        }

        if (e != null) { //set default list selection if from combo box change event
            if (deckType == DeckType.COLOR_DECK) {
                // default selection = basic two color deck
                lstDecks.setSelectedIndices(new Integer[]{0, 1});
            }
            else {
                lstDecks.setSelectedIndex(0);
            }
        }
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
        String deckType = cmbDeckTypes.getSelectedItem().name();
        StringBuilder state = new StringBuilder(deckType);
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

    /** Returns a clean name from the state that can be used for labels. */
    public final String getStateForLabel() {
        String deckType = cmbDeckTypes.getSelectedItem().toString();
        StringBuilder state = new StringBuilder(deckType);
        state.append(": ");
        joinSelectedDecks(state, ", ");
        return state.toString();
    }

    private void restoreSavedState() {
        if (stateSetting == null) {
            //if can't restore saved state, just refresh deck list
            refreshDecksList(selectedDeckType, true, null);
            return;
        }

        String savedState = prefs.getPref(stateSetting);
        refreshDecksList(getDeckTypeFromSavedState(savedState), true, null);
        lstDecks.setSelectedStrings(getSelectedDecksFromSavedState(savedState));
    }

    private DeckType getDeckTypeFromSavedState(String savedState) {
        try {
            if (StringUtils.isBlank(savedState)) {
                return selectedDeckType;
            }
            else {
                return DeckType.valueOf(savedState.split(";")[0]);
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
                return new ArrayList<String>();
            }
            else {
                return Arrays.asList(savedState.split(";")[1].split(SELECTED_DECK_DELIMITER));
            }
        }
        catch (Exception ex) {
            System.err.println(ex + " [savedState=" + savedState + "]");
            return new ArrayList<String>();
        }
    }

    public FComboBox<DeckType> getDecksComboBox() {
        return cmbDeckTypes;
    }
}
