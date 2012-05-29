package forge.gui.home.quest;

import java.io.File;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.game.GameNew;
import forge.game.GameType;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorQuestCardShop;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FTextArea;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestMode;
import forge.quest.QuestUtil;
import forge.quest.bazaar.QuestItemType;
import forge.quest.bazaar.QuestPetController;
import forge.quest.data.QuestAchievements;
import forge.quest.data.QuestAssets;
import forge.quest.data.QuestPreferences.QPref;

/** 
 * Utilities for the quest submenu, all over the MVC spectrum.
 * If a piece of code can be reused, it's dumped here.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public class SSubmenuQuestUtil {
    private static SelectablePanel selectedOpponent;

    /**
     * <p>
     * nextChallengeInWins.
     * </p>
     * 
     * @return a int.
     */
    public static int nextChallengeInWins() {
        final QuestController qData = AllZone.getQuest();
        final int challengesPlayed = qData.getAchievements().getChallengesPlayed();

        int mul = 5;
        if (qData.getAssets().hasItem(QuestItemType.ZEPPELIN)) {
            mul = 3;
        } else if (qData.getAssets().hasItem(QuestItemType.MAP)) {
            mul = 4;
        }

        final int wins = qData.getAchievements().getWin();
        final int delta = (wins < 20 ? 20 - wins : (challengesPlayed * mul) - wins);

        return (delta > 0) ? delta : 0;
    }

    private static void updatePlantAndPetForView(final IStatsAndPet view, final QuestController qCtrl) {
        for (int iSlot = 0; iSlot < QuestController.MAX_PET_SLOTS; iSlot++) {
            final List<QuestPetController> petList = qCtrl.getPetsStorage().getAvaliablePets(iSlot, qCtrl.getAssets());
            final String currentPetName = qCtrl.getSelectedPet(iSlot);

            if (iSlot == 0) { // Plant visiblity
                if (petList.isEmpty()) {
                    view.getCbPlant().setVisible(false);
                }
                else {
                    view.getCbPlant().setVisible(true);
                    view.getCbPlant().setSelected(currentPetName != null);
                }
            }
            if (iSlot == 1) {
                view.getCbxPet().removeAllItems();

                // Pet list visibility
                if (petList.size() > 0) {
                    view.getCbxPet().setVisible(true);
                    view.getCbxPet().addItem("Don't summon a pet");

                    for (final QuestPetController pet : petList) {
                        String name = "Summon " + pet.getName();
                        view.getCbxPet().addItem(name);
                        if (pet.getName().equals(currentPetName)) {
                            view.getCbxPet().setSelectedItem(name);
                        }
                    }
                } else {
                    view.getCbxPet().setVisible(false);
                }
            }
        }

        if (view.equals(VSubmenuChallenges.SINGLETON_INSTANCE)) {
            view.getLblZep().setVisible(qCtrl.getAssets().hasItem(QuestItemType.ZEPPELIN));
            view.getLblZep().setEnabled(qCtrl.getAssets().getItemLevel(
                    QuestItemType.ZEPPELIN) == 2 ? false : true);
        }
        else {
            view.getLblZep().setVisible(false);
        }
    }

    /** Updates stats, pets panels for both duels and challenges. */
    public static void updateStatsAndPet() {
        final QuestController qCtrl = AllZone.getQuest();
        final QuestAchievements qA = qCtrl.getAchievements();
        final QuestAssets qS = qCtrl.getAssets();

        if (qA == null) { return; }

        final IStatsAndPet[] viewsToUpdate = new IStatsAndPet[] {
            VSubmenuDuels.SINGLETON_INSTANCE,
            VSubmenuChallenges.SINGLETON_INSTANCE
        };

        for (final IStatsAndPet view : viewsToUpdate) {
            // Fantasy UI display
            view.getLblNextChallengeInWins().setVisible(true);
            view.getBtnBazaar().setVisible(true);
            view.getLblLife().setVisible(true);

            // Stats panel
            view.getLblCredits().setText("Credits: " + qS.getCredits());
            view.getLblLife().setText("Life: " + qS.getLife(qCtrl.getMode()));
            view.getLblWins().setText("Wins: " + qA.getWin());
            view.getLblLosses().setText("Losses: " + qA.getLost());
            view.updateCurrentDeckStatus();

            final int num = SSubmenuQuestUtil.nextChallengeInWins();
            if (num == 0) {
                view.getLblNextChallengeInWins().setText("Next challenge available now.");
            }
            else {
                view.getLblNextChallengeInWins().setText("Next challenge available in " + num + " wins.");
            }

            view.getLblWinStreak().setText(
                    "Win streak: " + qA.getWinStreakCurrent()
                    + " (Best:" + qA.getWinStreakBest() + ")");

            // Start panel: pet, plant, zep.
            if (qCtrl.getMode() == QuestMode.Fantasy) {
                updatePlantAndPetForView(view, qCtrl);
            }
            else {
                // Classic mode display changes
                view.getCbxPet().setVisible(false);
                view.getCbPlant().setVisible(false);
                view.getLblZep().setVisible(false);
                view.getLblNextChallengeInWins().setVisible(false);
                view.getBtnBazaar().setVisible(false);
                view.getLblLife().setVisible(false);
            }
        }
    }

    /** @return {@link forge.view.home.ViewQuest.SelectablePanel} */
    public static SelectablePanel getSelectedOpponent() {
        return selectedOpponent;
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getCurrentDeck() {
        Deck d = null;

        if (AllZone.getQuest().getAssets() != null) {
            d = AllZone.getQuest().getMyDecks().get(
                Singletons.getModel().getQuestPreferences().getPreference(QPref.CURRENT_DECK));
        }

        return d;
    }

    /** */
    public static void showSpellShop() {
        CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(
                new CEditorQuestCardShop(AllZone.getQuest()));
        FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_QUEST);
    }

    /** */
    public static void showBazaar() {
        Singletons.getControl().changeState(FControl.QUEST_BAZAAR);
        Singletons.getView().getFrame().validate();
    }

    /** */
    public static void startGame() {
        final QuestController qData = AllZone.getQuest();
        final QuestEvent event = selectedOpponent.getEvent();

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
                Constant.Runtime.HUMAN_DECK[0] = SSubmenuQuestUtil.getCurrentDeck();
                Constant.Runtime.COMPUTER_DECK[0] = event.getEventDeck();
                Constant.Runtime.setGameType(GameType.Quest);

                qData.getChallengesManager().randomizeOpponents();
                qData.getDuelsManager().randomizeOpponents();
                qData.setCurrentEvent(event);
                qData.save();

                if (qData.getMode() == QuestMode.Fantasy) {
                    int lifeAI = 20;
                    int baseLifeHuman = qData.getAssets().getLife(qData.getMode());
                    int extraLifeHuman = 0;

                    if (selectedOpponent.getEvent() instanceof QuestEventChallenge) {
                        lifeAI = ((QuestEventChallenge) event).getAILife();

                        if (qData.getAssets().hasItem(QuestItemType.ZEPPELIN)) {
                            extraLifeHuman = 3;
                        }
                    }

                    GameNew.newGame(
                            Constant.Runtime.HUMAN_DECK[0],
                            Constant.Runtime.COMPUTER_DECK[0],
                            QuestUtil.getHumanStartingCards(qData, event),
                            QuestUtil.getComputerStartingCards(event),
                            baseLifeHuman + extraLifeHuman,
                            lifeAI,
                            event.getIconFilename());
                } // End isFantasy
                else {
                    GameNew.newGame(SSubmenuQuestUtil.getCurrentDeck(), event.getEventDeck());
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

    /** Selectable panels for duels and challenges. */
    @SuppressWarnings("serial")
    public static class SelectablePanel extends FPanel {
        private final QuestEvent event;

        /** @param e0 &emsp; QuestEvent */
        public SelectablePanel(final QuestEvent e0) {
            super();
            this.event = e0;
            this.setSelectable(true);
            this.setHoverable(true);
            this.setLayout(new MigLayout("insets 0, gap 0"));

            this.setCommand(new Command() {
                @Override
                public void execute() {
                    if (selectedOpponent != null) {
                        selectedOpponent.setSelected(false);
                    }

                    if (VSubmenuDuels.SINGLETON_INSTANCE.getPnlDuels().isShowing() && getCurrentDeck() != null) {
                        VSubmenuDuels.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
                        VSubmenuChallenges.SINGLETON_INSTANCE.getBtnStart().setEnabled(false);
                    }
                    else if (VSubmenuChallenges.SINGLETON_INSTANCE.getPnlChallenges().isShowing() &&  getCurrentDeck() != null) {
                        VSubmenuDuels.SINGLETON_INSTANCE.getBtnStart().setEnabled(false);
                        VSubmenuChallenges.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
                    }

                    selectedOpponent = SSubmenuQuestUtil.SelectablePanel.this;
                }
            });

            // Icon
            final File base = ForgeProps.getFile(NewConstants.IMAGE_ICON);
            final File file = new File(base, event.getIconFilename());

            final FLabel lblIcon = new FLabel.Builder().iconScaleFactor(1).build();
            if (!file.exists()) {
                lblIcon.setIcon(FSkin.getIcon(FSkin.InterfaceIcons.ICO_UNKNOWN));
            }
            else {
                lblIcon.setIcon(new ImageIcon(file.toString()));
            }
            this.add(lblIcon, "h 60px!, w 60px!, gap 10px 10px 10px 0, span 1 2");

            // Name
            final FLabel lblName = new FLabel.Builder().hoverable(false).build();
            if (event instanceof QuestEventChallenge) {
                lblName.setText(event.getTitle() + ": "
                        + StringUtils.capitalize(event.getDifficulty())
                        + (((QuestEventChallenge) event).isRepeatable() ? ", Repeatable" : ""));
            }
            else {
                lblName.setText(event.getTitle() + ": "
                        + StringUtils.capitalize(event.getDifficulty()));
            }
            this.add(lblName, "h 31px!, gap 0 0 10px 5px, wrap");

            // Description
            final FTextArea tarDesc = new FTextArea();
            tarDesc.setText(event.getDescription());

            tarDesc.setFont(FSkin.getItalicFont(12));
            this.add(tarDesc, "w 80%!, h 30px!");
       }

        /** @return QuestEvent */
        public QuestEvent getEvent() {
            return event;
        }
    }
}

