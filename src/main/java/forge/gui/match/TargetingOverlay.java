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
package forge.gui.match;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import forge.Card;
import forge.Singletons;
import forge.control.FControl;
import forge.gui.match.nonsingleton.CField;
import forge.gui.toolbox.FSkin;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.view.FView;
import forge.view.arcane.CardPanel;

/**
 * Semi-transparent overlay panel. Should be used with layered panes.
 * 
 */

@SuppressWarnings("serial")
public enum TargetingOverlay {
    /** */
    SINGLETON_INSTANCE;

    private final JPanel pnl = new OverlayPanel();
    private final List<CardPanel> cardPanels = new ArrayList<CardPanel>();
    private final List<Point[]> arcs = new ArrayList<Point[]>();

    /**
     * Semi-transparent overlay panel. Should be used with layered panes.
     */
    private TargetingOverlay() {
        pnl.setOpaque(false);
        pnl.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPanel() {
        return this.pnl;
    }

    // TODO - this is called every repaint, regardless if card
    // positions have changed or not.  Could perform better if
    // it checked for a state change.  Doublestrike 28-09-12
    private void assembleArcs() {
        arcs.clear();
        cardPanels.clear();

        for (CField f : CMatchUI.SINGLETON_INSTANCE.getFieldControls()) {
            cardPanels.addAll(f.getView().getTabletop().getCardPanels());
        }

        final Point docOffsets = FView.SINGLETON_INSTANCE.getLpnDocument().getLocationOnScreen();
        // Locations of arc endpoint, per card, with ID as primary key.
        final Map<Integer, Point> endpoints = new HashMap<Integer, Point>();

        // Assemble card locations for easy reference
        for (CardPanel c : cardPanels) {
            if (!c.isShowing()) { continue; }
            endpoints.put(c.getCard().getUniqueNumber(), new Point(
                (int) (c.getParent().getLocationOnScreen().getX() + c.getCardLocation().getX() - docOffsets.getX() + c.getWidth() / 4),
                (int) (c.getParent().getLocationOnScreen().getY() + c.getCardLocation().getY() - docOffsets.getY() + c.getHeight() / 4)
            ));
        }

        List<Card> temp = new ArrayList<Card>();

        // Global cards
        for (CardPanel c : cardPanels) {
            if (!c.isShowing()) { continue; }

            // Enchantments
            // Doesn't work for global enchantments?! Doublestrike 10-10-12
            temp = c.getCard().getEnchantedBy();
            for (Card enchantingCard : temp) {
                arcs.add(new Point[] {
                    endpoints.get(c.getCard().getUniqueNumber()),
                    endpoints.get(enchantingCard.getUniqueNumber())
                });
            }
        }

        // Combat cards
        for (Card attackingCard : Singletons.getModel().getGame().getCombat().getAttackers()) {
            temp = Singletons.getModel().getGame().getCombat().getBlockers(attackingCard);
            for (Card blockingCard : temp) {
                arcs.add(new Point[] {
                    endpoints.get(attackingCard.getUniqueNumber()),
                    endpoints.get(blockingCard.getUniqueNumber())
                });
            }
        }

        temp.clear();
        endpoints.clear();
    }

    private class OverlayPanel extends JPanel {
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
            // No need for this except in match view
            if (FControl.SINGLETON_INSTANCE.getState() != 1) {
                return;
            }
            else if (!Boolean.valueOf(FModel.SINGLETON_INSTANCE.getPreferences().getPref(FPref.UI_TARGETING_OVERLAY))) {
                return;
            }

            super.paintComponent(g);
            // Arc drawing
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(FSkin.getColor(FSkin.Colors.CLR_ACTIVE));
            g2d.setStroke(new BasicStroke(3F));
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            assembleArcs();
            int w, h, x, y;

            for (Point[] p : arcs) {
                w = Math.abs((int) p[1].getX() - (int) p[0].getX());
                h = Math.abs((int) p[1].getY() - (int) p[0].getY());
                x = (Math.min((int) p[1].getX(), (int) p[0].getX()) - w);
                y = (Math.min((int) p[1].getY(), (int) p[0].getY()));

                g2d.drawArc(x, y, 2 * w, 2 * h, 0, 90);
                g2d.fillOval((int) p[0].getX() - 4, (int) p[0].getY() - 4, 8, 8);
                g2d.fillOval((int) p[1].getX() - 4, (int) p[1].getY() - 4, 8, 8);
            }
        }
    }
}
