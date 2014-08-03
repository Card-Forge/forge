package forge.deckchooser;

import forge.UiCommand;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckType;
import forge.deck.DeckgenUtil;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.ItemManagerContainer;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestUtil;
import forge.toolbox.FLabel;
import forge.util.Aggregates;
import forge.util.storage.IStorage;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public class FDeckChooser extends JPanel implements IDecksComboBoxListener {
    private DecksComboBox decksComboBox;
    private DeckType selectedDeckType;
    private ItemManagerContainer lstDecksContainer;

    private final DeckManager lstDecks = new DeckManager(GameType.Constructed);
    private final FLabel btnViewDeck = new FLabel.ButtonBuilder().text("View Deck").fontSize(14).build();
    private final FLabel btnRandom = new FLabel.ButtonBuilder().fontSize(14).build();

    private boolean isAi;

    private final ForgePreferences prefs = FModel.getPreferences();
    private FPref stateSetting = null;

    public FDeckChooser(boolean forAi) {
        setOpaque(false);
        isAi = forAi;
        UiCommand cmdViewDeck = new UiCommand() {
            @Override
            public void run() {
                if (selectedDeckType != DeckType.COLOR_DECK && selectedDeckType != DeckType.THEME_DECK) {
                    FDeckViewer.show(getDeck());
                }
            }
        };
        lstDecks.setItemActivateCommand(cmdViewDeck);
        btnViewDeck.setCommand(cmdViewDeck);
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
    }

    public DeckType getSelectedDeckType() { return selectedDeckType; }
    public void setSelectedDeckType(DeckType selectedDeckType0) {
        refreshDecksList(selectedDeckType0, false, null);
    }

    public DeckManager getLstDecks() { return lstDecks; }

    private void updateCustom() {
        lstDecks.setAllowMultipleSelections(false);

        lstDecks.setPool(DeckProxy.getAllConstructedDecks());
        lstDecks.setup(ItemManagerConfig.CONSTRUCTED_DECKS);

        btnRandom.setText("Random Deck");
        btnRandom.setCommand(new UiCommand() {
            @Override
            public void run() {
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

    private class RandomDeckGenerator extends DeckProxy implements Comparable<RandomDeckGenerator> {
        private String name;
        private int index;

        public RandomDeckGenerator(String name0, int index0) {
            super();
            name = name0;
            index = index0;
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
        public int compareTo(final RandomDeckGenerator d) {
            return d instanceof RandomDeckGenerator ? Integer.compare(index, ((RandomDeckGenerator)d).index) : 1;
        }

        @Override
        public Deck getDeck() {
            String sel = lstDecks.getSelectedItem().getName();
            switch (lstDecks.getGameType()) {
            case Commander:
                if (sel.equals("Random User Deck")) {
                    IStorage<Deck> decks = FModel.getDecks().getCommander();
                    if (decks.size() > 0) {
                        return Aggregates.random(decks);
                    }
                }
                return DeckgenUtil.generateCommanderDeck(isAi);
            case Archenemy:
                if (sel.equals("Random User Deck")) {
                    IStorage<Deck> decks = FModel.getDecks().getScheme();
                    if (decks.size() > 0) {
                        return Aggregates.random(decks);
                    }
                }
                return DeckgenUtil.generateSchemeDeck();
            case Planechase:
                if (sel.equals("Random User Deck")) {
                    IStorage<Deck> decks = FModel.getDecks().getPlane();
                    if (decks.size() > 0) {
                        return Aggregates.random(decks);
                    }
                }
                return DeckgenUtil.generatePlanarDeck();
            default:
                if (sel.equals("Random User Deck")) {
                    IStorage<Deck> decks = FModel.getDecks().getConstructed();
                    if (decks.size() > 0) {
                        return Aggregates.random(decks);
                    }
                }
                while (true) {
                    switch (Aggregates.random(DeckType.values())) {
                    case PRECONSTRUCTED_DECK:
                        return Aggregates.random(DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons())).getDeck();
                    case QUEST_OPPONENT_DECK:
                        return Aggregates.random(DeckProxy.getAllQuestEventAndChallenges()).getDeck();
                    case COLOR_DECK:
                        List<String> colors = new ArrayList<String>();
                        int count = Aggregates.randomInt(1, 3);
                        for (int i = 1; i <= count; i++) {
                            colors.add("Random " + i);
                        }
                        return DeckgenUtil.buildColorDeck(colors, isAi);
                    case THEME_DECK:
                        return Aggregates.random(DeckProxy.getAllThemeDecks()).getDeck();
                    default:
                        continue;
                    }
                }
            }
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
        btnRandom.setCommand(new UiCommand() {
            @Override
            public void run() {
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
        btnRandom.setCommand(new UiCommand() {
            @Override
            public void run() {
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
        btnRandom.setCommand(new UiCommand() {
            @Override
            public void run() {
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
        btnRandom.setCommand(new UiCommand() {
            @Override
            public void run() {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        lstDecks.setSelectedIndex(0);
    }

    private void updateRandom() {
        lstDecks.setAllowMultipleSelections(false);

        ArrayList<DeckProxy> decks = new ArrayList<DeckProxy>();
        decks.add(new RandomDeckGenerator("Random Generated Deck", 0));
        decks.add(new RandomDeckGenerator("Random User Deck", 1));

        lstDecks.setPool(decks);
        lstDecks.setup(ItemManagerConfig.STRING_ONLY);

        btnViewDeck.setVisible(false);
        btnRandom.setText("Random Deck");
        btnRandom.setCommand(new UiCommand() {
            @Override
            public void run() {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });
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

    public void populate() {
        if (decksComboBox == null) { //initialize components with delayed initialization the first time this is populated
            decksComboBox = new DecksComboBox();
            lstDecksContainer = new ItemManagerContainer(lstDecks);
            restoreSavedState();
            decksComboBox.addListener(this);
        }
        else {
            removeAll();
            restoreSavedState(); //ensure decks refreshed and state restored in case any deleted or added since last loaded
        }
        this.setLayout(new MigLayout("insets 0, gap 0"));
        decksComboBox.addTo(this, "w 100%, h 30px!, gapbottom 5px, spanx 2, wrap");
        this.add(lstDecksContainer, "w 100%, growy, pushy, spanx 2, wrap");
        this.add(btnViewDeck, "w 50%-3px, h 30px!, gaptop 5px, gapright 6px");
        this.add(btnRandom, "w 50%-3px, h 30px!, gaptop 5px");
        if (isShowing()) {
            revalidate();
            repaint();
        }
    }

    public final boolean isAi() {
        return isAi;
    }

    public void setIsAi(boolean isAiDeck) {
        this.isAi = isAiDeck;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckchooser.IDecksComboBoxListener#deckTypeSelected(forge.gui.deckchooser.DecksComboBoxEvent)
     */
    @Override
    public void deckTypeSelected(DecksComboBoxEvent ev) {
        refreshDecksList(ev.getDeckType(), false, ev);
    }

    private void refreshDecksList(DeckType deckType, boolean forceRefresh, DecksComboBoxEvent ev) {
        if (selectedDeckType == deckType && !forceRefresh) { return; }
        selectedDeckType = deckType;

        if (ev == null) {
            decksComboBox.refresh(deckType);
        }
        lstDecks.setCaption(deckType.toString());

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
        String deckType = decksComboBox.getDeckType().name();
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
        String deckType = decksComboBox.getDeckType().toString();
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

    public DecksComboBox getDecksComboBox() {
        return decksComboBox;
    }
}
