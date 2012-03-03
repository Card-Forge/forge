package forge.gui.home.quest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import forge.AllZone;
import forge.Command;
import forge.Singletons;
import forge.gui.home.EMenuItem;
import forge.gui.home.ICSubmenu;
import forge.gui.home.quest.SubmenuQuestUtil.SelectablePanel;
import forge.quest.data.QuestDuel;
import forge.view.ViewHomeUI;

/** 
 * TODO: Write javadoc for this type.
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
        return new Command() {
            public void execute() {
                if (AllZone.getQuestData() == null) {
                    ViewHomeUI.SINGLETON_INSTANCE.itemClick(EMenuItem.QUEST_DATA);
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
        VSubmenuDuels.SINGLETON_INSTANCE.populate();
        CSubmenuDuels.SINGLETON_INSTANCE.update();

        VSubmenuDuels.SINGLETON_INSTANCE.getBtnSpellShop().setCommand(
                new Command() { @Override
                    public void execute() { SubmenuQuestUtil.showSpellShop(); } });

        VSubmenuDuels.SINGLETON_INSTANCE.getBtnBazaar().setCommand(
                new Command() { @Override
                    public void execute() { SubmenuQuestUtil.showBazaar(); } });

        VSubmenuDuels.SINGLETON_INSTANCE.getBtnStart().addActionListener(
                new ActionListener() { @Override
            public void actionPerformed(final ActionEvent e) { SubmenuQuestUtil.startGame(); } });

        VSubmenuDuels.SINGLETON_INSTANCE.getBtnCurrentDeck().setCommand(
                new Command() { @Override
                    public void execute() {
                        ViewHomeUI.SINGLETON_INSTANCE.itemClick(EMenuItem.QUEST_DECKS);
                    }
                });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        SubmenuQuestUtil.updateStatsAndPet();

        final VSubmenuDuels view = VSubmenuDuels.SINGLETON_INSTANCE;

        if (AllZone.getQuestData() != null) {
            view.getLblTitle().setText("Duels: " + AllZone.getQuestData().getRank());

            view.getPnlDuels().removeAll();
            final List<QuestDuel> duels =
                    Singletons.getModel().getQuestEventManager().generateDuels();

            for (final QuestDuel d : duels) {
                final SelectablePanel temp = new SelectablePanel(d);
                view.getPnlDuels().add(temp, "w 96%!, h 86px!, gap 2% 0 5px 5px");
            }
        }
    }
}
