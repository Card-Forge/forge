package forge.control.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.game.GameType;
import forge.gui.GuiUtils;
import forge.gui.deckeditor.DeckEditorQuest;
import forge.gui.deckeditor.DeckEditorShop;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestChallenge;
import forge.quest.data.QuestData;
import forge.quest.data.QuestDataIO;
import forge.quest.data.QuestEvent;
import forge.quest.data.QuestEventManager;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.data.QuestUtil;
import forge.quest.data.item.QuestItemZeppelin;
import forge.quest.data.pet.QuestPetAbstract;
import forge.view.GuiTopLevel;
import forge.view.home.ViewQuest;

/** 
 * Controls logic and listeners for Quest mode in home screen.
 * 
 */
public class ControlQuest {
    private ViewQuest view;
    private QuestEvent event;
    private QuestData qData;
    private QuestPreferences qPrefs;
    private QuestEventManager qem;
    private JPanel selectedTab;

    private final MouseAdapter madStartGame, madDuels, madChallenges,
        madQuests, madDecks, madPreferences;
    private final ActionListener actPetSelect, actPlantSelect;
    private final Command cmdSpellShop, cmdBazaar,
        cmdEmbark, cmdNewDeck, cmdCurrentDeck, cmdResetPrefs, cmdDeckExit,
        cmdDeckDelete, cmdDeckSelect, cmdQuestSelect, cmdQuestDelete;
    private Deck currentDeck;
    private Map<String, QuestData> arrQuests;

