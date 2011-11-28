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
package forge.view.toolbox;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Semi-transparent overlay panel. Should be used with layered panes.
 * 
 */

// Currently used only once, in top level UI, with layering already in place.
// Getter in AllZone: getOverlay()
@SuppressWarnings("serial")
public class FOverlay extends JPanel {
    private final JButton btnClose;

    /**
     * Semi-transparent overlay panel. Should be used with layered panes.
     */
    public FOverlay() {
        super();
        this.btnClose = new JButton("X");
        this.btnClose.setForeground(Color.white);
        this.btnClose.setBorder(BorderFactory.createLineBorder(Color.white));
        this.btnClose.setOpaque(false);
        this.btnClose.setBackground(new Color(0, 0, 0));
        this.btnClose.setFocusPainted(false);

        this.btnClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                FOverlay.this.hideOverlay();
            }
        });
    }

    /**
     * Show overlay.
     */
    public void showOverlay() {
        this.setVisible(true);
    }

    /**
     * Hide overlay.
     */
    public void hideOverlay() {
        this.setVisible(false);
    }

    /**
     * Gets the close button, which must be added dynamically since different
     * overlays have different layouts. The overlay does not have the close
     * button by default, but a fully working instance is available if required.
     * 
     * @return JButton
     */
    public JButton getBtnClose() {
        return this.btnClose;
    }

    /**
     * For some reason, the alpha channel background doesn't work properly on
     * Windows 7, so the paintComponent override is required for a
     * semi-transparent overlay.
     * 
     * @param g
     *            &emsp; Graphics object
     */
    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
    }
}
