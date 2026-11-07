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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import forge.gui.SOverlayUtils;
import forge.toolbox.FSkin.SkinnedPanel;

/**
 * Semi-transparent overlay panel. Should be used with layered panes.
 *
 */

// Currently used only once, in top level UI, with layering already in place.
// Getter in AllZone: getOverlay()
@SuppressWarnings("serial")
public enum FOverlay {
    SINGLETON_INSTANCE;

    private final JButton btnClose = new JButton("X");
    private final OverlayPanel pnl = new OverlayPanel();

    /**
     * Semi-transparent overlay panel. Should be used with layered panes.
     */
    FOverlay() {
        pnl.setOpaque(false);
        pnl.setVisible(false);
        pnl.setFocusCycleRoot(true);

        btnClose.setForeground(Color.white);
        btnClose.setBorder(BorderFactory.createLineBorder(Color.white));
        btnClose.setOpaque(false);
        btnClose.setBackground(new Color(0, 0, 0));
        btnClose.setFocusPainted(false);

        btnClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                SOverlayUtils.hideOverlay();
            }
        });

        // Block all input events below the overlay
        pnl.addMouseListener(new MouseAdapter() { });
        pnl.addMouseMotionListener(new MouseMotionAdapter() { });
        pnl.addKeyListener(new KeyAdapter() { });
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

    public SkinnedPanel getPanel() {
        return this.pnl;
    }

    /**
     * Put on a component (as a client property set to true) that covers the whole overlay and paints its own
     * backdrop, so the overlay does not darken the window under it as well.
     */
    public static final String OWN_BACKDROP = "forge.overlay.ownBackdrop";

    private class OverlayPanel extends SkinnedPanel {
        /** In the Arena look the window darkens rather than going dark at once. */
        private final ArenaMotion.Fade dim = new ArenaMotion.Fade(this::repaint, 1f);

        @Override
        public void setVisible(final boolean visible) {
            if (visible && !isVisible() && ArenaTheme.isEnabled()) {
                dim.set(0f);
                dim.to(1f, 300);
            }
            super.setVisible(visible);
        }

        private boolean paintsOwnBackdrop() {
            for (final java.awt.Component c : getComponents()) {
                if (c.isVisible() && c instanceof javax.swing.JComponent
                        && Boolean.TRUE.equals(((javax.swing.JComponent) c).getClientProperty(OWN_BACKDROP))) {
                    return true;
                }
            }
            return false;
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
            if (paintsOwnBackdrop()) {
                return;
            }
            Color c = this.getBackground();
            final float shown = dim.get();
            if (shown < 1f) {
                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(c.getAlpha() * shown));
            }
            g.setColor(c);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
    }
}
