package forge.gui.home.constructed;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;

import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.view.home.StartButton;

/** 
 * Singleton instance of "Colors" submenu in "Constructed" group.
 *
 */
public enum VSubmenuQuestEvents implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /** */
    private final JPanel pnl            = new JPanel();
    private final StartButton btnStart  = new StartButton();
    private final List<JList> allLists  = new ArrayList<JList>();

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        pnl.setName(ESubmenuConstructedTypes.QUESTEVENTS.toString());
        final List<JList> generatedLists =
                SubmenuConstructedUtil.populateConstructedSubmenuView(pnl, btnStart);

        allLists.clear();
        for (final JList lst : generatedLists) {
            lst.setName(ESubmenuConstructedTypes.QUESTEVENTS.toString());
            allLists.add(lst);
        }
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroup() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getPanel()
     */
    @Override
    public JPanel getPanel() {
        return pnl;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    /** @return {@link java.util.List}<{@link javax.swing.JList}> */
    public List<JList> getLists() {
        return allLists;
    }
}
