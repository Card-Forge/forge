package forge.gui.home.quest;

import java.io.File;
import java.util.Set;

import javax.swing.ImageIcon;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.Deck;
import forge.game.GameNew;
import forge.game.GameType;
import forge.gui.deckeditor.QuestCardShop;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestChallenge;
import forge.quest.data.QuestData;
import forge.quest.data.QuestDataIO;
import forge.quest.data.QuestEvent;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.data.QuestUtil;
import forge.quest.data.item.QuestItemZeppelin;
import forge.quest.data.pet.QuestPetAbstract;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FOverlay;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FSkin;
import forge.view.toolbox.FTextArea;

/** 
 * Utilities for the quest submenu, all over the MVC spectrum.
 * If a piece of code can be reused, it's dumped here.
 */
public class SubmenuQuestUtil {
    private static SelectablePanel selectedOpponent;
    private static Deck currentDeck;

    /**
     * <p>
     * nextChallengeInWins.
     * </p>
     * 
     * @return a int.
     */
    public static int nextChallengeInWins() {
        final QuestData qData = AllZone.getQuestData();
        final int challengesPlayed = qData.getChallengesPlayed();

        int mul = 5;
        if (qData.getInventory().hasItem("Zeppelin")) {
            mul = 3;
        } else if (qData.getInventory().hasItem("Map")) {
            mul = 4;
        }

        final int delta = (qData.getWin() < 20
                ? 20 - qData.getWin()
                : (challengesPlayed * mul) - qData.getWin());

        return (delta > 0) ? delta : 0;
    }

