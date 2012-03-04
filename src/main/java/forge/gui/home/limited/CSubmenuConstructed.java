package forge.gui.home.limited;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.ArrayUtils;

import forge.CardList;
import forge.Command;
import forge.PlayerType;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.generate.Generate2ColorDeck;
import forge.deck.generate.Generate3ColorDeck;
import forge.deck.generate.Generate5ColorDeck;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.GameNew;
import forge.gui.home.ICSubmenu;
import forge.item.CardPrinted;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.quest.data.QuestEvent;
import forge.quest.data.QuestEventManager;
import forge.util.IStorage;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FOverlay;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FSkin;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuConstructed implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    private enum ESubmenuConstructedTypes { /** */
        COLORS, /** */
        THEMES, /** */
        CUSTOM, /** */
        QUESTEVENTS
    }

    private static final Map<String, String> COLOR_VALS = new HashMap<String, String>();

    private final MouseAdapter madDecklist = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() == 2) { showDecklist(((JList) e.getSource())); }
        }
    };

    static {
        COLOR_VALS.clear();
        COLOR_VALS.put("Random 1", "AI");
        COLOR_VALS.put("Random 2", "AI");
        COLOR_VALS.put("Random 3", "AI");
        COLOR_VALS.put("Random 4", "AI");
        COLOR_VALS.put("Black", "black");
        COLOR_VALS.put("Blue", "blue");
        COLOR_VALS.put("Green", "green");
        COLOR_VALS.put("Red", "red");
        COLOR_VALS.put("White", "white");
    }
    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#getMenuCommand()
     */
    @Override
    public Command getMenuCommand() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        // Nothing to see here...
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {
        final ForgePreferences prefs = Singletons.getModel().getPreferences();
        final VSubmenuConstructed view = VSubmenuConstructed.SINGLETON_INSTANCE;

        VSubmenuConstructed.SINGLETON_INSTANCE.populate();
        CSubmenuConstructed.SINGLETON_INSTANCE.update();

        // Radio button event handling
        view.getRadColorsAI().addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) {
                updateColors(PlayerType.COMPUTER); } });

        view.getRadColorsHuman().addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) {
                updateColors(PlayerType.HUMAN); } });

        view.getRadThemesAI().addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) {
                updateThemes(PlayerType.COMPUTER); } });

        view.getRadThemesHuman().addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) {
                updateThemes(PlayerType.HUMAN); } });

        view.getRadCustomAI().addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) {
            updateCustom(PlayerType.COMPUTER); } });

        view.getRadCustomHuman().addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) {
                updateCustom(PlayerType.HUMAN); } });

        view.getRadQuestsAI().addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) {
                updateQuestEvents(PlayerType.COMPUTER); } });

        view.getRadQuestsHuman().addActionListener(new ActionListener() { @Override
            public void actionPerformed(final ActionEvent arg0) {
                updateQuestEvents(PlayerType.HUMAN); } });

        // Checkbox event handling
        view.getBtnStart().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                startGame();
            }
        });

        // Checkbox event handling
        view.getCbSingletons().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(FPref.DECKGEN_SINGLETONS,
                        String.valueOf(view.getCbSingletons().isSelected()));
                prefs.save();
            }
        });

        view.getCbArtifacts().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(
                        FPref.DECKGEN_ARTIFACTS, String.valueOf(view.getCbArtifacts().isSelected()));
                prefs.save();
            }
        });

        view.getCbRemoveSmall().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(
                        FPref.DECKGEN_NOSMALL, String.valueOf(view.getCbRemoveSmall().isSelected()));
                prefs.save();
            }
        });

        // Pre-select checkboxes
        view.getCbSingletons().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        view.getCbArtifacts().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
        view.getCbRemoveSmall().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_NOSMALL));

        // First run: colors
        view.getRadColorsAI().setSelected(true);
        view.getRadColorsHuman().setSelected(true);

        updateColors(PlayerType.COMPUTER);
        updateColors(PlayerType.HUMAN);
    }

    /** Handles all control for "colors" radio button click. */
    private void updateColors(final PlayerType player0) {
        final JList lst = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getLstHumanDecks()
                : VSubmenuConstructed.SINGLETON_INSTANCE.getLstAIDecks());
        lst.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        lst.setListData(new String[] {"Random 1", "Random 2", "Random 3",
                "Random 4", "Black", "Blue", "Green", "Red", "White"});
        lst.setName(ESubmenuConstructedTypes.COLORS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final FLabel btn = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getBtnHumanRandom()
                  : VSubmenuConstructed.SINGLETON_INSTANCE.getBtnAIRandom());

        btn.setCommand(new Command() { @Override
                    public void execute() { randomSelectColors(lst); } });

        // Init with a random select.
        randomSelectColors(lst);
    }

    /** Handles all control for "themes" radio button click. */
    private void updateThemes(final PlayerType player0) {
        final JList lst = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getLstHumanDecks()
                : VSubmenuConstructed.SINGLETON_INSTANCE.getLstAIDecks());
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        lst.setName(ESubmenuConstructedTypes.COLORS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final List<String> themeNames = new ArrayList<String>();
        for (final String s : GenerateThemeDeck.getThemeNames()) { themeNames.add(s); }

        lst.setListData(themeNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(ESubmenuConstructedTypes.THEMES.toString());
        lst.removeMouseListener(madDecklist);

        final FLabel btn = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getBtnHumanRandom()
                  : VSubmenuConstructed.SINGLETON_INSTANCE.getBtnAIRandom());

        btn.setCommand(new Command() { @Override
                    public void execute() { randomSelectRegular(lst); } });

        // Init with a random select.
        randomSelectRegular(lst);
    }

    /** Handles all control for "custom" radio button click. */
    private void updateCustom(final PlayerType player0) {
        final JList lst = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getLstHumanDecks()
                : VSubmenuConstructed.SINGLETON_INSTANCE.getLstAIDecks());
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> customNames = new ArrayList<String>();
        final IStorage<Deck> allDecks = Singletons.getModel().getDecks().getConstructed();
        for (final Deck d : allDecks) { customNames.add(d.getName()); }

        lst.setListData(customNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(ESubmenuConstructedTypes.CUSTOM.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final FLabel btn = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getBtnHumanRandom()
                  : VSubmenuConstructed.SINGLETON_INSTANCE.getBtnAIRandom());

        btn.setCommand(new Command() { @Override
                    public void execute() { randomSelectRegular(lst); } });

        // Init with a random select.
        randomSelectRegular(lst);
    }

    /** Handles all control for "quest event" radio button click. */
    private void updateQuestEvents(final PlayerType player0) {
        final JList lst = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getLstHumanDecks()
                : VSubmenuConstructed.SINGLETON_INSTANCE.getLstAIDecks());
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> eventNames = new ArrayList<String>();

        for (final QuestEvent e : QuestEventManager.ALL_DUELS) {
            eventNames.add(e.getEventDeck().getName());
        }

        for (final QuestEvent e : QuestEventManager.ALL_CHALLENGES) {
            eventNames.add(e.getEventDeck().getName());
        }

        lst.setListData(eventNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(ESubmenuConstructedTypes.QUESTEVENTS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final FLabel btn = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getBtnHumanRandom()
                  : VSubmenuConstructed.SINGLETON_INSTANCE.getBtnAIRandom());

        btn.setCommand(new Command() { @Override
                    public void execute() { randomSelectRegular(lst); } });

        // Init with a random select.
        randomSelectRegular(lst);
    }
    /** 
     * Checks lengths of selected values for color lists
     * to see if a deck generator exists. Alert and visual reminder if fail.
     * 
     * @param colors0 &emsp; String[] of color names
     * @return boolean
     */
    private static boolean colorCheck(final String[] colors0) {
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

    /** @param lists0 &emsp; {@link java.util.List}<{@link javax.swing.JList}> */
    private void startGame() {
        final FOverlay overlay = Singletons.getView().getOverlay();
        overlay.setLayout(new MigLayout("insets 0, gap 0, align center"));

        final FPanel pnl = new FPanel();
        pnl.setLayout(new MigLayout("insets 0, gap 0, ax center, wrap"));
        pnl.setBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE));

        pnl.add(new FLabel.Builder().icon(FSkin.getIcon(FSkin.ForgeIcons.ICO_LOGO)).build(),
                "h 200px!, align center");
        pnl.add(new FLabel.Builder().text("Loading new game...")
                .fontScaleAuto(false).fontSize(22).build(), "h 40px!, align center");

        overlay.add(pnl, "h 300px!, w 400px!");

        overlay.showOverlay();
        GameNew.newGame(
                generateDeck(VSubmenuConstructed.SINGLETON_INSTANCE.getLstHumanDecks(), PlayerType.HUMAN),
                generateDeck(VSubmenuConstructed.SINGLETON_INSTANCE.getLstAIDecks(), PlayerType.COMPUTER));
        overlay.hideOverlay();
    }

    /** Generates deck from current list selection(s). */
    private static Deck generateDeck(final JList lst0, final PlayerType player0) {
        CardList cards = null;
        final String[] selection = Arrays.copyOf(lst0.getSelectedValues(),
                lst0.getSelectedValues().length, String[].class);

        final Deck deck;

        // Color deck
        if (lst0.getName().equals(ESubmenuConstructedTypes.COLORS.toString()) && colorCheck(selection)) {
            // Replace "random" with "AI" for deck generation code
            for (int i = 0; i < selection.length; i++) {
                selection[i] = COLOR_VALS.get(selection[i]);
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
        }

        // Theme deck
        else if (lst0.getName().equals(ESubmenuConstructedTypes.THEMES.toString())) {
            final GenerateThemeDeck gen = new GenerateThemeDeck();
            cards = gen.getThemeDeck(selection[0], 60);

            // After generating card lists, build deck.
            deck = new Deck();
            deck.getMain().add(cards);
        }
        else if (lst0.getName().equals(ESubmenuConstructedTypes.QUESTEVENTS.toString())) {
            deck = QuestEventManager.getEvent(selection[0]).getEventDeck();
        }
        // Custom deck
        else if (lst0.getName().equals(ESubmenuConstructedTypes.CUSTOM.toString())) {
            deck = Singletons.getModel().getDecks().getConstructed().get(selection[0]);
        }
        // Failure, for some reason
        else {
            deck = null;
        }

        return deck;
    }

    /** Shows decklist dialog for a given deck.
     * @param d0 &emsp; {@link forge.deck.Deck} */
    private void showDecklist(final JList lst0) {
        final String deckName = lst0.getSelectedValue().toString();
        final Deck deck;

        // Retrieve from custom or quest deck maps
        if (lst0.getName().equals(ESubmenuConstructedTypes.CUSTOM.toString())) {
            deck = Singletons.getModel().getDecks().getConstructed().get(deckName);
        }
        else {
            deck = QuestEventManager.getEvent(deckName).getEventDeck();
        }

        // Dump into map and display.
        final HashMap<String, Integer> deckMap = new HashMap<String, Integer>();

        for (final Entry<CardPrinted, Integer> s : deck.getMain()) {
            deckMap.put(s.getKey().getName(), s.getValue());
        }

        final String nl = System.getProperty("line.separator");
        final StringBuilder deckList = new StringBuilder();
        final String dName = deck.getName();

        deckList.append(dName == null ? "" : dName + nl + nl);

        final ArrayList<String> dmKeys = new ArrayList<String>();
        for (final String s : deckMap.keySet()) {
            dmKeys.add(s);
        }

        Collections.sort(dmKeys);

        for (final String s : dmKeys) {
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
        final int rcMsg = JOptionPane.showConfirmDialog(null, msg, "Decklist", JOptionPane.OK_CANCEL_OPTION);
        if (rcMsg == JOptionPane.OK_OPTION) {
            final StringSelection ss = new StringSelection(deckList.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
        }
    } // End showDecklist

    /** @param lst0 &emsp; {@link javax.swing.JList} */
    private void randomSelectColors(final JList lst0) {
     // Color select algorithm
        int x = -1;
        // HACK because 1 and 4 color decks are not supported yet. :(
        while (x == -1 || x == 1 || x == 4) {
            x = (int) Math.ceil(Math.random() * 5);
        }
        final int colorCount = x;

        final int maxCount = lst0.getModel().getSize();
        int[] selectedIndices = new int[colorCount];

        x = -1;
        for (int i = 0; i < colorCount; i++) {
            while (x == -1) {
                x = (int) Math.floor(Math.random() * maxCount);
                if (Arrays.asList(selectedIndices).contains(x)) { x = -1; }
                else { selectedIndices[i] = x; }
            }
            x = -1;
        }
        lst0.setSelectedIndices(selectedIndices);
        selectedIndices = null;
    }

    /** @param lst0 &emsp; {@link javax.swing.JList} */
    private void randomSelectRegular(final JList lst0) {
        final int size = lst0.getModel().getSize();

        if (size > 0) {
            final Random r = new Random();
            final int i = r.nextInt(size);

            lst0.setSelectedIndex(i);
            lst0.ensureIndexIsVisible(lst0.getSelectedIndex());
        }
    }
}
