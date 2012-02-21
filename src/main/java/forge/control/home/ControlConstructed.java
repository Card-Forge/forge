package forge.control.home;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import forge.AllZone;
import forge.CardList;
import forge.Constant;
import forge.PlayerType;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.generate.Generate2ColorDeck;
import forge.deck.generate.Generate3ColorDeck;
import forge.deck.generate.Generate5ColorDeck;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.GameNew;
import forge.game.GameType;
import forge.properties.ForgePreferences.FPref;
import forge.quest.data.QuestEvent;
import forge.util.IFolderMap;
import forge.view.home.ViewConstructed;

/** 
 * Controls logic and listeners for Constructed mode in home screen.
 *
 */
public class ControlConstructed {
    private final ViewConstructed view;
    private JPanel selectedTab;
    private final MouseListener madStartGame, madTabHuman, madTabAI, madSingletons, madArtifacts, madRemoveSmall;

    /**
     * 
     * Controls logic and listeners for "constructed" mode in home screen.
     * 
     * @param v0 &emsp; ViewConstructed
     */
    public ControlConstructed(final ViewConstructed v0) {
        this.view = v0;

        // Set action listeners
        madTabHuman = new MouseAdapter() { @Override
            public void mouseClicked(final MouseEvent e) { view.showHumanTab(); } };

        madTabAI = new MouseAdapter() { @Override
            public void mouseClicked(final MouseEvent e) { view.showAITab(); } };

        // Game start logic must happen outside of the EDT.
        madStartGame = new MouseAdapter() {
            @Override
            public void mouseReleased(final MouseEvent e) {
                final Thread t = new Thread() {
                    @Override
                    public void run() {
                        startGame();
                    }
                };
                t.start();
            }
        };

        madSingletons = new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                Singletons.getModel().getPreferences().setPref(
                        FPref.DECKGEN_SINGLETONS, String.valueOf(view.getCbSingletons().isSelected()));
            }
        };

        madArtifacts = new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                Singletons.getModel().getPreferences().setPref(
                        FPref.DECKGEN_ARTIFACTS, String.valueOf(view.getCbArtifacts().isSelected()));
            }
        };

        madRemoveSmall = new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                Singletons.getModel().getPreferences().setPref(
                        FPref.DECKGEN_NOSMALL, String.valueOf(view.getCbRemoveSmall().isSelected()));
            }
        };

        //Everything is defined; can now init.
        updateDeckLists();
        addListeners();

        view.getPnlHuman().getLstColorDecks().setSelectedIndices(new int[] {0, 1});
        view.getPnlHuman().listSelectManager(view.getPnlHuman().getLstColorDecks());

        view.getPnlAI().getLstColorDecks().setSelectedIndices(new int[] {0, 1});
        view.getPnlAI().listSelectManager(view.getPnlAI().getLstColorDecks());
    }

    /** @return ViewConstructed */
    public ViewConstructed getView() {
        return view;
    }

    /**
     * Updates visual state of tabber.
     * @param tab0 &emsp; JPanel tab object (can pass SubTab too).
     */
    public void updateTabber(final JPanel tab0) {
        if (selectedTab != null) {
            selectedTab.setEnabled(false);
        }

        tab0.setEnabled(true);
        selectedTab = tab0;
    }

    /** */
    public void updateDeckSelectionCheckboxes() {
        view.getCbSingletons().setSelected(
                Singletons.getModel().getPreferences().getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        view.getCbArtifacts().setSelected(
                Singletons.getModel().getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
        view.getCbRemoveSmall().setSelected(
                Singletons.getModel().getPreferences().getPrefBoolean(FPref.DECKGEN_NOSMALL));
    }

    /** */
    public void addListeners() {
        view.getBtnStart().removeMouseListener(madStartGame);
        view.getBtnStart().addMouseListener(madStartGame);

        view.getTabHuman().removeMouseListener(madTabHuman);
        view.getTabHuman().addMouseListener(madTabHuman);

        view.getTabAI().removeMouseListener(madTabAI);
        view.getTabAI().addMouseListener(madTabAI);

        view.getCbSingletons().removeMouseListener(madSingletons);
        view.getCbArtifacts().removeMouseListener(madArtifacts);
        view.getCbRemoveSmall().removeMouseListener(madRemoveSmall);

        view.getCbSingletons().addMouseListener(madSingletons);
        view.getCbArtifacts().addMouseListener(madArtifacts);
        view.getCbRemoveSmall().addMouseListener(madRemoveSmall);
    }

    /** */
    public void updateDeckLists() {
        view.getPnlHuman().getLstColorDecks().setListData(getColorNames());
        view.getPnlAI().getLstColorDecks().setListData(getColorNames());

        view.getPnlHuman().getLstThemeDecks().setListData(getThemeNames());
        view.getPnlAI().getLstThemeDecks().setListData(getThemeNames());

        view.getPnlHuman().getLstCustomDecks().setListData(getCustomNames());
        view.getPnlAI().getLstCustomDecks().setListData(getCustomNames());

        view.getPnlHuman().getLstQuestDecks().setListData(getEventNames());
        view.getPnlAI().getLstQuestDecks().setListData(getEventNames());
    }

    //========= DECK GENERATION

    /** Generates deck from current list selection(s). */
    private Deck generateDeck(final JList lst0, final PlayerType player0) {
        CardList cards = null;
        final String[] selection = oa2sa(lst0.getSelectedValues());
        final Deck deck;

        // Color deck
        if (lst0.getName().equals("lstColor") && colorCheck(selection)) {
            final Map<String, String> colorVals = new HashMap<String, String>();
            colorVals.put("Random 1", "AI");
            colorVals.put("Random 2", "AI");
            colorVals.put("Random 3", "AI");
            colorVals.put("Random 4", "AI");
            colorVals.put("Black", "black");
            colorVals.put("Blue", "blue");
            colorVals.put("Green", "green");
            colorVals.put("Red", "red");
            colorVals.put("White", "white");

            // Replace "random" with "AI" for deck generation code
            for (int i = 0; i < selection.length; i++) {
                selection[i] = colorVals.get(selection[i]);
            }

            // 2, 3, and 5 colors.
            if (selection.length == 2) {
                final Generate2ColorDeck gen = new Generate2ColorDeck(
                        selection[0], selection[1]);
                cards = gen.get2ColorDeck(60, player0);
            }
            else if (selection.length == 3) {
                final Generate3ColorDeck gen = new Generate3ColorDeck(
                        selection[0], selection[1], selection[2]);
                cards = gen.get3ColorDeck(60, player0);
            }
            else {
                final Generate5ColorDeck gen = new Generate5ColorDeck(
                        selection[0], selection[1], selection[2], selection[3], selection[4]);
                cards = gen.get5ColorDeck(60, player0);
            }

            // After generating card lists, build deck.
            deck = new Deck();
            deck.getMain().add(cards);
            colorVals.clear();
        }

        // Theme deck
        else if (lst0.getName().equals("lstTheme")) {
            final GenerateThemeDeck gen = new GenerateThemeDeck();
            cards = gen.getThemeDeck(selection[0], 60);

            // After generating card lists, build deck.
            deck = new Deck();
            deck.getMain().add(cards);
        }
        else if (lst0.getName().equals("lstQuest")) {
            deck = Singletons.getModel().getQuestEventManager().getEvent(selection[0]).getEventDeck();
        }
        // Custom deck
        else {
            deck = AllZone.getDecks().getConstructed().get(selection[0]);
        }

        return deck;
    }

    /** Fired when start button is pressed; checks various conditions from lists and starts game. */
    private void startGame() {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "ControlConstructed() > startGame() must be accessed from outside the event dispatch thread.");
        }

        // If everything is OK, show progress bar and start inits.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBarProgress().setMaximum(2);
                view.getBarProgress().reset();
                view.getBarProgress().setShowETA(false);
                view.getBarProgress().setShowCount(false);
                view.getBarProgress().setDescription("Starting New Game");
                view.getBarProgress().setVisible(true);
                view.getBtnStart().setVisible(false);
            }
        });

        SwingUtilities.invokeLater(new Runnable() { @Override
            public void run() { view.getBarProgress().increment(); } });

        Constant.Runtime.HUMAN_DECK[0] = generateDeck(view.getPnlHuman().getLstCurrentSelected(), PlayerType.HUMAN);

        SwingUtilities.invokeLater(new Runnable() { @Override
            public void run() { view.getBarProgress().increment(); } });

        Constant.Runtime.COMPUTER_DECK[0] = generateDeck(view.getPnlAI().getLstCurrentSelected(), PlayerType.COMPUTER);

        Constant.Runtime.setGameType(GameType.Constructed);

        if (Constant.Runtime.COMPUTER_DECK[0] == null || Constant.Runtime.HUMAN_DECK[0] == null) {
            SwingUtilities.invokeLater(new Runnable() { @Override
                public void run() {
                    view.getBarProgress().setVisible(false);
                    view.getBtnStart().setVisible(true);
                }
            });
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBtnStart().setVisible(true);
                view.getBarProgress().setVisible(false);

                GameNew.newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0],
                        new CardList(), new CardList(), 20, 20);
            }
        });
    }

    /**
     * 
     * Array of color selections present in list boxes. Values
     * correspond to colorVals hash map.
     * 
     * @return Object[]
     */
    // Four randoms are included which should cover all possibilities.
    private String[] getColorNames() {
        return new String[] {"Random 1", "Random 2", "Random 3",
                "Random 4", "Black", "Blue", "Green", "Red", "White"};
    }

    /**
     * Array of theme names, usually used in list boxes.
     * 
     * @return Object[]
     */
    private String[] getThemeNames() {
        final List<String> themeNames = new ArrayList<String>();
        themeNames.clear();

        for (final String s : GenerateThemeDeck.getThemeNames()) {
            themeNames.add(s);
        }

        return oa2sa(themeNames.toArray());
    }

    private String[] getCustomNames() {
        final List<String> customNames = new ArrayList<String>();
        customNames.clear();

        final IFolderMap<Deck> allDecks = AllZone.getDecks().getConstructed();
        for (final Deck d : allDecks) { customNames.add(d.getName()); }

        return oa2sa(customNames.toArray());
    }

    private String[] getEventNames() {
        // Probably a better place for this, but it's a time consuming method,
        // and must be completed before UI is built, and a better spot is hard to find.
        Singletons.getModel().getQuestEventManager().assembleAllEvents();

        final List<String> eventNames = new ArrayList<String>();
        eventNames.clear();

        for (final QuestEvent e : Singletons.getModel().getQuestEventManager().getAllChallenges()) {
            eventNames.add(e.getEventDeck().getName());
        }

        for (final QuestEvent e : Singletons.getModel().getQuestEventManager().getAllDuels()) {
            eventNames.add(e.getEventDeck().getName());
        }

        return oa2sa(eventNames.toArray());
    }

    /** 
     * Checks lengths of selected values for color lists
     * to see if a deck generator exists. Alert and visual reminder if fail.
     * 
     * @param colors0 &emsp; String[] of color names
     * @return boolean
     */
    private boolean colorCheck(final String[] colors0) {
        boolean result = true;

        if (colors0.length == 1) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, single color generated decks aren't supported yet."
                    + "\n\rPlease choose at least one more color for this deck.",
                    "Generate deck: 1 color", JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        else if (colors0.length == 4) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, four color generated decks aren't supported yet."
                    + "\n\rPlease use 2, 3, or 5 colors for this deck.",
                    "Generate deck: 4 colors", JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        else if (colors0.length > 5) {
            JOptionPane.showMessageDialog(null,
                    "Generate deck: maximum five colors!",
                    "Generate deck: too many colors", JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        return result;
    }

    /**
     * Exhaustively converts object array to string array.
     * Probably a much easier way to do this.
     * 
     * @param o0 &emsp; Object[]
     * @return String[]
     */
    private String[] oa2sa(final Object[] o0) {
        final String[] output = new String[o0.length];

        for (int i = 0; i < o0.length; i++) {
            output[i] = o0[i].toString();
        }

        return output;
    }
}
