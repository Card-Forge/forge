package forge.gui.home.constructed;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;
import javax.swing.SwingUtilities;

import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.gui.home.ICSubmenu;
import forge.util.IFolderMap;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum CSubmenuCustom implements ICSubmenu {
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
        VSubmenuCustom.SINGLETON_INSTANCE.populate();
        CSubmenuCustom.SINGLETON_INSTANCE.update();

        for (JList lst : VSubmenuCustom.SINGLETON_INSTANCE.getLists()) {
            SubmenuConstructedUtil.randomSelect(lst);

            lst.addMouseListener(new MouseAdapter() { @Override
                public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) {
                    final String deckName = ((JList) e.getSource()).getSelectedValue().toString();
                    SubmenuConstructedUtil.showDecklist(Singletons.getModel().getDecks().getConstructed().get(deckName));
            } } });
        }

        VSubmenuCustom.SINGLETON_INSTANCE.getBtnStart().addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(final MouseEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                SubmenuConstructedUtil.startGame(VSubmenuCustom.SINGLETON_INSTANCE.getLists());
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
        final List<String> customNames = new ArrayList<String>();
        final IFolderMap<Deck> allDecks = Singletons.getModel().getDecks().getConstructed();
        for (final Deck d : allDecks) { customNames.add(d.getName()); }

        for (JList lst : VSubmenuCustom.SINGLETON_INSTANCE.getLists()) {
            lst.setListData(SubmenuConstructedUtil.oa2sa(customNames.toArray()));
        }
    }
}
