package forge.gui.home.quest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import forge.AllZone;
import forge.Command;
import forge.gui.home.EMenuItem;
import forge.gui.home.ICSubmenu;
import forge.gui.home.VHomeUI;
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
public enum CSubmenuDuels implements ICSubmenu {
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
            public void execute() {
                if (qc.getAchievements() == null) {
                    VHomeUI.SINGLETON_INSTANCE.itemClick(EMenuItem.QUEST_DATA);
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
        final VSubmenuDuels view = VSubmenuDuels.SINGLETON_INSTANCE;
        view.populate();
        CSubmenuDuels.SINGLETON_INSTANCE.update();

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
                        VHomeUI.SINGLETON_INSTANCE.itemClick(EMenuItem.QUEST_DECKS);
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

        final VSubmenuDuels view = VSubmenuDuels.SINGLETON_INSTANCE;

        if (AllZone.getQuest().getAchievements() != null) {
            view.getLblTitle().setText("Duels: " + AllZone.getQuest().getRank());

            view.getPnlDuels().removeAll();
            final List<QuestEventDuel> duels = AllZone.getQuest().getDuelsManager().generateDuels();

            for (final QuestEventDuel d : duels) {
                final SelectablePanel temp = new SelectablePanel(d);
                view.getPnlDuels().add(temp, "w 96%!, h 86px!, gap 2% 0 5px 5px");
            }
        }
    }
}
