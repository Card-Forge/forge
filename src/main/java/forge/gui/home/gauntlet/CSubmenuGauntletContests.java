package forge.gui.home.gauntlet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import forge.deck.DeckgenUtil.DeckTypes;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.MatchStartHelper;
import forge.game.player.PlayerType;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.model.FModel;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.util.IStorage;

/** 
 * Controls the "gauntlet contests" submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */

@SuppressWarnings("serial")
public enum CSubmenuGauntletContests implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final VSubmenuGauntletContests view = VSubmenuGauntletContests.SINGLETON_INSTANCE;

    private final MouseAdapter madDecklist = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() == 2) {
                DeckgenUtil.showDecklist(((JList) e.getSource())); }
        }
    };

    private final ActionListener actStartGame = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            startGame();
        }
    };

    private final Command cmdRandomRegular = new Command() {
        @Override
        public void execute() {
            DeckgenUtil.randomSelect(view.getLstDecks());
        }
    };

    private final Command cmdRandomColors = new Command() {
        @Override
        public void execute() {
            view.getLstDecks().setSelectedIndices(DeckgenUtil.randomSelectColors());
        }
    };

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
        final ActionListener deckUpdate = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
            updateDecks(); }
        };

        view.getBtnStart().addActionListener(actStartGame);
        view.getLstDecks().addMouseListener(madDecklist);

        // Deck list and radio button event handling
        view.getRadUserDecks().setSelected(true);

        view.getRadQuestDecks().addActionListener(deckUpdate);
        view.getRadColorDecks().addActionListener(deckUpdate);
        view.getRadThemeDecks().addActionListener(deckUpdate);
        view.getRadUserDecks().addActionListener(deckUpdate);

        updateDecks();
        updateData();

        view.getGauntletLister().setSelectedIndex(0);
    }

    /** Handles all control for "custom" radio button click. */
    private void updateDecks() {
        if (view.getRadUserDecks().isSelected()) {
            view.getBtnRandom().setCommand(cmdRandomRegular);
            updateUserDecks();
        }
        else if (view.getRadQuestDecks().isSelected()) {
            view.getBtnRandom().setCommand(cmdRandomRegular);
            updateQuestDecks();
        }
        else if (view.getRadThemeDecks().isSelected()) {
            view.getBtnRandom().setCommand(cmdRandomRegular);
            updateThemeDecks();
        }
        else if (view.getRadColorDecks().isSelected()) {
            view.getBtnRandom().setCommand(cmdRandomColors);
            updateColorDecks();
        }
    }

    private void updateData() {
        final File[] files = GauntletIO.getGauntletFilesLocked();
        final List<GauntletData> data = new ArrayList<GauntletData>();

        for (final File f : files) {
            if (f.getName().matches(GauntletIO.REGEX_LOCKED)) {
                data.add(GauntletIO.loadGauntlet(f));
            }
        }

        view.getGauntletLister().setGauntlets(data);
        view.getGauntletLister().setSelectedIndex(0);
    }

    private void updateUserDecks() {
        final List<String> customNames = new ArrayList<String>();
        final IStorage<Deck> allDecks = Singletons.getModel().getDecks().getConstructed();
        for (final Deck d : allDecks) { customNames.add(d.getName()); }

        view.getLstDecks().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        view.getLstDecks().setListData(customNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        view.getLstDecks().setName(DeckTypes.CUSTOM.toString());

        // Init first in list
        view.getLstDecks().setSelectedIndex(0);
    }

    /** Handles all control for "quest event" radio button click. */
    private void updateQuestDecks() {
        final List<String> eventNames = new ArrayList<String>();
        QuestController quest = Singletons.getModel().getQuest();

        for (final QuestEvent e : quest.getDuelsManager().getAllDuels()) {
            eventNames.add(e.getEventDeck().getName());
        }

        for (final QuestEvent e : quest.getChallengesManager().getAllChallenges()) {
            eventNames.add(e.getEventDeck().getName());
        }

        view.getLstDecks().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        view.getLstDecks().setListData(eventNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        view.getLstDecks().setName(DeckTypes.QUESTEVENTS.toString());

        // Init first in list
        view.getLstDecks().setSelectedIndex(0);
    }

    /** Handles all control for "themes" radio button click. */
    private void updateThemeDecks() {
        final List<String> themeNames = new ArrayList<String>();
        for (final String s : GenerateThemeDeck.getThemeNames()) { themeNames.add(s); }

        view.getLstDecks().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        view.getLstDecks().setListData(themeNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        view.getLstDecks().setName(DeckTypes.THEMES.toString());

        // Init first in list
        view.getLstDecks().setSelectedIndex(0);
    }

    /** Handles all control for "colors" radio button click. */
    private void updateColorDecks() {
        view.getLstDecks().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        view.getLstDecks().setListData(new String[] {"Random 1", "Random 2", "Random 3",
                "Random 4", "Black", "Blue", "Green", "Red", "White"});
        view.getLstDecks().setName(DeckTypes.COLORS.toString());

        // Init basic two color deck
        view.getLstDecks().setSelectedIndices(new int[]{0, 1});
    }

    /** */
    private void startGame() {
        final GauntletData gd = view.getGauntletLister().getSelectedGauntlet();
        final Deck userDeck;

        if (gd.getUserDeck() != null) {
            userDeck = gd.getUserDeck();
        }
        else {
            final String[] selection = Arrays.asList(
                    view.getLstDecks().getSelectedValues()).toArray(new String[0]);

            if (view.getRadColorDecks().isSelected()) {
                if (!DeckgenUtil.colorCheck(selection)) { return; }
                userDeck = DeckgenUtil.buildColorDeck(selection, PlayerType.HUMAN);
            }
            else if (view.getRadQuestDecks().isSelected()) {
                userDeck = DeckgenUtil.buildQuestDeck(selection);
            }
            else if (view.getRadThemeDecks().isSelected()) {
                userDeck = DeckgenUtil.buildThemeDeck(selection);
            }
            else {
                userDeck = DeckgenUtil.buildCustomDeck(selection);
            }
            gd.setUserDeck(userDeck);
        }

        gd.stamp();
        FModel.SINGLETON_INSTANCE.setGauntletData(gd);

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
                final GauntletData gd = FModel.SINGLETON_INSTANCE.getGauntletData();
                Deck aiDeck = gd.getDecks().get(gd.getCompleted());

                MatchStartHelper starter = new MatchStartHelper();
                Lobby lobby = Singletons.getControl().getLobby();
                starter.addPlayer(lobby.findLocalPlayer(PlayerType.HUMAN), gd.getUserDeck());
                starter.addPlayer(lobby.findLocalPlayer(PlayerType.COMPUTER), aiDeck);

                MatchController mc = Singletons.getModel().getMatch();
                mc.initMatch(GameType.Gauntlet, starter.getPlayerMap());
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

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return new Command() {
            @Override
            public void execute() {
                updateData();
            }
        };
    }
}
