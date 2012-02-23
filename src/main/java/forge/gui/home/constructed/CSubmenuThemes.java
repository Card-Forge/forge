package forge.gui.home.constructed;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;
import javax.swing.SwingUtilities;

import forge.Command;
import forge.deck.generate.GenerateThemeDeck;
import forge.gui.home.ICSubmenu;
/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum CSubmenuThemes implements ICSubmenu {
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
        VSubmenuThemes.SINGLETON_INSTANCE.populate();
        CSubmenuThemes.SINGLETON_INSTANCE.update();

        for (final JList lst : VSubmenuThemes.SINGLETON_INSTANCE.getLists()) {
            SubmenuConstructedUtil.randomSelect(lst);
        }

        VSubmenuThemes.SINGLETON_INSTANCE.getBtnStart().addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(final MouseEvent e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                SubmenuConstructedUtil.startGame(VSubmenuThemes.SINGLETON_INSTANCE.getLists());
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
        final List<String> themeNames = new ArrayList<String>();
        for (final String s : GenerateThemeDeck.getThemeNames()) { themeNames.add(s); }

        for (JList lst : VSubmenuThemes.SINGLETON_INSTANCE.getLists()) {
            lst.setListData(SubmenuConstructedUtil.oa2sa(themeNames.toArray()));
        }
    }
}
