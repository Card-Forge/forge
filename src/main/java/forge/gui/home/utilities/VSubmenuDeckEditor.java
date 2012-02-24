package forge.gui.home.utilities;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.view.toolbox.FLabel;

/** 
 * Singleton instance of "Colors" submenu in "Constructed" group.
 *
 */
public enum VSubmenuDeckEditor implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /** */
    private final JPanel pnl = new JPanel();

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        pnl.setLayout(new MigLayout("insets 0, gap 0, align center"));
        pnl.setOpaque(false);

        pnl.add(new FLabel.Builder().text("Open Deck Editor").opaque(true)
                .hoverable(true).cmdClick(CSubmenuDeckEditor.SINGLETON_INSTANCE.getMenuCommand())
                .fontScaleAuto(false).fontSize(16).build(), "w 200px!, h 40px!");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroup() {
        return EMenuGroup.CONSTRUCTED;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getPanel()
     */
    @Override
    public JPanel getPanel() {
        return pnl;
    }
}
