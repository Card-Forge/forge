package forge.control.home;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.AllZone;
import forge.CardList;
import forge.Command;
import forge.Constant;
import forge.PlayerType;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.deck.generate.Generate2ColorDeck;
import forge.deck.generate.Generate3ColorDeck;
import forge.deck.generate.Generate5ColorDeck;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.GameType;
import forge.item.CardPrinted;
import forge.view.home.ViewConstructed;

/** 
 * Controls logic and listeners for Constructed mode in home screen.
 *
 */
public class ControlConstructed {
    private ViewConstructed view;

    private JList currentHumanSelection = null;
    private JList currentAISelection = null;

    private final Map<String, String> colorVals;
    private List<String> themeNames;
    private List<String> deckNames;
    private final MouseListener madStartGame, madDecksAI, madDecksHuman,
        madHumanRandomDeck, madAIRandomDeck, madHumanRandomTheme, madAIRandomTheme;

    /**
     * 
     * Controls logic and listeners for "constructed" mode in home screen.
     * 
     * @param v0 &emsp; ViewConstructed
     */
    public ControlConstructed(ViewConstructed v0) {
        this.view = v0;

        // Reference values for colors, needed for deck generation classes.
        colorVals = new HashMap<String, String>();
        colorVals.put("Random 1", "AI");
        colorVals.put("Random 2", "AI");
        colorVals.put("Random 3", "AI");
        colorVals.put("Random 4", "AI");
        colorVals.put("Black", "black");
        colorVals.put("Blue", "blue");
        colorVals.put("Green", "green");
        colorVals.put("Red", "red");
        colorVals.put("White", "white");

        // Update list data
        view.getLstColorsHuman().setListData(getColorNames());
        view.getLstThemesHuman().setListData(oa2sa(getThemeNames()));
        view.getLstColorsAI().setListData(getColorNames());
        view.getLstThemesAI().setListData(oa2sa(getThemeNames()));

        // Set action listeners
        madHumanRandomTheme = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                randomPick(view.getLstThemesHuman());
            }
        };

        madAIRandomTheme = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                randomPick(view.getLstThemesAI());
            }
        };

        madHumanRandomDeck = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                randomPick(view.getLstDecksHuman());
            }
        };

        madAIRandomDeck = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                randomPick(view.getLstDecksAI());
            }
        };

        // Game start logic must happen outside of the EDT.
        madStartGame = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                final Thread t = new Thread() {
                    @Override
                    public void run() {
                        startGame();
                    }
                };
                t.start();
            }
        };

        madDecksAI = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = view.getLstDecksAI().locationToIndex(e.getPoint());
                    showDecklist(AllZone.getDecks().getConstructed().get(deckNames.get(index)));
                 }
            }
        };

        madDecksHuman = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = view.getLstDecksHuman().locationToIndex(e.getPoint());
                    if (index > 0) {
                        showDecklist(AllZone.getDecks().getConstructed().get(deckNames.get(index)));
                    }
                 }
            }
        };
    }

    /** @return ViewConstructed */
    public ViewConstructed getView() {
        return view;
    }

    /** */
    public void addListeners() {
        // Selection regulation
        view.getLstColorsHuman().getSelectionModel().addListSelectionListener(new HumanColorsListener());
        view.getLstColorsAI().getSelectionModel().addListSelectionListener(new AIColorsListener());
        view.getLstColorsHuman().setSelectedIndices(new int[] {0, 1});
        view.getLstColorsAI().setSelectedIndices(new int[] {0, 1});

        view.getLstThemesAI().getSelectionModel().addListSelectionListener(new AIThemesListener());
        view.getLstThemesHuman().getSelectionModel().addListSelectionListener(new HumanThemesListener());

        view.getLstDecksHuman().getSelectionModel().addListSelectionListener(new HumanDecksListener());
        view.getLstDecksAI().getSelectionModel().addListSelectionListener(new AIDecksListener());

        view.getLstDecksHuman().removeMouseListener(madDecksHuman);
        view.getLstDecksHuman().addMouseListener(madDecksHuman);

        view.getLstDecksAI().removeMouseListener(madDecksAI);
        view.getLstDecksAI().addMouseListener(madDecksAI);

        view.getBtnStart().removeMouseListener(madStartGame);
        view.getBtnStart().addMouseListener(madStartGame);

        view.getBtnAIRandomDeck().removeMouseListener(madAIRandomDeck);
        view.getBtnAIRandomDeck().addMouseListener(madAIRandomDeck);

        view.getBtnHumanRandomDeck().removeMouseListener(madHumanRandomDeck);
        view.getBtnHumanRandomDeck().addMouseListener(madHumanRandomDeck);

        view.getBtnAIRandomTheme().removeMouseListener(madAIRandomTheme);
        view.getBtnAIRandomTheme().addMouseListener(madAIRandomTheme);

        view.getBtnHumanRandomTheme().removeMouseListener(madHumanRandomTheme);
        view.getBtnHumanRandomTheme().addMouseListener(madHumanRandomTheme);
    }

    //========== LISTENERS

    /** Listeners for human lists, which pass control directly to regulation logic. */
    public class HumanColorsListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) { regulateHuman(view.getLstColorsHuman()); }
    }

    private class HumanThemesListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) { regulateHuman(view.getLstThemesHuman()); }
    }

    private class HumanDecksListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) { regulateHuman(view.getLstDecksHuman()); }
    }

    /** Listeners for AI lists, which pass control directly to regulation logic. */
    private class AIColorsListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) { regulateAI(view.getLstColorsAI()); }
    }

    private class AIThemesListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) { regulateAI(view.getLstThemesAI()); }
    }

    private class AIDecksListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) { regulateAI(view.getLstDecksAI()); }
    }

    //========= REGULATION AND VALIDATION

    /** 
     * Checks lengths of selected values for human color lists
     * to see if a deck generator exists. Alert and visual reminder if fail.
     * 
     * @param human0 &emsp; String[] of color names
     * @return boolean
     */
    public boolean checkValidityOfHumanSelectedColors(String[] human0) {
        boolean result = true;

        if (human0.length == 1) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, single color generated decks aren't supported yet."
                    + "\n\rPlease choose at least one more color for the human deck.",
                    "Human deck: 1 color", JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        else if (human0.length == 4) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, four color generated decks aren't supported yet."
                    + "\n\rPlease use 2, 3, or 5 colors for the human deck.",
                    "Human deck: 4 colors", JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        else if (human0.length > 5) {
            JOptionPane.showMessageDialog(null,
                    "Human deck: maximum five colors!",
                    "Human deck: too many colors", JOptionPane.ERROR_MESSAGE);
            result = false;
        }

        return result;
    }

    /** 
     * Checks lengths of selected values for AI color lists
     * to see if a deck generator exists. Alert and visual reminder if fail.
     *
     * @param ai0 &emsp; String[] of color names
     * @return boolean
     */
    public boolean checkValidityOfAISelectedColors(String[] ai0) {
        boolean result = true;

        if (ai0.length == 1) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, single color generated decks aren't supported yet."
                    + "\n\rPlease choose at least one more color for the AI deck.",
                    "AI deck: 1 color", JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        else if (ai0.length == 4) {
            JOptionPane.showMessageDialog(null,
                    "Sorry, four color generated decks aren't supported yet."
                    + "\n\rPlease use 2, 3, or 5 colors for the AI deck.",
                    "AI deck: 4 colors", JOptionPane.ERROR_MESSAGE);
            result = false;
        }
        else if (ai0.length > 5) {
            JOptionPane.showMessageDialog(null,
                    "AI deck: maximum five colors!",
                    "AI deck: Too many colors", JOptionPane.ERROR_MESSAGE);
            result = false;
        }

        return result;
    }

    /**
     * Random chooser for theme decks.
     * 
     * @param lst0 {@link javax.swing.JList}
     */
    public void randomPick(JList lst0) {
        Random r = new Random();
        int i = 0;
        if (lst0.getName().equals("lstThemesHuman") || lst0.getName().equals("lstThemesAI")) {
            i = r.nextInt(themeNames.size());
        }
        else {
            i = r.nextInt(deckNames.size());
        }

        lst0.setSelectedIndex(i);
        lst0.ensureIndexIsVisible(lst0.getSelectedIndex());
    }

    /** 
     * Regulates that only one of the three deck type JLists is in use at a time.
     *
     * @param lst0 {@link javax.swing.JList} that has been clicked
     */
    public void regulateHuman(JList lst0) {
        if (currentHumanSelection != null && lst0 != currentHumanSelection) {
            currentHumanSelection.clearSelection();
        }

        currentHumanSelection = lst0;

        // Random chooser for pre-constructed decks
        if (lst0.getName() != null && lst0.getName().equals("lstDecksHuman")
                && lst0.getSelectedIndex() == 0) {
            Random r = new Random();

            int i = 0;
            while (i == 0) { i = r.nextInt(deckNames.size()); }
            lst0.setSelectedIndex(i);
        }
    }

    /** 
     * Regulates that only one of the three deck type JLists is in use at a time.
     * 
     * @param lst0 &emsp; a JList that has been clicked
     */
    public void regulateAI(JList lst0) {
        if (currentAISelection != null && lst0 != currentAISelection) {
            currentAISelection.clearSelection();
        }

        currentAISelection = lst0;

        // Random chooser for theme decks
        if (lst0.getName() != null && lst0.getName().equals("lstThemesAI")
                && lst0.getSelectedIndex() == 0) {
            Random r = new Random();

            int i = 0;
            while (i == 0) { i = r.nextInt(themeNames.size()); }
            lst0.setSelectedIndex(i);
        }

        // Random chooser for pre-constructed decks
        if (lst0.getName() != null && lst0.getName().equals("lstDecksAI")
                && lst0.getSelectedIndex() == 0) {
            Random r = new Random();

            int i = 0;
            while (i == 0) { i = r.nextInt(deckNames.size()); }
            lst0.setSelectedIndex(i);
        }
    }

    //========= DECK GENERATION

    /** Generates human deck from current list selection(s). */
    private void generateHumanDecks(String[] selectedLines) {
        CardList cards = null;

        // Human: Color-based deck generation
        if (currentHumanSelection.getName().equals("lstColorsHuman")) {
            // Replace "random" with "AI" for deck generation code
            for (int i = 0; i < selectedLines.length; i++) {
                selectedLines[i] = colorVals.get(selectedLines[i]);
            }

            // 2, 3, and 5 colors.
            if (selectedLines.length == 2) {
                Generate2ColorDeck gen = new Generate2ColorDeck(
                        selectedLines[0], selectedLines[1]);
                cards = gen.get2ColorDeck(60, PlayerType.HUMAN);
            }
            else if (selectedLines.length == 3) {
                Generate3ColorDeck gen = new Generate3ColorDeck(
                        selectedLines[0], selectedLines[1], selectedLines[2]);
                cards = gen.get3ColorDeck(60, PlayerType.HUMAN);
            }
            else {
                Generate5ColorDeck gen = new Generate5ColorDeck(
                        selectedLines[0], selectedLines[1], selectedLines[2], selectedLines[3], selectedLines[4]);
                cards = gen.get5ColorDeck(60, PlayerType.HUMAN);
            }

            // After generating card lists, convert to deck and save.
            final Deck deck = new Deck();

            deck.getMain().add(cards);


            Constant.Runtime.HUMAN_DECK[0] = deck;
        }

        // Human: theme deck generation
        else if (currentHumanSelection.getName().equals("lstThemesHuman")) {
            GenerateThemeDeck gen = new GenerateThemeDeck();
            cards = gen.getThemeDeck(selectedLines[0], 60);

            // After generating card lists, convert to deck and save.
            final Deck deck = new Deck();
            deck.getMain().add(cards);

            Constant.Runtime.HUMAN_DECK[0] = deck;
        }

        // Human: deck file
        else {
            Constant.Runtime.HUMAN_DECK[0] = AllZone.getDecks().getConstructed().get(selectedLines[0]);
        }
    }

    private void generateAIDecks(String[] ai0) {
        CardList cards = null;
        // AI: Color-based deck generation
        if (currentAISelection.getName().equals("lstColorsAI")) {
            // Replace "random" with "AI" for deck generation code
            for (int i = 0; i < ai0.length; i++) {
                ai0[i] = colorVals.get(ai0[i]);
            }

            // 2, 3, and 5 colors.
            if (ai0.length == 2) {
                Generate2ColorDeck gen = new Generate2ColorDeck(
                        ai0[0], ai0[1]);
                cards = gen.get2ColorDeck(60, PlayerType.COMPUTER);
            }
            else if (ai0.length == 3) {
                Generate3ColorDeck gen = new Generate3ColorDeck(
                        ai0[0], ai0[1], ai0[2]);
                cards = gen.get3ColorDeck(60, PlayerType.COMPUTER);
            }
            else {
                Generate5ColorDeck gen = new Generate5ColorDeck(
                        ai0[0], ai0[1], ai0[2], ai0[3], ai0[4]);
                cards = gen.get5ColorDeck(60, PlayerType.COMPUTER);
            }

            // After generating card lists, convert to deck and save.
            final Deck deck = new Deck();
            deck.getMain().add(cards);

            Constant.Runtime.COMPUTER_DECK[0] = deck;
        }

        // AI: theme deck generation
        else if (currentAISelection.getName().equals("lstThemesAI")) {
            GenerateThemeDeck gen = new GenerateThemeDeck();

            // After generating card lists, convert to deck and save.
            final Deck deck = new Deck();
            deck.getMain().add(gen.getThemeDeck(ai0[0], 60));

            Constant.Runtime.COMPUTER_DECK[0] = deck;
        }

        // AI: deck file
        else {
            Constant.Runtime.COMPUTER_DECK[0] = AllZone.getDecks().getConstructed().get(ai0[0]);
        }
    }

    //========= OTHER
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

        final String[] humanSelected = oa2sa(currentHumanSelection.getSelectedValues());
        final String[] aiSelected = oa2sa(currentAISelection.getSelectedValues());
        Constant.Runtime.setGameType(GameType.Constructed);

        // Check color-based deck selection for appropriate length
        if (currentHumanSelection.getName().equals("lstColorsHuman")) {
            if (!checkValidityOfHumanSelectedColors(humanSelected)) {
                view.getBarProgress().setVisible(false);
                view.getBtnStart().setVisible(true);
                return;
            }
        }

        if (currentAISelection.getName().equals("lstColorsAI")) {
            if (!checkValidityOfAISelectedColors(aiSelected)) {
                view.getBarProgress().setVisible(false);
                view.getBtnStart().setVisible(true);
                return;
            }
        }

        generateAIDecks(aiSelected);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBarProgress().increment();
            }
         });

        generateHumanDecks(humanSelected);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBarProgress().increment();
            }
         });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBtnStart().setVisible(true);
                view.getBarProgress().setVisible(false);

                Singletons.getControl().changeState(FControl.MATCH_SCREEN);
                Singletons.getControl().getMatchControl().initMatch();

                AllZone.getGameAction().newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0]);
            }
        });
    }

    /**
     * What to do after exiting the deck editor in the deck lister.
     * 
     * @return Command
     */
    public Command getEditorExitCommand() {
        Command exit = new Command() {
            private static final long serialVersionUID = -9133358399503226853L;

            @Override
            public void execute() {

            }
        };

        return exit;
    }

    //========= LIST BOX VALUES
    /** */
    public void updateDeckNames() {
        deckNames = new ArrayList<String>();
        deckNames.add(0, "Random");

        
        deckNames.addAll(AllZone.getDecks().getConstructed().getNames());

        // No pre-constructed decks?
        if (deckNames.size() == 1) { deckNames = new ArrayList<String>(); }

        view.getLstDecksHuman().setListData(deckNames.toArray());
        view.getLstDecksAI().setListData(deckNames.toArray());
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
    private Object[] getThemeNames() {
        themeNames = new ArrayList<String>();
        for (String s : GenerateThemeDeck.getThemeNames()) {
            themeNames.add(s);
        }
        // No theme decks?
        if (themeNames.size() == 1) { themeNames = new ArrayList<String>(); }

        return themeNames.toArray();
    }

    /**
     * 
     * 
     */
    private void showDecklist(Deck d0) {
        HashMap<String, Integer> deckMap = new HashMap<String, Integer>();

        for (Entry<CardPrinted, Integer> s : d0.getMain()) {
            deckMap.put(s.getKey().getName(), s.getValue());
        }

        String nl = System.getProperty("line.separator");
        StringBuilder deckList = new StringBuilder();
        String dName = d0.getName();

        deckList.append(dName == null ? "" : dName + nl + nl);

        ArrayList<String> dmKeys = new ArrayList<String>();
        for (final String s : deckMap.keySet()) {
            dmKeys.add(s);
        }

        Collections.sort(dmKeys);

        for (String s : dmKeys) {
            deckList.append(deckMap.get(s) + " x " + s + nl);
        }

        final StringBuilder msg = new StringBuilder();
        if (deckMap.keySet().size() <= 32) {
            msg.append(deckList.toString() + nl);
        } else {
            msg.append("Decklist too long for dialog." + nl + nl);
        }

        msg.append("Copy Decklist to Clipboard?");

        // Output
        int rcMsg = JOptionPane.showConfirmDialog(null, msg, "Decklist", JOptionPane.OK_CANCEL_OPTION);
        if (rcMsg == JOptionPane.OK_OPTION) {
            final StringSelection ss = new StringSelection(deckList.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
        }
    } // End showDecklist

    /**
     * Exhaustively converts object array to string array.
     * Probably a much easier way to do this.
     * 
     * @param o0 &emsp; Object[]
     * @return String[]
     */
    private String[] oa2sa(Object[] o0) {
        String[] output = new String[o0.length];

        for (int i = 0; i < o0.length; i++) {
            output[i] = o0[i].toString();
        }

        return output;
    }
}
