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
package forge.toolbox;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;

/**
 * Utility to manage absolutely positioned components
 *
 */

// Currently used only once, in top level UI, with layering already in place.
public enum FAbsolutePositioner {
    /** */
    SINGLETON_INSTANCE;

    private final JPanel panel = new JPanel();

    private FAbsolutePositioner() {
        panel.setOpaque(false);
        panel.setLayout(null);
    }

    public void initialize(final JLayeredPane parent, final Integer index) {
        parent.add(panel, index);
    }

    public void containerResized(final Rectangle mainBounds) {
        panel.setBounds(mainBounds);
        panel.validate();
    }

    /**
     * Show the given component absolutely positioned at the given screen location
     *
     * @param comp &emsp; Component to absolutely position
     * @param screenX &emsp; Screen X location to show component at
     * @param screenY &emsp; Screen Y location to show component at
     */
    public void show(final Component comp, final int screenX, final int screenY) {
        if (comp.getParent() != panel) {
            comp.setVisible(false);
            panel.add(comp);
        }
        final Point panelScreenLocation = panel.getLocationOnScreen();
        comp.setLocation(screenX - panelScreenLocation.x, screenY - panelScreenLocation.y);
        comp.setVisible(true);
    }

    /**
     * Show the given component absolutely positioned relative to another component
     *
     * @param comp &emsp; Component to absolutely position
     * @param relativeToComp &emsp; Component to position relative to
     * @param offsetX &emsp; X offset of relative location
     * @param offsetY &emsp; Y offset of relative location
     */
    public void show(final Component comp, final Component relativeToComp, final int offsetX, final int offsetY) {
        final Point screenLocation = relativeToComp.getLocationOnScreen();
        show(comp, screenLocation.x + offsetX, screenLocation.y + offsetY);
    }

    /**
     * Hide given absolutely positioned component
     *
     * @param comp &emsp; Component to hide
     */
    public void hide(final Component comp) {
        panel.remove(comp);
    }

    /**
     * Hide all absolutely positioned components
     */
    public void hideAll() {
        panel.removeAll();
        FMouseAdapter.forceMouseUp(); //ensure mouse not stuck down somewhere
    }
}
