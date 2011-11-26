package forge.view.match;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FRoundedPanel;

/** 
 * Handles display of child components of sidebar area in match UI.
 * SHOULD PROBABLY COLLAPSE INTO TOP LEVEL.
 *
 */
@SuppressWarnings("serial")
public class ViewAreaSidebar extends FPanel {
    private ViewCardviewer cardviewer;
    private ViewTabber tabber;
    private FRoundedPanel sidebar;

    /** 
     * Handles display of all components of sidebar area in match UI.
     *
     */
    public ViewAreaSidebar() {
        super();
        setOpaque(false);
        setLayout(new MigLayout("insets 0"));
        sidebar = new FRoundedPanel();
        sidebar.setLayout(new MigLayout("wrap, gap 0, insets 0"));
        sidebar.setBackground(AllZone.getSkin().getClrTheme());
        sidebar.setCorners(new boolean[] {true, true, false, false});

        // Add tabber, cardview, and finally sidebar. Unfortunately,
        // tabber and cardviewer cannot extend FVerticalTabPanel, since that
        // requires child panels to be prepared before it's instantiated.
        // Therefore, their vertical tab panels must be accessed indirectly via
        // an instance of this class.
        cardviewer = new ViewCardviewer();
        tabber = new ViewTabber();

        sidebar.add(cardviewer.getVtpCardviewer(), "w 97%!, h 40%!, gapleft 1%, gapright 2%");
        sidebar.add(tabber.getVtpTabber(), "w 97%!, h 60%!, gapright 2%");
        add(sidebar, "h 98%!, w 98%!, gapleft 2%, gaptop 1%");
    }

    /**
     * Retrieves vertical tab panel used for card picture and detail.
     * 
     * @return ViewCardviewer vertical tab panel
     */
    public ViewCardviewer getCardviewer() {
        return cardviewer;
    }

    /**
     * Retrieves vertical tab panel used for data presentation.
     * 
     * @return ViewTabber vertical tab panel
     */
    public ViewTabber getTabber() {
        return tabber;
    }
}
