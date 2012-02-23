package forge.gui.home.constructed;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;
import javax.swing.SwingUtilities;

import forge.Command;
import forge.Singletons;
import forge.gui.home.ICSubmenu;
import forge.quest.data.QuestEvent;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum CSubmenuQuestEvents implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getMenuCommand()
     */
    @Override
    public Command getMenuCommand() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#initialize()
     */
    @Override
    public void initialize() {
        VSubmenuQuestEvents.SINGLETON_INSTANCE.populate();
        CSubmenuQuestEvents.SINGLETON_INSTANCE.update();

        for (JList lst : VSubmenuQuestEvents.SINGLETON_INSTANCE.getLists()) {
            SubmenuConstructedUtil.randomSelect(lst);

            lst.addMouseListener(new MouseAdapter() { @Override
                public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                        final String deckName = ((JList) e.getSource()).getSelectedValue().toString();
                        Singletons.getModel().getQuestEventManager().getEvent(deckName);
                        SubmenuConstructedUtil.showDecklist(Singletons.getModel().getQuestEventManager().getEvent(deckName).getEventDeck());
             } } });
        }

        VSubmenuQuestEvents.SINGLETON_INSTANCE.getBtnStart().addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(final MouseEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                SubmenuConstructedUtil.startGame(VSubmenuQuestEvents.SINGLETON_INSTANCE.getLists());
                            }
                        });
                    }
                });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final List<String> eventNames = new ArrayList<String>();

        for (final QuestEvent e : Singletons.getModel().getQuestEventManager().getAllChallenges()) {
            eventNames.add(e.getEventDeck().getName());
        }

        for (final QuestEvent e : Singletons.getModel().getQuestEventManager().getAllDuels()) {
            eventNames.add(e.getEventDeck().getName());
        }

        for (JList lst : VSubmenuQuestEvents.SINGLETON_INSTANCE.getLists()) {
            lst.setListData(SubmenuConstructedUtil.oa2sa(eventNames.toArray()));
        }
    }
}
