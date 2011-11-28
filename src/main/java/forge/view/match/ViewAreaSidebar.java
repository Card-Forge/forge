/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view.match;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FRoundedPanel;

/**
 * Handles display of child components of sidebar area in match UI. SHOULD
 * PROBABLY COLLAPSE INTO TOP LEVEL.
 * 
 */
@SuppressWarnings("serial")
public class ViewAreaSidebar extends FPanel {
    private final ViewCardviewer cardviewer;
    private final ViewTabber tabber;
    private final FRoundedPanel sidebar;

    /**
     * Handles display of all components of sidebar area in match UI.
     * 
     */
    public ViewAreaSidebar() {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0"));
        this.sidebar = new FRoundedPanel();
        this.sidebar.setLayout(new MigLayout("wrap, gap 0, insets 0"));
        this.sidebar.setBackground(AllZone.getSkin().getClrTheme());
        this.sidebar.setCorners(new boolean[] { true, true, false, false });

        // Add tabber, cardview, and finally sidebar. Unfortunately,
        // tabber and cardviewer cannot extend FVerticalTabPanel, since that
        // requires child panels to be prepared before it's instantiated.
        // Therefore, their vertical tab panels must be accessed indirectly via
        // an instance of this class.
        this.cardviewer = new ViewCardviewer();
        this.tabber = new ViewTabber();

        this.sidebar.add(this.cardviewer.getVtpCardviewer(), "w 97%!, h 40%!, gapleft 1%, gapright 2%");
        this.sidebar.add(this.tabber.getVtpTabber(), "w 97%!, h 60%!, gapright 2%");
        this.add(this.sidebar, "h 98%!, w 98%!, gapleft 2%, gaptop 1%");
    }

    /**
     * Retrieves vertical tab panel used for card picture and detail.
     * 
     * @return ViewCardviewer vertical tab panel
     */
    public ViewCardviewer getCardviewer() {
        return this.cardviewer;
    }

    /**
     * Retrieves vertical tab panel used for data presentation.
     * 
     * @return ViewTabber vertical tab panel
     */
    public ViewTabber getTabber() {
        return this.tabber;
    }
}
