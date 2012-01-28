package forge.control.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.control.FControl;
import forge.deck.Deck;
import forge.game.GameType;
import forge.gui.deckeditor.DeckEditorQuest;
import forge.gui.deckeditor.DeckEditorShop;
import forge.quest.data.QuestChallenge;
import forge.quest.data.QuestData;
import forge.quest.data.QuestEvent;
import forge.quest.data.QuestUtil;
import forge.quest.data.item.QuestItemZeppelin;
import forge.view.GuiTopLevel;
import forge.view.home.ViewQuest;

/** 
 * Controls logic and listeners for Quest mode in home screen.
 * 
 */
public class ControlQuest {
    private ViewQuest view;
    private QuestEvent event;
    private final ActionListener actPetSelect, actPlantSelect;
    private final MouseAdapter madStartGame;

    /**
     * Controls logic and listeners for quest mode in home screen.
     * 
     * @param v0 &emsp; ViewQuest
     */
    public ControlQuest(ViewQuest v0) {
        this.view = v0;

        if (view.hasPreviousQuest()) {
            updateDeckList();
        }

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

        actPetSelect = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                if (view.getPetComboBox().getSelectedIndex() > 0) {
                    view.getQuestData().getPetManager().setSelectedPet(
                            (String) view.getPetComboBox().getSelectedItem());
                } else {
                    view.getQuestData().getPetManager().setSelectedPet(null);
                }
            }
        };

        actPlantSelect = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                view.getQuestData().getPetManager()
                        .setUsePlant(view.getPlantCheckBox().isSelected());
            }
        };

        addListeners();
    }

    private void addListeners() {
        if (view.hasPreviousQuest()) {
            view.getBtnStart().removeMouseListener(madStartGame);
            view.getBtnStart().addMouseListener(madStartGame);

            view.getPetComboBox().removeActionListener(actPetSelect);
            view.getPetComboBox().addActionListener(actPetSelect);

            view.getPlantCheckBox().removeActionListener(actPlantSelect);
            view.getPlantCheckBox().addActionListener(actPlantSelect);
        }
    }

    /** @return ViewQuest */
    public ViewQuest getView() {
        return view;
    }

    /** */
    private void updateDeckList() {
        view.getLstDeckChooser().setListData(AllZone.getQuestData().getDeckNames().toArray());
        view.getLstDeckChooser().setSelectedIndex(0);
    }

    /** */
    public void showDeckEditor() {
        final Command exit = new Command() {
            private static final long serialVersionUID = -5110231879431074581L;

            @Override
            public void execute() {
                // saves all deck data
                AllZone.getQuestData().saveData();
                updateDeckList();
            }
        };

        DeckEditorQuest g = new DeckEditorQuest(AllZone.getQuestData());
        g.show(exit);
        g.setVisible(true);
    }

    private void updateCredits() {
        view.getLblCredits().setText(Long.toString(view.getQuestData().getCredits()));
    }

    private void updateLife() {
        view.getLblLife().setText(Long.toString(view.getQuestData().getLife()));
    }

    /** */
    public void showCardShop() {
        final Command exit = new Command() {
            private static final long serialVersionUID = 8567193482568076362L;

            @Override
            public void execute() {
                // saves all deck data
                AllZone.getQuestData().saveData();
                updateCredits();
                updateLife();
            }
        };

        DeckEditorShop g = new DeckEditorShop(AllZone.getQuestData());
        g.show(exit);
        g.setVisible(true);
    }

    /** */
    public void showBazaar() {
        GuiTopLevel g = ((GuiTopLevel) AllZone.getDisplay());

        g.getController().changeState(FControl.QUEST_BAZAAR);
        g.validate();
    } // card shop button

    /**
     * The actuator for new quests.
     */
    public void newQuest() {
        int difficulty = 0;
        QuestData questData = new QuestData();

        final String mode = view.getRadFantasy().isSelected()
                ? forge.quest.data.QuestData.FANTASY
                : forge.quest.data.QuestData.REALISTIC;

        if (view.getRadEasy().isSelected()) {
            difficulty = 0;
        } else if (view.getRadMedium().isSelected()) {
            difficulty = 1;
        } else if (view.getRadHard().isSelected()) {
            difficulty = 2;
        } else if (view.getRadExpert().isSelected()) {
            difficulty = 3;
        } else {
            JOptionPane.showMessageDialog(null,
                    "This should not be happening!",
                    "New Quest: Difficulty Bug!?", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (questData.hasSaveFile()) {
            // this will overwrite your save file!
            final Object[] possibleValues = { "Yes", "No" };
            final Object choice = JOptionPane.showOptionDialog(null,
                    "Starting a new quest will overwrite your current quest. Continue?", "Start New Quest?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, possibleValues, possibleValues[1]);

            if (!choice.equals(0)) {
                return;
            }
        }

        // give the user a few cards to build a deck
        questData.newGame(difficulty, mode, view.getCbStandardStart().isSelected());

        questData.saveData();

        // set global variable
        AllZone.setQuestData(questData);
        view.getParentView().resetQuest();
    }

    /** */
    private void startGame() {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "ControlQuest() > startGame() must be accessed from outside the event dispatch thread.");
        }

        if (view.getLstDeckChooser().getSelectedIndex() == -1) {
            JOptionPane.showMessageDialog(null,
                    "A mysterious wall blocks your way."
                    + "\n\rAn unseen sepulchral voice booms:"
                    + "\n\r\"Entrance Forbidden Without A Deck\"",
                    "No deck", JOptionPane.ERROR_MESSAGE);
            return;
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

        event = view.getSelectedOpponent().getEvent();
        AllZone.setQuestEvent(event);
        Constant.Runtime.setGameType(GameType.Quest);
        final QuestItemZeppelin zeppelin = (QuestItemZeppelin) view.getQuestData().getInventory().getItem("Zeppelin");
        zeppelin.setZeppelinUsed(false);
        view.getQuestData().randomizeOpponents();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getBarProgress().increment();
            }
         });

        String deckname = (String) view.getLstDeckChooser().getSelectedValue();
        Constant.Runtime.HUMAN_DECK[0] = view.getQuestData().getDeck(deckname);
        Constant.Runtime.COMPUTER_DECK[0] = event.getEventDeck();
        final Deck humanDeck = view.getQuestData().getDeck(deckname);

        Constant.Runtime.HUMAN_DECK[0] = humanDeck;

        Constant.Quest.OPP_ICON_NAME[0] = event.getIcon();

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
                    setupChallenge(humanDeck);
                } else {
                    setupDuel(humanDeck);
                }
                view.getQuestData().saveData();
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
    final void setupDuel(final Deck humanDeck) {
        final Deck computer = event.getEventDeck();
        Constant.Runtime.COMPUTER_DECK[0] = computer;

        AllZone.getGameAction().newGame(
                Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0],
                QuestUtil.getHumanStartingCards(view.getQuestData()),
                QuestUtil.getComputerStartingCards(view.getQuestData()),
                view.getQuestData().getLife(), 20);
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

        if (view.getQuestData().getInventory().getItemLevel("Gear") == 2) {
            extraLife = 3;
        }

        AllZone.getGameAction().newGame(
                Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0],
                QuestUtil.getHumanStartingCards(view.getQuestData(), event),
                QuestUtil.getComputerStartingCards(view.getQuestData(), event),
                view.getQuestData().getLife() + extraLife, ((QuestChallenge) event).getAILife());

    }
}
