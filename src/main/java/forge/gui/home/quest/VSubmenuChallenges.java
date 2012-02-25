package forge.gui.home.quest;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FScrollPane;

/**  */
public enum VSubmenuChallenges implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    //========== INSTANTIATION
    private final JPanel pnl        = new JPanel();
    private final JPanel pnlChallenges = new JPanel();

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getPanel()
     */
    @Override
    public JPanel getPanel() {
        return pnl;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroup() {
        return EMenuGroup.QUEST;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        // Note: "Challenges" submenu uses the instances of
        // title, stats, and start panels from "Duels" submenu.
        final FPanel pnlTitle = VSubmenuDuels.SINGLETON_INSTANCE.getPnlTitle();
        final JPanel pnlStats = VSubmenuDuels.SINGLETON_INSTANCE.getPnlStats();
        final JPanel pnlStart = VSubmenuDuels.SINGLETON_INSTANCE.getPnlStart();
        VSubmenuDuels.SINGLETON_INSTANCE.getBtnStart().setEnabled(false);
        ////

        final FScrollPane scrChallenges = new FScrollPane(pnlChallenges,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrChallenges.setBorder(null);
        pnlChallenges.setOpaque(false);
        pnlChallenges.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        pnl.setOpaque(false);
        pnl.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        pnl.add(pnlTitle, "w 94%!, h 30px!, gap 3% 0 15px 15px");
        pnl.add(pnlStats, "w 94%!, gap 3% 0 0 20px");
        pnl.add(scrChallenges, "w 94%!, pushy, growy, gap 3% 0 0 0");
        pnl.add(pnlStart, "w 94%, gap 3% 0 15px 5%");
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPnlChallenges() {
        return pnlChallenges;
    }
}
