package forge.deck;

import forge.Forge;
import forge.deck.Deck;
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
import forge.util.Utils;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FDeckChooser extends FScreen {
    private static final float PADDING = Utils.scaleMin(5);

    private FComboBox<DeckType> cmbDeckTypes;
    private DeckType selectedDeckType;

    private final DeckManager lstDecks = new DeckManager(GameType.Constructed);
    private final FButton btnViewDeck = new FButton("View Deck");
    private final FButton btnRandom = new FButton("Random Deck");

    private boolean isAi;

    private final ForgePreferences prefs = FModel.getPreferences();
    private FPref stateSetting = null;

    public FDeckChooser(boolean isAi0) {
        super("");
        isAi = isAi0;

        lstDecks.setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.back();
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
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (selectedDeckType != DeckType.COLOR_DECK && selectedDeckType != DeckType.THEME_DECK) {
                    FDeckViewer.show(getDeck());
                }
            }
        });
    }

    public void initialize() {
        initialize(DeckType.COLOR_DECK);
    }
    public void initialize(DeckType defaultDeckType) {
        initialize(null, defaultDeckType);
    }
    public void initialize(FPref savedStateSetting, DeckType defaultDeckType) {
        stateSetting = savedStateSetting;
        selectedDeckType = defaultDeckType;

        if (cmbDeckTypes == null) { //initialize components with delayed initialization the first time this is populated
            cmbDeckTypes = new FComboBox<DeckType>(DeckType.values());
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

        cmbDeckTypes.setBounds(x, y, width, fieldHeight);
        y += cmbDeckTypes.getHeight() + 1;
        lstDecks.setBounds(x, y, width, height - y - fieldHeight - 2 * PADDING); //leave room for buttons at bottom

        y += lstDecks.getHeight() + PADDING;
        float buttonWidth = (width - PADDING) / 2;
        btnViewDeck.setBounds(x, y, buttonWidth, fieldHeight);
        x += buttonWidth + PADDING;
        btnRandom.setBounds(x, y, buttonWidth, fieldHeight);
    }

    public DeckType getSelectedDeckType() { return selectedDeckType; }
    public void setSelectedDeckType(DeckType selectedDeckType0) {
        refreshDecksList(selectedDeckType0, false, null);
    }

    public DeckManager getLstDecks() { return lstDecks; }

    private void updateCustom() {
        lstDecks.setAllowMultipleSelections(false);

        lstDecks.setPool(DeckProxy.getAllConstructedDecks(FModel.getDecks().getConstructed()));
        lstDecks.setup(ItemManagerConfig.CONSTRUCTED_DECKS);

        btnRandom.setText("Random Deck");
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        lstDecks.setSelectedIndex(0);
    }

    private class ColorDeckGenerator extends DeckProxy implements Comparable<ColorDeckGenerator> {
        private String name;
        private int index;

        public ColorDeckGenerator(String name0, int index0) {
            super();
            name = name0;
            this.index = index0;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }


        @Override
        public int compareTo(final ColorDeckGenerator d) {
            return d instanceof ColorDeckGenerator ? Integer.compare(this.index, ((ColorDeckGenerator)d).index) : 1;
        }

        @Override
        public Deck getDeck() {
            List<String> selection = new ArrayList<String>();
            for (DeckProxy deck : lstDecks.getSelectedItems()) {
                selection.add(deck.getName());
            }
            if (DeckgenUtil.colorCheck(selection)) {
                return DeckgenUtil.buildColorDeck(selection, isAi);
            }
            return null;
        }
        
        @Override
        public boolean isGeneratedDeck() {
            return true;
        }
    }

    private void updateColors() {
        lstDecks.setAllowMultipleSelections(true);

        String[] colors = new String[] { "Random 1", "Random 2", "Random 3",
                "White", "Blue", "Black", "Red", "Green" };
        ArrayList<DeckProxy> decks = new ArrayList<DeckProxy>();
        for (int i = 0; i < colors.length; i++) {
            decks.add(new ColorDeckGenerator(colors[i], i));
        }

        lstDecks.setPool(decks);
        lstDecks.setup(ItemManagerConfig.STRING_ONLY);

        btnRandom.setText("Random Colors");
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                DeckgenUtil.randomSelectColors(lstDecks);
            }
        });

        // default selection = basic two color deck
        lstDecks.setSelectedIndices(new Integer[]{0, 1});
    }

    private void updateThemes() {
        lstDecks.setAllowMultipleSelections(false);

        lstDecks.setPool(DeckProxy.getAllThemeDecks());
        lstDecks.setup(ItemManagerConfig.STRING_ONLY);

        btnRandom.setText("Random Deck");
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        lstDecks.setSelectedIndex(0);
    }

    private void updatePrecons() {
        lstDecks.setAllowMultipleSelections(false);

        lstDecks.setPool(DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons()));
        lstDecks.setup(ItemManagerConfig.PRECON_DECKS);

        btnRandom.setText("Random Deck");
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        lstDecks.setSelectedIndex(0);
    }

    private void updateQuestEvents() {
        lstDecks.setAllowMultipleSelections(false);

        lstDecks.setPool(DeckProxy.getAllQuestEventAndChallenges());
        lstDecks.setup(ItemManagerConfig.QUEST_EVENT_DECKS);

        btnRandom.setText("Random Deck");
        btnRandom.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        lstDecks.setSelectedIndex(0);
    }

    public Deck getDeck() {
        DeckProxy proxy = lstDecks.getSelectedItem();
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
        this.isAi = isAiDeck;
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