    /** Updates stats, pets panels for both duels and challenges. */
    public static void updateStatsAndPet() {
        final QuestData qData;
        final VSubmenuDuels view = VSubmenuDuels.SINGLETON_INSTANCE;

        ////////// TODO - THIS SHOULD NOT BE HERE AND WILL BE MOVED EVENTUALLY.
        if (AllZone.getQuestData() == null) {
            final String questname = Singletons.getModel()
                    .getQuestPreferences().getPreference(QPref.CURRENT_QUEST);

            qData = QuestDataIO.loadData(new File(
                    ForgeProps.getFile(NewConstants.Quest.DATA_DIR) + questname + ".dat"));
            AllZone.setQuestData(qData);
        }
        else {
            qData = AllZone.getQuestData();
        }
        ////////////////////////////////////////////////////////////////////////

        // Stats panel
        view.getLblCredits().setText("Credits: " + qData.getCredits());
        view.getLblLife().setText("Life: " + qData.getLife());
        view.getLblWins().setText("Wins: " + qData.getWin());
        view.getLblLosses().setText("Losses: " + qData.getLost());
        view.setCurrentDeckStatus();

        final int num = SubmenuQuestUtil.nextChallengeInWins();
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
        if (qData.getMode().equals(QuestData.FANTASY)) {
            final Set<String> petList = qData.getPetManager().getAvailablePetNames();
            final QuestPetAbstract currentPet = qData.getPetManager().getSelectedPet();

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
            if (qData.getPetManager().getPlant().getLevel() == 0) {
                view.getCbPlant().setVisible(false);
            }
            else {
                view.getCbPlant().setVisible(true);
                view.getCbPlant().setSelected(qData.getPetManager().shouldPlantBeUsed());
            }

            // Zeppelin visibility
            final QuestItemZeppelin zeppelin = (QuestItemZeppelin) qData.getInventory().getItem("Zeppelin");
            view.getCbZep().setVisible(zeppelin.hasBeenUsed());
        }
        else {
            view.getCbxPet().setVisible(false);
            view.getCbPlant().setVisible(false);
            view.getCbZep().setVisible(false);
        }
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
                    if (selectedOpponent != null) { selectedOpponent.setSelected(false); }
                    else { VSubmenuDuels.SINGLETON_INSTANCE.getBtnStart().setEnabled(true); }

                    selectedOpponent = SubmenuQuestUtil.SelectablePanel.this;
                }
            });

            // Icon
            final File base = ForgeProps.getFile(NewConstants.IMAGE_ICON);
            final File file = new File(base, event.getIconFilename());

            final FLabel lblIcon = new FLabel.Builder().iconScaleFactor(1).build();
            if (!file.exists()) {
                lblIcon.setIcon(FSkin.getIcon(FSkin.ForgeIcons.ICO_UNKNOWN));
            }
            else {
                lblIcon.setIcon(new ImageIcon(file.toString()));
            }
            this.add(lblIcon, "h 60px!, w 60px!, gap 10px 10px 10px 0, span 1 2");

            // Name
            final FLabel lblName = new FLabel.Builder()
                    .text(event.getTitle() + ": " + event.getDifficulty()).hoverable(false).build();
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

    /** @return {@link forge.view.home.ViewQuest.SelectablePanel} */
    public static SelectablePanel getSelectedOpponent() {
        return selectedOpponent;
    }

    /** @param deck0 &emsp; {@link forge.deck.Deck} */
    public static void setCurrentDeck(final Deck deck0) {
        currentDeck = deck0;
    }

    /** @return {@link forge.deck.Deck} */
    public static Deck getCurrentDeck() {
        return currentDeck;
    }

    /** */
    @SuppressWarnings("serial")
    public static void showSpellShop() {
        final Command exit = new Command() {
            @Override
            public void execute() {
                AllZone.getQuestData().saveData();
                updateStatsAndPet();
            }
        };

        QuestCardShop g = new QuestCardShop(AllZone.getQuestData());
        g.show(exit);
        g.setVisible(true);
    }

    /** */
    public static void showBazaar() {
        Singletons.getControl().changeState(FControl.QUEST_BAZAAR);
        Singletons.getView().getFrame().validate();
    }

    /** */
    public static void startGame() {
        final QuestData qData = AllZone.getQuestData();
        final QuestEvent event = selectedOpponent.getEvent();
        final FOverlay overlay = Singletons.getView().getOverlay();
        final FPanel pnl = new FPanel();

        // Overlay layout
        overlay.setLayout(new MigLayout("insets 0, gap 0, align center"));

        pnl.setLayout(new MigLayout("insets 0, gap 0, ax center, wrap"));
        pnl.setBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE));
        pnl.add(new FLabel.Builder().icon(FSkin.getIcon(FSkin.ForgeIcons.ICO_LOGO)).build(),
                "h 200px!, align center");
        pnl.add(new FLabel.Builder().text("Loading new game...")
                .fontScaleAuto(false).fontSize(22).build(), "h 40px!, align center");

        overlay.add(pnl, "h 300px!, w 400px!");
        overlay.showOverlay();

        // Logic
        final QuestItemZeppelin zeppelin = (QuestItemZeppelin) qData.getInventory().getItem("Zeppelin");
        zeppelin.setZeppelinUsed(false);
        qData.randomizeOpponents();

        Constant.Runtime.HUMAN_DECK[0] = currentDeck;
        Constant.Runtime.COMPUTER_DECK[0] = event.getEventDeck();
        Constant.Quest.OPP_ICON_NAME[0] = event.getIconFilename();
        Constant.Runtime.setGameType(GameType.Quest);
        AllZone.setQuestEvent(event);
        qData.saveData();

        if (qData.isFantasy()) {
            Constant.Quest.FANTASY_QUEST[0] = true;
            int lifeAI = 20;
            int lifeHuman = 20;

            if (selectedOpponent.getEvent().getEventType().equals("challenge")) {
                int extraLife = 0;

                if (qData.getInventory().getItemLevel("Gear") == 2) {
                    extraLife = 3;
                }
                lifeAI = ((QuestChallenge) event).getAILife();
                lifeHuman = qData.getLife() + extraLife;
            }

            GameNew.newGame(
                    Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0],
                    QuestUtil.getHumanStartingCards(qData),
                    QuestUtil.getComputerStartingCards(qData),
                    lifeHuman, lifeAI);
        } // End isFantasy
        else {
            GameNew.newGame(currentDeck, event.getEventDeck());
        }

        // Start transisiton to match UI.
        overlay.hideOverlay();
    }
}
