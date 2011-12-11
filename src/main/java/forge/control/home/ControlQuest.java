package forge.control.home;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.gui.deckeditor.DeckEditorQuest;
import forge.gui.deckeditor.DeckEditorShop;
import forge.quest.data.QuestData;
import forge.quest.data.QuestUtil;
import forge.quest.gui.QuestFrame;
import forge.quest.gui.bazaar.QuestBazaarPanel;
import forge.view.GuiTopLevel;
import forge.view.home.ViewQuest;

/** 
 * Controls logic and listeners for Quest mode in home screen.
 * 
 */
public class ControlQuest {
    private ViewQuest view;

    /**
     * Controls logic and listeners for quest mode in home screen.
     * 
     * @param v0 &emsp; ViewQuest
     */
    public ControlQuest(ViewQuest v0) {
        this.view = v0;

        updateDeckList();
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

    /** */
    public void showCardShop() {
        final Command exit = new Command() {
            private static final long serialVersionUID = 8567193482568076362L;

            @Override
            public void execute() {
                // saves all deck data
                AllZone.getQuestData().saveData();
            }
        };

        DeckEditorShop g = new DeckEditorShop(AllZone.getQuestData());
        g.show(exit);
        g.setVisible(true);
    }

    /** */
    // Since QuestBazaarPanel is not in a JFrame for some reason, one
    // must be created here.  Later, this will be integrated into the
    // top level UI.  Doublestrike 11-12-11.
    public void showBazaar() {
        QuestFrame f = new QuestFrame();
        f.getContentPane().add(new QuestBazaarPanel(f));
        f.setVisible(true);

        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                AllZone.getQuestData().saveData();
            }
        });
    } // card shop button

    /**
     * The actuator for new quests.
     */
    public void newQuest() {
        int difficulty = 0;
        QuestData questData = AllZone.getQuestData();

        final String mode = view.getRadFantasy().isSelected() ? forge.quest.data.QuestData.FANTASY
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
                    "Please select a difficulty.",
                    "New Quest: Difficulty?", JOptionPane.ERROR_MESSAGE);
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
    public void start() {
        QuestData questData = AllZone.getQuestData();

        Constant.Runtime.HUMAN_DECK[0] = questData.getDeck((String) view.getLstDeckChooser().getSelectedValue());
        Constant.Runtime.COMPUTER_DECK[0] = view.getSelectedOpponent().getEvent().getEventDeck();

        AllZone.setQuestEvent(view.getSelectedOpponent().getEvent());

        GuiTopLevel g = (GuiTopLevel) AllZone.getDisplay();
        g.getController().changeState(1);
        g.getController().getMatchController().initMatch();

        AllZone.getGameAction().newGame(
                Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0],
                QuestUtil.getHumanStartingCards(questData),
                QuestUtil.getComputerStartingCards(questData),
                questData.getLife(), 20, null);
    }
}
