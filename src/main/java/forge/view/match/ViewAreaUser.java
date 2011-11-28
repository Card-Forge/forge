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

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.view.toolbox.FPanel;

/**
 * Parent panel for display of input, hand, and dock. SHOULD PROBABLY COLLAPSE
 * INTO TOP LEVEL.
 * 
 */
@SuppressWarnings("serial")
public class ViewAreaUser extends FPanel {
    private final ViewDock pnlDock;
    private final ViewHand pnlHand;

    private JPanel pnlMessage;
    private final ViewInput pnlInput;

    /**
     * Assembles user area of match UI.
     */
    public ViewAreaUser() {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("fill, insets 0, gap 0"));

        // Input panel
        this.pnlInput = new ViewInput();

        // Hand panel
        this.pnlHand = new ViewHand();

        // Dock panel
        this.pnlDock = new ViewDock();

        // A.D.D.
        this.add(this.pnlInput, "h 100%!, west, w 200px!");
        this.add(this.pnlHand, "grow, gapleft 5");
        this.add(this.pnlDock, "growx, h 50px!, south, gaptop 5, gapleft 5");
    }

    /**
     * Gets the pnl dock.
     * 
     * @return ViewDock
     */
    public ViewDock getPnlDock() {
        return this.pnlDock;
    }

    /**
     * Gets the pnl hand.
     * 
     * @return ViewHand
     */
    public ViewHand getPnlHand() {
        return this.pnlHand;
    }

    /**
     * Gets the pnl message.
     * 
     * @return JPanel
     */
    public JPanel getPnlMessage() {
        return this.pnlMessage;
    }

    /**
     * Gets the pnl input.
     * 
     * @return ViewInput
     */
    public ViewInput getPnlInput() {
        return this.pnlInput;
    }
}
