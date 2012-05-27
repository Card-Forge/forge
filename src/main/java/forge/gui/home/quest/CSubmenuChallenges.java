package forge.gui.home.quest;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import forge.AllZone;
import forge.Command;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.CMainMenu;
import forge.gui.home.ICSubmenu;
import forge.gui.home.quest.SSubmenuQuestUtil.SelectablePanel;
import forge.gui.toolbox.FLabel;
import forge.quest.QuestController;
import forge.quest.QuestEventChallenge;
import forge.quest.bazaar.QuestPetController;

/** 
 * Controls the quest challenges submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuChallenges implements ICSubmenu, ICDoc {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getCommand()
     */
    @SuppressWarnings("serial")
    @Override
    public Command getMenuCommand() {
        final QuestController qc = AllZone.getQuest();
        return new Command() {
            @Override
            public void execute() {
                if (qc.getAchievements() == null) {
                    CMainMenu.SINGLETON_INSTANCE.itemClick(EDocID.HOME_QUESTDATA);
                }
            }
        };
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#initialize()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        final VSubmenuChallenges view = VSubmenuChallenges.SINGLETON_INSTANCE;

        view.getBtnSpellShop().setCommand(
                new Command() { @Override
                    public void execute() { SSubmenuQuestUtil.showSpellShop(); } });

        view.getBtnBazaar().setCommand(
                new Command() { @Override
                    public void execute() { SSubmenuQuestUtil.showBazaar(); } });

        view.getBtnStart().addActionListener(
                new ActionListener() { @Override
            public void actionPerformed(final ActionEvent e) { SSubmenuQuestUtil.startGame(); } });

        ((FLabel) view.getLblZep()).setCommand(
                new Command() {
                    @Override
                    public void execute() {
                        int todo = 5;
                        //AllZone.getQuest().getAchievements().setCurrentChallenges(null);
                        //AllZone.getQuest().getAssets().setItemLevel(QuestItemType.ZEPPELIN, 2);
                        //update();
                    }
                });

        view.getBtnCurrentDeck().setCommand(
                new Command() { @Override
                    public void execute() {
                        CMainMenu.SINGLETON_INSTANCE.itemClick(EDocID.HOME_QUESTDECKS);
                    }
                });

        final QuestController quest = AllZone.getQuest();
        view.getCbPlant().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                quest.selectPet(0, view.getCbPlant().isSelected() ? "Plant" : null);
            }
        });

        view.getCbxPet().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final int slot = 1;
                final int index = view.getCbxPet().getSelectedIndex();
                List<QuestPetController> pets = quest.getPetsStorage().getAvaliablePets(slot, quest.getAssets());
                String petName = index <= 0 || index > pets.size() ? null : pets.get(index - 1).getName();
                quest.selectPet(slot, petName);
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        SSubmenuQuestUtil.updateStatsAndPet();

        final VSubmenuChallenges view = VSubmenuChallenges.SINGLETON_INSTANCE;
        final QuestController qCtrl = AllZone.getQuest();

        if (qCtrl.getAchievements() != null) {
            view.getLblTitle().setText("Challenges: " + qCtrl.getRank());

            view.getPnlChallenges().removeAll();
            final List<QuestEventChallenge> challenges = qCtrl.getChallengesManager().generateChallenges(qCtrl);

            for (final QuestEventChallenge c : challenges) {
                final SelectablePanel temp = new SelectablePanel(c);
                view.getPnlChallenges().add(temp, "w 96%!, h 86px!, gap 2% 0 5px 5px");
            }

            if (challenges.size() == 0) {
                final FLabel lbl = new FLabel.Builder()
                    .text(VSubmenuChallenges.SINGLETON_INSTANCE.getLblNextChallengeInWins().getText())
                    .fontAlign(SwingConstants.CENTER).build();
                lbl.setForeground(Color.red);
                lbl.setBackground(Color.white);
                lbl.setBorder(new EmptyBorder(10, 10, 10, 10));
                lbl.setOpaque(true);
                view.getPnlChallenges().add(lbl, "w 50%!, h 30px!, gap 25% 0 50px 0");
            }
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