    /**
     * Controls logic and listeners for quest mode in home screen.
     * 
     * @param v0 &emsp; ViewQuest
     */
    @SuppressWarnings("serial")
    public ControlQuest(ViewQuest v0) {
        // Inits
        this.view = v0;
        this.qem = new QuestEventManager();
        this.qPrefs = Singletons.getModel().getQuestPreferences();
        AllZone.setQuestEventManager(this.qem);

        //========= Listener inits

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

        madDuels = new MouseAdapter() { @Override
            public void mouseClicked(MouseEvent e) { view.showDuelsTab(); } };

        madChallenges = new MouseAdapter() { @Override
            public void mouseClicked(MouseEvent e) { view.showChallengesTab(); } };

        madQuests = new MouseAdapter() { @Override
            public void mouseClicked(MouseEvent e) { view.showQuestsTab(); } };

        madDecks = new MouseAdapter() { @Override
            public void mouseClicked(MouseEvent e) {
                view.showDecksTab();
                if (ControlQuest.this.qem != null) { refreshDecks(); }
            }
        };

        madPreferences = new MouseAdapter() { @Override
            public void mouseClicked(MouseEvent e) { view.showPrefsTab(); } };

        actPetSelect = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                if (view.getCbxPet().getSelectedIndex() > 0) {
                    qData.getPetManager().setSelectedPet(
                            view.getCbxPet().getSelectedItem().toString().substring(7));
                } else {
                    qData.getPetManager().setSelectedPet(null);
                }
            }
        };

        actPlantSelect = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                qData.getPetManager()
                        .setUsePlant(view.getCbPlant().isSelected());
            }
        };

        cmdSpellShop = new Command() { @Override
            public void execute() { showSpellShop(); } };

        cmdBazaar = new Command() { @Override
            public void execute() { showBazaar(); } };

        cmdEmbark = new Command() { @Override
            public void execute() { newQuest(); } };

        cmdCurrentDeck = new Command() { @Override
            public void execute() { view.showDecksTab(); } };

        cmdNewDeck = new Command() { @Override
            public void execute() {
                final DeckEditorQuest editor = new DeckEditorQuest(qData);
                editor.show(cmdDeckExit);
                editor.setVisible(true);
            }
        };

        cmdResetPrefs = new Command() {
            @Override
            public void execute() {
                qPrefs.reset();
                qPrefs.save();
                view.resetPrefs();
            }
        };

        cmdDeckExit = new Command() {
            @Override
            public void execute() {
                AllZone.getQuestData().saveData();
                refreshDecks();
                GuiTopLevel g = ((GuiTopLevel) AllZone.getDisplay());
                g.getController().getHomeView().getBtnQuest().grabFocus();
            }
        };

        cmdDeckSelect = new Command() {
            @Override
            public void execute() {
                currentDeck = view.getLstDecks().getSelectedDeck();
                qPrefs.setPreference(QPref.CURRENT_DECK, currentDeck.toString());
                view.setCurrentDeckStatus();
            }
        };

        cmdDeckDelete = new Command() { @Override
            public void execute() { refreshDecks(); } };

        cmdQuestSelect = new Command() { @Override
            public void execute() { changeQuest(); } };

        cmdQuestDelete = new Command() { @Override
            public void execute() { refreshQuests(); } };

        addListeners();
    }

    /** @return ViewQuest */
    public ViewQuest getView() {
        return view;
    }

    /** @return {@link forge.quest.gui.main.QuestEventManager} */
    public QuestEventManager getQEM() {
        return this.qem;
    }

    /** @return {@link forge.Command} What to do when the deck editor exits. */
    public Command getExitCommand() {
        return cmdDeckExit;
    }

    /** @return String &emsp; indicates the rank of this current quest */
    public String getRankString() {
        return qData.getRank();
    }

    /** @return forge.deck.Deck */
    public Deck getCurrentDeck() {
        return this.currentDeck;
    }

    /** @return  */
    public Map<String, QuestData> getAllQuests() {
        return arrQuests;
    }

    /**
     * Updates visual state of tabber.
     * @param tab0 &emsp; JPanel tab object (can pass SubTab too).
     */
    public void updateTabber(JPanel tab0) {
        if (selectedTab != null) {
            selectedTab.setEnabled(false);
        }

        tab0.setEnabled(true);
        selectedTab = tab0;
    }

    private void addListeners() {
        view.getTabDuels().removeMouseListener(madDuels);
        view.getTabDuels().addMouseListener(madDuels);

        view.getTabChallenges().removeMouseListener(madChallenges);
        view.getTabChallenges().addMouseListener(madChallenges);

        view.getTabDecks().removeMouseListener(madDecks);
        view.getTabDecks().addMouseListener(madDecks);

        view.getTabQuests().removeMouseListener(madQuests);
        view.getTabQuests().addMouseListener(madQuests);

        view.getTabPreferences().removeMouseListener(madPreferences);
        view.getTabPreferences().addMouseListener(madPreferences);

        view.getLstQuests().setSelectCommand(cmdQuestSelect);
        view.getLstQuests().setEditCommand(cmdQuestDelete);
        view.getLstQuests().setDeleteCommand(cmdQuestDelete);

        view.getBtnEmbark().setCommand(cmdEmbark);
        view.getBtnResetPrefs().setCommand(cmdResetPrefs);

        if (this.qem != null) {
            view.getBtnStart().removeMouseListener(madStartGame);
            view.getBtnStart().addMouseListener(madStartGame);

            view.getBtnBazaar().setCommand(cmdBazaar);

            view.getBtnNewDeck().setCommand(cmdNewDeck);

            view.getBtnCurrentDeck().setCommand(cmdCurrentDeck);

            view.getBtnSpellShop().setCommand(cmdSpellShop);

            view.getCbxPet().addActionListener(actPetSelect);

            view.getCbPlant().addActionListener(actPlantSelect);

            view.getLstDecks().setSelectCommand(cmdDeckSelect);
            view.getLstDecks().setDeleteCommand(cmdDeckDelete);

            view.getLstDecks().setExitCommand(cmdDeckExit);
        }
    }

    /**
     * The actuator for new quests.
     */
    private void newQuest() {
        int difficulty = 0;
        QuestData newdata = new QuestData();

        final String mode = view.getRadFantasy().isSelected()
                ? forge.quest.data.QuestData.FANTASY
                : forge.quest.data.QuestData.CLASSIC;

        if (view.getRadEasy().isSelected()) {
            difficulty = 0;
        } else if (view.getRadMedium().isSelected()) {
            difficulty = 1;
        } else if (view.getRadHard().isSelected()) {
            difficulty = 2;
        } else if (view.getRadExpert().isSelected()) {
            difficulty = 3;
        } else {
            throw new IllegalStateException(
                    "ControlQuest() > newQuest(): Error starting new quest!");
        }

        final Object o = JOptionPane.showInputDialog(null, "Poets will remember your quest as:", "Quest Name", JOptionPane.OK_CANCEL_OPTION);

        if (o == null) { return; }

        final String questName = GuiUtils.cleanString(o.toString());

        if (getAllQuests().get(questName) != null || questName.equals("")) {
            JOptionPane.showMessageDialog(null, "Please pick another quest name, a quest already has that name.");
            return;
        }

        // Give the user a few cards to build a deck
        newdata.newGame(difficulty, mode, view.getCbStandardStart().isSelected());
        newdata.setName(questName);
        newdata.saveData();

        // Save in preferences.
        qPrefs.setPreference(QPref.CURRENT_QUEST, questName + ".dat");
        Singletons.getModel().getQuestPreferences().save();

        view.getParentView().resetQuest();
    }   // New Quest

    private void changeQuest() {
        AllZone.setQuestData(view.getLstQuests().getSelectedQuest());
        this.qData = AllZone.getQuestData();
        this.qem.assembleAllEvents();
        AllZone.setQuestEventManager(this.qem);

        // Save in preferences.
        qPrefs.setPreference(QPref.CURRENT_QUEST, qData.getName() + ".dat");
        Singletons.getModel().getQuestPreferences().save();

        refreshDecks();
        refreshStats();
    }

    /** Resets quests, then retrieves and sets current quest. */
    public void refreshQuests() {
        File dirQuests = ForgeProps.getFile(NewConstants.Quest.DATA_DIR);

        // Temporary transition code between v1.2.2 and v1.2.3.
        // Can be safely deleted after release of 1.2.3.
        if (!dirQuests.exists()) {
            dirQuests.mkdirs();
        }
        File olddata = new File("res/quest/questData.dat");
        File newpath = new File(dirQuests.getPath() + "/questData.dat");

        if (olddata.exists()) { olddata.renameTo(newpath); }
        // end block which can be deleted

        // Iterate over files and load quest datas for each.
        FilenameFilter takeDatFiles = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".dat");
            }
        };
        File[] arrFiles = dirQuests.listFiles(takeDatFiles);
        arrQuests = new HashMap<String, QuestData>();
        for (File f : arrFiles) {
            arrQuests.put(f.getName(), QuestDataIO.loadData(f));
        }

        // Populate list with available quest datas.
        view.getLstQuests().setQuests(arrQuests.values().toArray(new QuestData[0]));

        // If there are quests available, force select.
        if (arrQuests.size() > 0) {
            final String questname = qPrefs.getPreference(QPref.CURRENT_QUEST);

            // Attempt to select previous quest.
            if (arrQuests.get(questname) != null) {
                view.getLstQuests().setSelectedQuestData(arrQuests.get(questname));
            }
            else {
                view.getLstQuests().setSelectedIndex(0);
            }

            // Drop into AllZone.
            AllZone.setQuestData(view.getLstQuests().getSelectedQuest());
        }
        else {
            AllZone.setQuestData(null);
        }

        this.qData = AllZone.getQuestData();

        if (qem.getAllDuels() == null) {
            qem.assembleAllEvents();
        }
    }

    /** Resets decks, then retrieves and sets current deck. */
    public void refreshDecks() {
        // Retrieve and set all decks
        Deck[] temp = (qData == null ? new Deck[] {} : qData.getDecks().toArray(new Deck[0]));
        view.getLstDecks().setDecks(temp);

        // Look through list for preferred deck from prefs
        currentDeck = null;
        final String cd = qPrefs.getPreference(QPref.CURRENT_DECK);
        for (Deck d : temp) {
            if (d.getName().equals(cd)) {
                currentDeck = d;
                view.getLstDecks().setSelectedDeck(d);
                break;
            }
        }

        // Not found? Set first one. Still not found? OK, throw to setCurrentDeckStatus().
        if (currentDeck == null) { view.getLstDecks().setSelectedIndex(0); }
        view.setCurrentDeckStatus();
    }

    /** Updates all statistics in several panels. */
    public void refreshStats() {
        if (qData == null) { return; }

        // Stats panel
        view.getLblCredits().setText("Credits: " + qData.getCredits());
        view.getLblLife().setText("Life: " + qData.getLife());
        view.getLblWins().setText("Wins: " + qData.getWin());
        view.getLblLosses().setText("Losses: " + qData.getLost());
        view.getBarProgress().setVisible(false);
        view.setCurrentDeckStatus();

        final int num = nextChallengeInWins();
        if (num == 0) {
            view.getLblNextChallengeInWins().setText("Next challenge available now.");
        }
        else {
            view.getLblNextChallengeInWins().setText("Next challenge available in " + num + " wins.");
        }

        view.getLblWinStreak().setText(
                "Win streak: " + qData.getWinStreakCurrent()
                + " (Best:" + qData.getWinStreakBest() + ")");

        // Start panel: pet, plant, zep.
        if (this.qData.getMode().equals(QuestData.FANTASY)) {
            final Set<String> petList = this.qData.getPetManager().getAvailablePetNames();
            final QuestPetAbstract currentPet = this.qData.getPetManager().getSelectedPet();

            view.getCbxPet().removeAllItems();
            // Pet list visibility
            if (petList.size() > 0) {
                view.getCbxPet().setEnabled(true);
                view.getCbxPet().addItem("Don't summon a pet");
                for (final String pet : petList) {
                    view.getCbxPet().addItem("Summon " + pet);
                }

                if (currentPet != null) { view.getCbxPet().setSelectedItem("Summon " + currentPet.getName()); }
            } else {
                view.getCbxPet().setVisible(false);
            }

            // Plant visiblity
            if (this.qData.getPetManager().getPlant().getLevel() == 0) {
                view.getCbPlant().setVisible(false);
            }
            else {
                view.getCbPlant().setVisible(true);
                view.getCbPlant().setSelected(this.qData.getPetManager().shouldPlantBeUsed());
            }

            // Zeppelin visibility
            final QuestItemZeppelin zeppelin = (QuestItemZeppelin) this.qData.getInventory().getItem("Zeppelin");
            view.getCbZep().setVisible(zeppelin.hasBeenUsed());
        }
        else {
            view.getCbxPet().setVisible(false);
            view.getCbPlant().setVisible(false);
            view.getCbZep().setVisible(false);
        }
    }

    /** */
    @SuppressWarnings("serial")
    private void showSpellShop() {
        final Command exit = new Command() {
            @Override
            public void execute() {
                AllZone.getQuestData().saveData();
                refreshStats();
            }
        };

        DeckEditorShop g = new DeckEditorShop(AllZone.getQuestData());
        g.show(exit);
        g.setVisible(true);
    }

    /** */
    private void showBazaar() {
        GuiTopLevel g = ((GuiTopLevel) AllZone.getDisplay());

        g.getController().changeState(FControl.QUEST_BAZAAR);
        g.validate();
    }

    /** */
    private void startGame() {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "ControlQuest() > startGame() must be accessed from outside the event dispatch thread.");
        }

        if (currentDeck == null) {
            JOptionPane.showMessageDialog(null,
                    "A mysterious wall blocks your way."
                    + "\n\rAn unseen sepulchral voice booms:"
                    + "\n\r\"Entrance Forbidden Without A Deck\"",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
        }

        view.getBarProgress().setVisible(true);

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

        event = view.getSelectedOpponent().getEvent();
        AllZone.setQuestEvent(event);
        Constant.Runtime.setGameType(GameType.Quest);
        final QuestItemZeppelin zeppelin = (QuestItemZeppelin) qData.getInventory().getItem("Zeppelin");
        zeppelin.setZeppelinUsed(false);
        qData.randomizeOpponents();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBarProgress().increment();
            }
         });

        Constant.Runtime.HUMAN_DECK[0] = currentDeck;
        Constant.Runtime.COMPUTER_DECK[0] = event.getEventDeck();
        Constant.Quest.OPP_ICON_NAME[0] = event.getIconFilename();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBarProgress().increment();
            }
         });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                GuiTopLevel g = (GuiTopLevel) AllZone.getDisplay();
                g.getController().changeState(FControl.MATCH_SCREEN);
                g.getController().getMatchController().initMatch();

                AllZone.getMatchState().reset();
                if (event.getEventType().equals("challenge")) {
                    setupChallenge(currentDeck);
                } else {
                    setupDuel(currentDeck);
                }
                qData.saveData();
            }
        });
    }

    /**
     * <p>
     * setupDuel.
     * </p>
     * 
     * @param humanDeck
     *            a {@link forge.deck.Deck} object.
     */
    private void setupDuel(final Deck humanDeck) {
        final Deck computer = event.getEventDeck();
        Constant.Runtime.COMPUTER_DECK[0] = computer;

        AllZone.getGameAction().newGame(
                Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0],
                QuestUtil.getHumanStartingCards(qData),
                QuestUtil.getComputerStartingCards(qData),
                qData.getLife(), 20);
    }

    /**
     * <p>
     * setupChallenge.
     * </p>
     * 
     * @param humanDeck
     *            a {@link forge.deck.Deck} object.
     */
    private void setupChallenge(final Deck humanDeck) {
        int extraLife = 0;

        if (qData.getInventory().getItemLevel("Gear") == 2) {
            extraLife = 3;
        }

        AllZone.getGameAction().newGame(
                Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0],
                QuestUtil.getHumanStartingCards(qData, event),
                QuestUtil.getComputerStartingCards(qData, event),
                qData.getLife() + extraLife, ((QuestChallenge) event).getAILife());

    }

    /**
     * <p>
     * nextChallengeInWins.
     * </p>
     * 
     * @return a int.
     */
    private int nextChallengeInWins() {
        // Number of wins was 25, lowering the number to 20 to help short term
        // questers.
        if (qData.getWin() < 20) {
            return 20 - qData.getWin();
        }

        // The int mul has been lowered by one, should face special opps more
        // frequently.
        final int challengesPlayed = qData.getChallengesPlayed();
        int mul = 5;

        if (qData.getInventory().hasItem("Zeppelin")) {
            mul = 3;
        } else if (qData.getInventory().hasItem("Map")) {
            mul = 4;
        }

        final int delta = (challengesPlayed * mul) - qData.getWin();

        return (delta > 0) ? delta : 0;
    }
}
