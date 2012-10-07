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

import forge.AllZone;
import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.GameNew;
import forge.game.GameType;
import forge.game.PlayerStartsGame;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletDeckUtil;
import forge.gauntlet.GauntletDeckUtil.DeckTypes;
import forge.gauntlet.GauntletIO;
import forge.gui.SOverlayUtils;
import forge.gui.framework.ICDoc;
import forge.model.FModel;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.util.IStorage;

/** 
 * Controls the "quick gauntlet" submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */

@SuppressWarnings("serial")
public enum CSubmenuGauntletQuick implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final MouseAdapter madDecklist = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() == 2) {
                GauntletDeckUtil.showDecklist(((JList) e.getSource())); }
        }
    };

    private final Command cmdRandomRegular = new Command() {
        @Override
        public void execute() {
            GauntletDeckUtil.randomSelect(view.getLstDecks());
        }
    };

    private final Command cmdRandomColors = new Command() {
        @Override
        public void execute() {
            view.getLstDecks().setSelectedIndices(GauntletDeckUtil.randomSelectColors());
        }
    };

    private final ActionListener actStartGame = new ActionListener() { @Override
        public void actionPerformed(ActionEvent arg0) { startGame(); } };

    private final VSubmenuGauntletQuick view = VSubmenuGauntletQuick.SINGLETON_INSTANCE;

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
        final File[] files = GauntletIO.getGauntletFilesUnlocked();
        final List<GauntletData> data = new ArrayList<GauntletData>();

        for (final File f : files) {
            data.add(GauntletIO.loadGauntlet(f));
        }

        view.getGauntletLister().setGauntlets(data);
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
        QuestController quest = AllZone.getQuest();

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
        // Start game overlay
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

        // Find appropriate filename for new save, create and set new save file.
        final File[] arrFiles = GauntletIO.getGauntletFilesQuick();
        final List<String> lstNames = new ArrayList<String>();
        for (File f : arrFiles) { lstNames.add(f.getName()); }

        int num = 1;
        while (lstNames.contains(GauntletIO.PREFIX_QUICK + num + ".dat")) { num++; }
        FModel.SINGLETON_INSTANCE.getGauntletData().setActiveFile(new File(
                GauntletIO.DIR_GAUNTLETS + GauntletIO.PREFIX_QUICK + num + ".dat"));

        // Pull user deck
        final Deck userDeck;
        final String[] selection = Arrays.asList(
                view.getLstDecks().getSelectedValues()).toArray(new String[0]);

        if (view.getRadColorDecks().isSelected()) {
            if (!GauntletDeckUtil.colorCheck(selection)) { return; }
            userDeck = GauntletDeckUtil.buildColorDeck(selection);
        }
        else if (view.getRadQuestDecks().isSelected()) {
            userDeck = GauntletDeckUtil.buildQuestDeck(selection);
        }
        else if (view.getRadThemeDecks().isSelected()) {
            userDeck = GauntletDeckUtil.buildThemeDeck(selection);
        }
        else {
            userDeck = GauntletDeckUtil.buildCustomDeck(selection);
        }

        // Generate gauntlet decks
        final int numOpponents = view.getSliOpponents().getValue();
        final List<DeckTypes> lstDecktypes = new ArrayList<DeckTypes>();
        final List<String> lstEventNames = new ArrayList<String>();
        final List<Deck> lstGauntletDecks = new ArrayList<Deck>();
        Deck tempDeck;
        int randType;

        if (view.getBoxColorDecks().isSelected()) { lstDecktypes.add(DeckTypes.COLORS); }
        if (view.getBoxThemeDecks().isSelected()) { lstDecktypes.add(DeckTypes.THEMES); }
        if (view.getBoxUserDecks().isSelected()) { lstDecktypes.add(DeckTypes.CUSTOM); }
        if (view.getBoxQuestDecks().isSelected()) { lstDecktypes.add(DeckTypes.QUESTEVENTS); }

        for (int i = 0; i < numOpponents; i++) {
            randType = (int) Math.round(Math.random() * (lstDecktypes.size() - 1));
            if (lstDecktypes.get(randType).equals(DeckTypes.COLORS)) {
                tempDeck = GauntletDeckUtil.getRandomColorDeck();
                lstEventNames.add("Random colors deck");
            }
            else if (lstDecktypes.get(randType).equals(DeckTypes.THEMES)) {
                tempDeck = GauntletDeckUtil.getRandomThemeDeck();
                lstEventNames.add("Random theme deck");
            }
            else if (lstDecktypes.get(randType).equals(DeckTypes.CUSTOM)) {
                tempDeck =  GauntletDeckUtil.getRandomCustomDeck();
                lstEventNames.add(tempDeck.getName());
            }
            else {
                tempDeck =  GauntletDeckUtil.getRandomQuestDeck();
                lstEventNames.add(tempDeck.getName());
            }

            lstGauntletDecks.add(tempDeck);
        }

        FModel.SINGLETON_INSTANCE.getGauntletData()
            .setDecks(lstGauntletDecks);
        FModel.SINGLETON_INSTANCE.getGauntletData()
            .setEventNames(lstEventNames);

        // Reset all variable fields to 0, stamps and saves automatically.
        FModel.SINGLETON_INSTANCE.getGauntletData().reset();

        FModel.SINGLETON_INSTANCE.getGauntletData()
            .setUserDeck(userDeck);

        final SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
            @Override
            public Object doInBackground() {
                final GauntletData gd = FModel.SINGLETON_INSTANCE.getGauntletData();

                Singletons.getModel().getMatchState().setGameType(GameType.Gauntlet);

                Deck human = gd.getUserDeck();
                Deck aiDeck = gd.getDecks().get(gd.getCompleted()); 
                if (human != null && aiDeck != null) {
                    GameNew.newGame(new PlayerStartsGame( AllZone.getHumanPlayer(), human),
                            new PlayerStartsGame(AllZone.getComputerPlayer(), aiDeck));
                }
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
        return null;
    }
}
