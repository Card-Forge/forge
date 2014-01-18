package forge.gui.deckchooser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;
import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gui.MouseUtil;
import forge.gui.MouseUtil.MouseCursor;
import forge.gui.deckchooser.DecksComboBox.DeckType;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.itemmanager.DeckManager;
import forge.gui.toolbox.itemmanager.ItemManagerContainer;
import forge.item.PreconDeck;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestUtil;

@SuppressWarnings("serial")
public class FDeckChooser extends JPanel implements IDecksComboBoxListener {
    private boolean isUISetup = false;

    private DecksComboBox decksComboBox;
    private DeckType selectedDeckType = DeckType.COLOR_DECK;

    private final DeckManager lstDecks = new DeckManager(GameType.Constructed);
    private final FLabel btnRandom = new FLabel.ButtonBuilder().text("Random").fontSize(14).build();

    private boolean isAi;

    private final ForgePreferences prefs = Singletons.getModel().getPreferences();
    private FPref stateSetting = null;

    public FDeckChooser(boolean forAi) {
        setOpaque(false);
        isAi = forAi;
        lstDecks.setItemActivateCommand(new Command() {
            @Override
            public void run() {
                if (selectedDeckType != DeckType.COLOR_DECK && selectedDeckType != DeckType.THEME_DECK) {
                    DeckgenUtil.showDecklist(getDeck());
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
    }

    public DeckType getSelectedDeckType() { return selectedDeckType; }
    public void setSelectedDeckType(DeckType selectedDeckType0) {
        if (selectedDeckType != selectedDeckType0) {
            selectedDeckType = selectedDeckType0;
            decksComboBox.refresh(selectedDeckType0);
        }
    }

    public DeckManager getLstDecks() { return lstDecks; }
    private FLabel getBtnRandom() { return btnRandom; }

    private void updateCustom() {
        lstDecks.setAllowMultipleSelections(false);

        lstDecks.setPool(Singletons.getModel().getDecks().getConstructed());
        lstDecks.update();

        getBtnRandom().setText("Random Deck");
        getBtnRandom().setCommand(new Command() {
            @Override
            public void run() {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        lstDecks.setSelectedIndex(0);
    }

    //Deck that generates its card pool via some algorithm
    private abstract class DeckGenerator extends Deck {
        public DeckGenerator(final String name0) {
            super(name0);
        }

        @Override
        protected DeckBase newInstance(final String name0) {
            final DeckGenerator deckGen = this;
            return new DeckGenerator(name0) {
                @Override
                public Deck generateDeck() {
                    return deckGen.generateDeck();
                }
            };
        }

        public abstract Deck generateDeck();
    }

    private class ColorDeckGenerator extends DeckGenerator {
        public ColorDeckGenerator(String name0) {
            super(name0);
        }

        @Override
        public Deck generateDeck() {
            List<String> selection = new ArrayList<String>();
            for (Deck deck : lstDecks.getSelectedItems()) {
                selection.add(deck.getName());
            }
            if (DeckgenUtil.colorCheck(selection)) {
                return DeckgenUtil.buildColorDeck(selection, isAi);
            }
            return null;
        }
    }

    private void updateColors() {
        lstDecks.setAllowMultipleSelections(true);

        ArrayList<Deck> decks = new ArrayList<Deck>();
        decks.add(new ColorDeckGenerator("Random 1"));
        decks.add(new ColorDeckGenerator("Random 2"));
        decks.add(new ColorDeckGenerator("Random 3"));
        decks.add(new ColorDeckGenerator("White"));
        decks.add(new ColorDeckGenerator("Blue"));
        decks.add(new ColorDeckGenerator("Black"));
        decks.add(new ColorDeckGenerator("Red"));
        decks.add(new ColorDeckGenerator("Green"));

        lstDecks.setPool(decks);
        lstDecks.update(true);

        getBtnRandom().setText("Random Colors");
        getBtnRandom().setCommand(new Command() {
            @Override
            public void run() {
                DeckgenUtil.randomSelectColors(lstDecks);
            }
        });

        // default selection = basic two color deck
        lstDecks.setSelectedIndices(new Integer[]{0, 1});
    }

    private class ThemeDeckGenerator extends DeckGenerator {
        public ThemeDeckGenerator(String name0) {
            super(name0);
        }

        @Override
        public Deck generateDeck() {
            return DeckgenUtil.buildThemeDeck(this.getName());
        }
    }

    private void updateThemes() {
        lstDecks.setAllowMultipleSelections(false);

        ArrayList<Deck> decks = new ArrayList<Deck>();
        for (final String s : GenerateThemeDeck.getThemeNames()) {
            decks.add(new ThemeDeckGenerator(s));
        }

        lstDecks.setPool(decks);
        lstDecks.update(true);

        getBtnRandom().setText("Random Deck");
        getBtnRandom().setCommand(new Command() {
            @Override
            public void run() {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        lstDecks.setSelectedIndex(0);
    }

    private void updatePrecons() {
        lstDecks.setAllowMultipleSelections(false);

        ArrayList<Deck> decks = new ArrayList<Deck>();
        for (final PreconDeck preconDeck : QuestController.getPrecons()) {
            decks.add(preconDeck.getDeck());
        }

        lstDecks.setPool(decks);
        lstDecks.update(false, true);

        getBtnRandom().setText("Random Deck");
        getBtnRandom().setCommand(new Command() {
            @Override
            public void run() {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        lstDecks.setSelectedIndex(0);
    }

    private void updateQuestEvents() {
        lstDecks.setAllowMultipleSelections(false);

        ArrayList<Deck> decks = new ArrayList<Deck>();
        QuestController quest = Singletons.getModel().getQuest();
        for (QuestEvent e : quest.getDuelsManager().getAllDuels()) {
            decks.add(e.getEventDeck());
        }
        for (QuestEvent e : quest.getChallenges()) {
            decks.add(e.getEventDeck());
        }

        lstDecks.setPool(decks);
        lstDecks.update(false, true);

        getBtnRandom().setText("Random Deck");
        getBtnRandom().setCommand(new Command() {
            @Override
            public void run() {
                DeckgenUtil.randomSelect(lstDecks);
            }
        });

        lstDecks.setSelectedIndex(0);
    }

    public Deck getDeck() {
        Deck deck = lstDecks.getSelectedItem();
        if (deck instanceof DeckGenerator) {
            return ((DeckGenerator)deck).generateDeck();
        }
        return deck;
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

    public void populate() {
        setupUI();
        removeAll();
        this.setLayout(new MigLayout("insets 0, gap 0, flowy"));
        this.add(decksComboBox.getComponent(), "w 10:100%, h 30px!, gapbottom 5px");
        this.add(new ItemManagerContainer(lstDecks), "w 10:100%, growy, pushy");
        this.add(btnRandom, "w 10:100%, h 30px!, gaptop 5px");
        if (isShowing()) {
            validate();
            repaint();
        }
    }

    public void setIsAiDeck(boolean isAiDeck) {
        this.isAi = isAiDeck;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckchooser.IDecksComboBoxListener#deckTypeSelected(forge.gui.deckchooser.DecksComboBoxEvent)
     */
    @Override
    public void deckTypeSelected(DecksComboBoxEvent ev) {
        MouseUtil.setMouseCursor(MouseCursor.WAIT_CURSOR);
        refreshDecksList(ev.getDeckType());
        MouseUtil.setMouseCursor(MouseCursor.DEFAULT_CURSOR);
    }

    /**
     * Creates the various UI components that make up the Deck Chooser.
     * <p>
     * Only needs to be called once from populate() method so that
     * components are "lazy-loaded" when Deck Chooser is first displayed.
     */
    private void setupUI() {
        if (!isUISetup) {
            // Only do this once.
            isUISetup = true;
            // core UI components.
            decksComboBox = new DecksComboBox();
            // monitor events generated by these components.
            decksComboBox.addListener(this);
            // now everything is in place, fire initial populate event.
            restoreSavedState();
            decksComboBox.refresh(selectedDeckType);
        }
    }

    private void refreshDecksList(DeckType deckType) {
        selectedDeckType = deckType;
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
        String state = deckType;
        state += ";";
        Iterable<Deck> selectedDecks = lstDecks.getSelectedItems();
        if (selectedDecks != null) {
            for (Deck deck : selectedDecks) {
                state += deck + SELECTED_DECK_DELIMITER;
            }
            state = state.substring(0, state.length() - SELECTED_DECK_DELIMITER.length());
        }
        return state;
    }

    /** Returns a clean name from the state that can be used for labels. */
    public final String getStateForLabel() {
        String deckType = decksComboBox.getDeckType().toString();
        String state = deckType;
        state += ": ";
        Iterable<Deck> selectedDecks = lstDecks.getSelectedItems();
        if (selectedDecks != null) {
            for (Deck deck : selectedDecks) {
                state += deck + ", ";
            }
            state = state.substring(0, state.length() - 2);
        }
        return state;
    }

    private void restoreSavedState() {
        if (stateSetting == null) {
            //if can't restore saved state, just refresh decks combo box
            decksComboBox.refresh(selectedDeckType);
            return;
        }

        String savedState = prefs.getPref(stateSetting);
        selectedDeckType = getDeckTypeFromSavedState(savedState);
        decksComboBox.refresh(selectedDeckType);
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
}
