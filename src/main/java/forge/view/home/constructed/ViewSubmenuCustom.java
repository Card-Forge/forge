package forge.view.home.constructed;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Command;
import forge.Player;
import forge.control.home.IControlSubmenu;
import forge.control.home.constructed.ControlSubmenuCustom;
import forge.model.home.MenuGroup;
import forge.view.home.IViewSubmenu;
import forge.view.home.StartButton;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FList;
import forge.view.toolbox.FScrollPane;

/** 
 * Singleton instance of "Colors" submenu in "Constructed" group.
 *
 */
public enum ViewSubmenuCustom implements IViewSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /** */
    private final JPanel pnl            = new JPanel();
    private final StartButton btnStart  = new StartButton();
    private final List<JList> allLists  = new ArrayList<JList>();

    private ViewSubmenuCustom() {
        populate();
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        final String lstConstraints = "w 41%!, h 32%, gap 6% 0 4% 2%";
        final String btnConstraints = "newline, ax center, gap 6% 0 0 0, span 2 1";
        final List<Player> players = AllZone.getPlayersInGame();

        allLists.clear();
        pnl.removeAll();

        pnl.setOpaque(false);
        pnl.setLayout(new MigLayout("insets 0, gap 0"));

        for (int i = 0; i < players.size(); i++) {
            if (i % 2 == 1) {
                pnl.add(new CustomSelectPanel(players.get(i)), lstConstraints + ", wrap");
            }
            else {
                pnl.add(new CustomSelectPanel(players.get(i)), lstConstraints);
            }
        }

        pnl.add(btnStart, btnConstraints);
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getControl()
     */
    @Override
    public IControlSubmenu getControl() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public MenuGroup getGroup() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getPanel()
     */
    @Override
    public JPanel getPanel() {
        return pnl;
    }

    /** @return {@link java.util.List}<{@link javax.swing.JList}> */
    public List<JList> getLists() {
        return allLists;
    }

    @SuppressWarnings("serial")
    private class CustomSelectPanel extends JPanel {
        public CustomSelectPanel(final Player p0) {
            final Command cmdRandom = new Command() { @Override
                public void execute() { ControlSubmenuCustom.SINGLETON_INSTANCE.randomSelect(); } };
            allLists.add(new FList());
            allLists.get(allLists.size() - 1).setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

            this.setOpaque(false);
            this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
            this.add(new FLabel.Builder().text(p0.getName()).fontSize(14)
                    .fontScaleAuto(false).build(),
                    "w 100%!, h 25px!, gap 0 0 0 8px");
            this.add(new FScrollPane(allLists.get(allLists.size() - 1)), "w 100%!, pushy, growy");
            this.add(new FLabel.Builder().text("Random").fontSize(14).opaque(true)
                    .hoverable(true).cmdClick(cmdRandom).fontScaleAuto(false).build(),
                    "w 100%!, h 25px!, gap 0 0 8px 0");
        }
    }
}
