package forge.gui.home.quest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import forge.Command;
import forge.Singletons;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.CMainMenu;
import forge.gui.home.quest.SSubmenuQuestUtil.SelectablePanel;
import forge.quest.QuestController;
import forge.quest.QuestEventDuel;
import forge.quest.bazaar.QuestPetController;

/** 
 * Controls the quest duels submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuDuels implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#initialize()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        final VSubmenuDuels view = VSubmenuDuels.SINGLETON_INSTANCE;

        view.getBtnSpellShop().setCommand(
                new Command() { @Override
                    public void execute() { SSubmenuQuestUtil.showSpellShop(); } });

        view.getBtnBazaar().setCommand(
                new Command() { @Override
                    public void execute() { SSubmenuQuestUtil.showBazaar(); } });

        view.getBtnStart().addActionListener(
                new ActionListener() { @Override
            public void actionPerformed(final ActionEvent e) { SSubmenuQuestUtil.startGame(); } });

        view.getBtnCurrentDeck().setCommand(
                new Command() { @Override
                    public void execute() {
                        CMainMenu.SINGLETON_INSTANCE.itemClick(EDocID.HOME_QUESTDECKS);
                    }
                });

        final QuestController quest = Singletons.getModel().getQuest();
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

        final VSubmenuDuels view = VSubmenuDuels.SINGLETON_INSTANCE;

        if (Singletons.getModel().getQuest().getAchievements() != null) {
            view.getLblTitle().setText("Duels: " + Singletons.getModel().getQuest().getRank());

            view.getPnlDuels().removeAll();
            final List<QuestEventDuel> duels = Singletons.getModel().getQuest().getDuelsManager().generateDuels();

            for (final QuestEventDuel d : duels) {
                final SelectablePanel temp = new SelectablePanel(d);
                view.getPnlDuels().add(temp, "w 96%!, h 86px!, gap 2% 0 5px 5px");
            }
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @SuppressWarnings("serial")
    @Override
    public Command getCommandOnSelect() {
        final QuestController qc = Singletons.getModel().getQuest();
        return new Command() {
            @Override
            public void execute() {
                if (qc.getAchievements() == null) {
                    CMainMenu.SINGLETON_INSTANCE.itemClick(EDocID.HOME_QUESTDATA);
                }
            }
        };
    }
}
