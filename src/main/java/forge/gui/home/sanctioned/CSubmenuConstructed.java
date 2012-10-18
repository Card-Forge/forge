package forge.gui.home.sanctioned;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.ArrayUtils;

import forge.Command;
import forge.Singletons;
import forge.control.Lobby;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.MatchStartHelper;
import forge.game.player.PlayerType;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.ExperimentalLabel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.util.IStorage;

/** 
 * Controls the constructed submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuConstructed implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    private final VSubmenuConstructed view = VSubmenuConstructed.SINGLETON_INSTANCE;

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
            if (e.getClickCount() == 2) {
                final JList src = ((JList) e.getSource());
                if (src.equals(view.getLstDecksAI())) {
                    if (view.getRadColorsAI().isSelected()) { return; }
                    if (view.getRadThemesAI().isSelected()) { return; }
                }
                else {
                    if (view.getRadColorsHuman().isSelected()) { return; }
                    if (view.getRadThemesHuman().isSelected()) { return; }
                }

                DeckgenUtil.showDecklist(src);
            }
        }
    };

    private final QuestController quest = Singletons.getModel().getQuest();

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
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getLstUserDecks()
                : VSubmenuConstructed.SINGLETON_INSTANCE.getLstDecksAI());
        lst.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        lst.setListData(new String[] {"Random 1", "Random 2", "Random 3",
                "Random 4", "Black", "Blue", "Green", "Red", "White"});
        lst.setName(ESubmenuConstructedTypes.COLORS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final ExperimentalLabel btn = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getBtnHumanRandom()
                  : VSubmenuConstructed.SINGLETON_INSTANCE.getBtnAIRandom());

        btn.setCommand(new Command() { @Override
                    public void execute() { lst.setSelectedIndices(
                            DeckgenUtil.randomSelectColors()); } });

        // Init basic two color deck
        lst.setSelectedIndices(new int[]{0, 1});
    }

    /** Handles all control for "themes" radio button click. */
    private void updateThemes(final PlayerType player0) {
        final JList lst = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getLstUserDecks()
                : VSubmenuConstructed.SINGLETON_INSTANCE.getLstDecksAI());
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        lst.setName(ESubmenuConstructedTypes.COLORS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final List<String> themeNames = new ArrayList<String>();
        for (final String s : GenerateThemeDeck.getThemeNames()) { themeNames.add(s); }

        lst.setListData(themeNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(ESubmenuConstructedTypes.THEMES.toString());
        lst.removeMouseListener(madDecklist);

        final ExperimentalLabel btn = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getBtnHumanRandom()
                  : VSubmenuConstructed.SINGLETON_INSTANCE.getBtnAIRandom());

        btn.setCommand(new Command() { @Override
                    public void execute() { DeckgenUtil.randomSelect(lst); } });

        // Init first in list
        lst.setSelectedIndex(0);
    }

    /** Handles all control for "custom" radio button click. */
    private void updateCustom(final PlayerType player0) {
        final JList lst = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getLstUserDecks()
                : VSubmenuConstructed.SINGLETON_INSTANCE.getLstDecksAI());
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> customNames = new ArrayList<String>();
        final IStorage<Deck> allDecks = Singletons.getModel().getDecks().getConstructed();
        for (final Deck d : allDecks) { customNames.add(d.getName()); }

        lst.setListData(customNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(ESubmenuConstructedTypes.CUSTOM.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final ExperimentalLabel btn = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getBtnHumanRandom()
                  : VSubmenuConstructed.SINGLETON_INSTANCE.getBtnAIRandom());

        btn.setCommand(new Command() { @Override
                    public void execute() { DeckgenUtil.randomSelect(lst); } });

        // Init first in list
        lst.setSelectedIndex(0);
    }

    /** Handles all control for "quest event" radio button click. */
    private void updateQuestEvents(final PlayerType player0) {
        final JList lst = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getLstUserDecks()
                : VSubmenuConstructed.SINGLETON_INSTANCE.getLstDecksAI());
        lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final List<String> eventNames = new ArrayList<String>();

        for (final QuestEvent e : quest.getDuelsManager().getAllDuels()) {
            eventNames.add(e.getEventDeck().getName());
        }

        for (final QuestEvent e : quest.getChallengesManager().getAllChallenges()) {
            eventNames.add(e.getEventDeck().getName());
        }

        lst.setListData(eventNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        lst.setName(ESubmenuConstructedTypes.QUESTEVENTS.toString());
        lst.removeMouseListener(madDecklist);
        lst.addMouseListener(madDecklist);

        final ExperimentalLabel btn = (player0.equals(PlayerType.HUMAN)
                ? VSubmenuConstructed.SINGLETON_INSTANCE.getBtnHumanRandom()
                  : VSubmenuConstructed.SINGLETON_INSTANCE.getBtnAIRandom());

        btn.setCommand(new Command() { @Override
                    public void execute() { DeckgenUtil.randomSelect(lst); } });

        // Init first in list
        lst.setSelectedIndex(0);
    }

    /** @param lists0 &emsp; {@link java.util.List}<{@link javax.swing.JList}> */
    private void startGame() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        final SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
            @Override
            public Object doInBackground() {
                Deck humanDeck = generateDeck(VSubmenuConstructed.SINGLETON_INSTANCE.getLstUserDecks(), PlayerType.HUMAN);
                Deck aiDeck = generateDeck(VSubmenuConstructed.SINGLETON_INSTANCE.getLstDecksAI(), PlayerType.COMPUTER);

                MatchStartHelper starter = new MatchStartHelper();
                Lobby lobby = Singletons.getControl().getLobby();
                starter.addPlayer(lobby.findLocalPlayer(PlayerType.HUMAN), humanDeck);
                starter.addPlayer(lobby.findLocalPlayer(PlayerType.COMPUTER), aiDeck);
                
                MatchController mc = Singletons.getModel().getMatch(); 
                mc.initMatch(GameType.Constructed, starter.getPlayerMap());
                mc.startRound();
                
                return null;
            }

            @Override
            public void done() {
                SOverlayUtils.hideOverlay();
            }
        };
        worker.execute();
    }

    /** Generates deck from current list selection(s). */
    private Deck generateDeck(final JList lst0, final PlayerType player0) {
        final String[] selection = Arrays.copyOf(lst0.getSelectedValues(),
                lst0.getSelectedValues().length, String[].class);

        final Deck deck;

        if (selection.length == 0) { return null; }

        if (lst0.getName().equals(ESubmenuConstructedTypes.COLORS.toString()) && DeckgenUtil.colorCheck(selection)) {
            deck = DeckgenUtil.buildColorDeck(selection, player0);
        }
        else if (lst0.getName().equals(ESubmenuConstructedTypes.THEMES.toString())) {
            deck = DeckgenUtil.buildThemeDeck(selection);
        }
        else if (lst0.getName().equals(ESubmenuConstructedTypes.QUESTEVENTS.toString())) {
            deck = DeckgenUtil.buildQuestDeck(selection);
        }
        // Custom deck
        else if (lst0.getName().equals(ESubmenuConstructedTypes.CUSTOM.toString())) {
            deck = DeckgenUtil.buildCustomDeck(selection);
        }
        // Failure, for some reason
        else {
            deck = null;
        }

        return deck;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
