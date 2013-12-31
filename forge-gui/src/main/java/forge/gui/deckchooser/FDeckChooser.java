package forge.gui.deckchooser;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.game.player.RegisteredPlayer;
import forge.gui.MouseUtil;
import forge.gui.MouseUtil.MouseCursor;
import forge.gui.deckchooser.DecksComboBox.DeckType;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestUtil;
import forge.util.IHasName;
import forge.util.storage.IStorage;

@SuppressWarnings("serial")
public class FDeckChooser extends JPanel implements IDecksComboBoxListener {

    private final Color BORDER_COLOR = FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor().darker();

    private boolean isUISetup = false;

    private DecksComboBox decksComboBox;
    private DeckType selectedDeckType = DeckType.COLOR_DECK;
    private List<String> selectedDecks = new ArrayList<String>();

    private final JList<String> lstDecks  = new FList<String>();
    private final FLabel btnRandom = new FLabel.ButtonBuilder().text("Random").fontSize(16).build();

    private final JScrollPane scrDecks =
            new FScrollPane(lstDecks, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private final FLabel lblDecklist = new FLabel.Builder().text("Double click deck for its decklist.").fontSize(12).build();

    private boolean isAi;

    private final ForgePreferences prefs = Singletons.getModel().getPreferences();
    private FPref stateSetting = null;

    private final MouseAdapter madDecklist = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (MouseEvent.BUTTON1 == e.getButton() && e.getClickCount() == 2) {
                if (selectedDeckType != DeckType.COLOR_DECK && selectedDeckType != DeckType.THEME_DECK) {
                    DeckgenUtil.showDecklist(getDeck());
                }
            }
        }
    };

    public FDeckChooser(final String titleText, boolean forAi, boolean canSwitchType) {
        setOpaque(false);
        isAi = forAi;
    }

    public FDeckChooser(String titleText, boolean forAi) {
        this(titleText, forAi, false);
    }

    public void initialize(FPref savedStateSetting, DeckType defaultDeckType) {
        stateSetting = savedStateSetting;
        selectedDeckType = defaultDeckType;
    }
    public void initialize(DeckType defaultDeckType) {
        initialize(null, defaultDeckType);
    }
    public void initialize() {
        initialize(DeckType.COLOR_DECK);
    }

    private JList<String> getLstDecks()  { return lstDecks;  }
    private FLabel       getBtnRandom() { return btnRandom; }

    private void updateColors() {
        final JList<String> lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        final String[] listData = new String[] {"Random 1", "Random 2", "Random 3", "Black", "Blue", "Green", "Red", "White"};
        lst.setListData(listData);
        lst.setName(DeckgenUtil.DeckTypes.COLORS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        getBtnRandom().setText("Random colors");
        getBtnRandom().setCommand(new Command() {
            @Override public void run() { lst.setSelectedIndices(DeckgenUtil.randomSelectColors(8)); } });

        if (listData.length > 0) {
            // default selection = basic two color deck
            lst.setSelectedIndices(getSelectedDeckIndices(Arrays.asList(listData), new int[]{0, 1}));
            lst.ensureIndexIsVisible(lst.getSelectedIndices()[0]);
        }

    }

    private void updateThemes() {
        final JList<String> lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final List<String> listData = new ArrayList<String>();
        for (final String s : GenerateThemeDeck.getThemeNames()) { listData.add(s); }

        lst.setListData(listData.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(DeckgenUtil.DeckTypes.THEMES.toString());
        lst.removeMouseListener(madDecklist);

        getBtnRandom().setText("Random deck");
        getBtnRandom().setCommand(new Command() {
            @Override public void run() { DeckgenUtil.randomSelect(lst); } });

        if (listData.size() > 0) {
            lst.setSelectedIndices(getSelectedDeckIndices(listData, new int[]{0}));
            lst.ensureIndexIsVisible(lst.getSelectedIndices()[0]);
        }

    }

    private void updateCustom() {
        final JList<String> lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> listData = new ArrayList<String>();
        addDecksRecursive(Singletons.getModel().getDecks().getConstructed(), listData, null);

        lst.setListData(listData.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(DeckgenUtil.DeckTypes.CUSTOM.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        getBtnRandom().setText("Random deck");
        getBtnRandom().setCommand(new Command() {
            @Override public void run() { DeckgenUtil.randomSelect(lst); } });

        if (listData.size() > 0) {
            lst.setSelectedIndices(getSelectedDeckIndices(listData, new int[]{0}));
            lst.ensureIndexIsVisible(lst.getSelectedIndices()[0]);
        }
    }

    private <T extends IHasName> void addDecksRecursive(IStorage<T> node, List<String> customNames, String namePrefix ) {
        String path = namePrefix == null ? "" : namePrefix + " / ";
        for (final String fn : node.getFolders().getItemNames() )
        {
            IStorage<T> f = node.getFolders().get(fn);
            addDecksRecursive(f, customNames, path + fn);
        }
        for (final T d : node) { customNames.add(path + d.getName()); }
    }

    private void updatePrecons() {
        final JList<String> lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> listData = new ArrayList<String>();
        addDecksRecursive(QuestController.getPrecons(), listData, null);

        lst.setListData(listData.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(DeckgenUtil.DeckTypes.PRECON.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        getBtnRandom().setText("Random deck");
        getBtnRandom().setCommand(new Command() {
            @Override public void run() { DeckgenUtil.randomSelect(lst); } });

        if (listData.size() > 0) {
            lst.setSelectedIndices(getSelectedDeckIndices(listData, new int[]{0}));
            lst.ensureIndexIsVisible(lst.getSelectedIndices()[0]);
        }
    }

    private void updateQuestEvents() {
        final JList<String> lst = getLstDecks();
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> listData = new ArrayList<String>();

        QuestController quest = Singletons.getModel().getQuest();
        for (QuestEvent e : quest.getDuelsManager().getAllDuels()) {
            listData.add(e.getName());
        }

        for (QuestEvent e : quest.getChallenges()) {
            listData.add(e.getTitle());
        }

        lst.setListData(listData.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(DeckgenUtil.DeckTypes.QUESTEVENTS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        getBtnRandom().setText("Random event");
        getBtnRandom().setCommand(new Command() {
            @Override public void run() { DeckgenUtil.randomSelect(lst); } });

        if (listData.size() > 0) {
            lst.setSelectedIndices(getSelectedDeckIndices(listData, new int[]{0}));
            lst.ensureIndexIsVisible(lst.getSelectedIndices()[0]);
        }

    }

    public Deck getDeck() {
        JList<String> lst0 = getLstDecks();
        final List<String> selection = lst0.getSelectedValuesList();

        if (selection.isEmpty()) { return null; }

        // Special branch for quest events
        if (lst0.getName().equals(DeckgenUtil.DeckTypes.QUESTEVENTS.toString()))
            return DeckgenUtil.getQuestEvent(selection.get(0)).getEventDeck();
        if (lst0.getName().equals(DeckgenUtil.DeckTypes.COLORS.toString()) && DeckgenUtil.colorCheck(selection))
            return DeckgenUtil.buildColorDeck(selection, isAi);
        if (lst0.getName().equals(DeckgenUtil.DeckTypes.THEMES.toString()))
            return DeckgenUtil.buildThemeDeck(selection.get(0));
        if (lst0.getName().equals(DeckgenUtil.DeckTypes.CUSTOM.toString()))
            return DeckgenUtil.getConstructedDeck(selection.get(0));
        if (lst0.getName().equals(DeckgenUtil.DeckTypes.PRECON.toString()))
            return DeckgenUtil.getPreconDeck(selection.get(0));

        return null;
    }

    /** Generates deck from current list selection(s). */
    public RegisteredPlayer getPlayer() {
        if (getLstDecks().getSelectedValuesList().isEmpty()) { return null; }

        // Special branch for quest events
        if (getLstDecks().getName().equals(DeckgenUtil.DeckTypes.QUESTEVENTS.toString())) {
            QuestEvent event = DeckgenUtil.getQuestEvent(getLstDecks().getSelectedValuesList().get(0));
            RegisteredPlayer result = new RegisteredPlayer(event.getEventDeck());
            if( event instanceof QuestEventChallenge ) {
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
        this.add(decksComboBox.getComponent(), "w 10:100%, h 30px!");
        this.add(scrDecks, "w 10:100%, growy, pushy");
        this.add(btnRandom, "w 10:100%, h 26px!, gap 0 0 2px 0");
        this.add(lblDecklist, "w 10:100%, h 20px!, , gap 0 0 5px 0");
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
            // set component styles.
            lstDecks.setOpaque(false);
            scrDecks.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, BORDER_COLOR));
            // monitor events generated by these components.
            decksComboBox.addListener(this);
            // now everything is in place, fire initial populate event.
            restoreSavedState();
            decksComboBox.refresh(selectedDeckType);
        }
    }

    private void refreshDecksList(DeckType deckType) {
        switch (deckType) {
        case CUSTOM_DECK:
            updateCustom();
            lblDecklist.setVisible(true);
            break;
        case COLOR_DECK:
            updateColors();
            lblDecklist.setVisible(false);
            break;
        case THEME_DECK:
            updateThemes();
            lblDecklist.setVisible(false);
            break;
        case QUEST_OPPONENT_DECK:
            updateQuestEvents();
            lblDecklist.setVisible(true);
            break;
        case PRECONSTRUCTED_DECK:
            updatePrecons();
            lblDecklist.setVisible(true);
            break;
        }
        selectedDeckType = deckType;
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
        final JList<String> lst = getLstDecks();
        state += ";";
        for (String value : lst.getSelectedValuesList()) {
            state += value + SELECTED_DECK_DELIMITER;
        }
        state = state.substring(0, state.length()-SELECTED_DECK_DELIMITER.length());
        return state;
    }

    private void restoreSavedState() {
        if (stateSetting != null) {
            String savedState = prefs.getPref(stateSetting);
            selectedDeckType = getDeckTypeFromSavedState(savedState);
            selectedDecks = getSelectedDecksFromSavedState(savedState);
        }
    }

    private DeckType getDeckTypeFromSavedState(String savedState) {
        try {
            if (StringUtils.isBlank(savedState)) {
                return selectedDeckType;
            } else {
                return DeckType.valueOf(savedState.split(";")[0]);
            }
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage() + ". Using default : " + selectedDeckType);
            return selectedDeckType;
        }
    }

    private List<String> getSelectedDecksFromSavedState(String savedState) {
        try {
            if (StringUtils.isBlank(savedState)) {
                return new ArrayList<String>();
            } else {
                return Arrays.asList(savedState.split(";")[1].split(SELECTED_DECK_DELIMITER));
            }
        } catch (Exception ex) {
            System.err.println(ex + " [savedState=" + savedState + "]");
            return new ArrayList<String>();
        }
    }

    private int[] getSelectedDeckIndices(List<String> listData, int[] defaultSelection) {
        if (selectedDecks != null && selectedDecks.size() > 0) {
            List<Integer> selectedIndices = new ArrayList<Integer>();
            for (String deck : selectedDecks) {
                int index = listData.indexOf(deck);
                if (index >= 0) {
                    selectedIndices.add(index);
                }
            }
            int[] indices = ArrayUtils.toPrimitive(selectedIndices.toArray(new Integer[selectedIndices.size()]));
            if (indices.length == 0) { indices = defaultSelection; }
            // only do this once at startup.
            selectedDecks = null;
            return indices;
        } else {
            return defaultSelection;
        }
    }

}
