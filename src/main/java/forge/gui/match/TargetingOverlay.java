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
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import forge.Card;
import forge.Singletons;
import forge.control.FControl;
import forge.gui.match.controllers.CDock;
import forge.gui.match.nonsingleton.CField;
import forge.gui.match.nonsingleton.VField;
import forge.gui.toolbox.FSkin;
import forge.view.FView;
import forge.view.arcane.CardPanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;

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

    private CardPanel activePanel = null;
    
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

        List<VField> fields = VMatchUI.SINGLETON_INSTANCE.getFieldViews();

        switch (CDock.SINGLETON_INSTANCE.getArcState()) {
            case 0:
                return;
            case 1:
                // Draw only hovered card
                activePanel = null;
                for (CField f : CMatchUI.SINGLETON_INSTANCE.getFieldControls()) {
                    cardPanels.addAll(f.getView().getTabletop().getCardPanels());
                }
                for (VField f : fields) {
                    if (f.getTabletop().getCardFromMouseOverPanel() != null) {
                        activePanel = f.getTabletop().getMouseOverPanel();
                        break;
                    }
                }
                break;
            default:
                // Draw all
                for (CField f : CMatchUI.SINGLETON_INSTANCE.getFieldControls()) {
                    cardPanels.addAll(f.getView().getTabletop().getCardPanels());
                }
        }

        final Point docOffsets = FView.SINGLETON_INSTANCE.getLpnDocument().getLocationOnScreen();
        // Locations of arc endpoint, per card, with ID as primary key.
        final Map<Integer, Point> endpoints = new HashMap<Integer, Point>();

        for (CardPanel c : cardPanels) {
            if (!c.isShowing()) { continue; }
            endpoints.put(c.getCard().getUniqueNumber(), new Point(
                (int) (c.getParent().getLocationOnScreen().getX() + c.getCardLocation().getX() /* - docOffsets.getX() */ + c.getWidth() / 4),
                (int) (c.getParent().getLocationOnScreen().getY() + c.getCardLocation().getY() /* - docOffsets.getY() */ + c.getHeight() / 4)
            ));
        }

        List<Card> temp = new ArrayList<Card>();

        if (CDock.SINGLETON_INSTANCE.getArcState() == 1) {
            // Only work with the active panel
            if (activePanel == null) { return; }
            Card c = activePanel.getCard();
            
            Card enchanting = c.getEnchantingCard();
            List<Card> enchantedBy = c.getEnchantedBy();
            List<Card> blocking = c.getBlockedThisTurn();
            List<Card> blockedBy = c.getBlockedByThisTurn();

            if (null != enchanting) {
                if (!enchanting.getController().equals(c.getController())) {
                    arcs.add(new Point[] {
                        endpoints.get(enchanting.getUniqueNumber()),
                        endpoints.get(c.getUniqueNumber())
                    });
                }
            }

            if (null != enchantedBy) {
                for (Card enc : enchantedBy) {
                    if (!enc.getController().equals(c.getController())) {
                    arcs.add(new Point[] {
                        endpoints.get(c.getUniqueNumber()),
                        endpoints.get(enc.getUniqueNumber())
                    });
                    }
                }
            }

            for (Card attackingCard : Singletons.getModel().getGame().getCombat().getAttackers()) {
                temp = Singletons.getModel().getGame().getCombat().getBlockers(attackingCard);
                for (Card blockingCard : temp) {
                    if (!attackingCard.equals(c) && !blockingCard.equals(c)) { continue; }
                    arcs.add(new Point[] {
                        endpoints.get(attackingCard.getUniqueNumber()),
                        endpoints.get(blockingCard.getUniqueNumber())
                    });
                }
            }

            if (null != blocking) {
                for (Card b : blocking) {
                    arcs.add(new Point[]{
                        endpoints.get(c.getUniqueNumber()),
                        endpoints.get(b.getUniqueNumber())
                    });
                }
            }

            if (null != blockedBy) {
                for (Card b : blockedBy) {
                    arcs.add(new Point[]{
                        endpoints.get(c.getUniqueNumber()),
                        endpoints.get(b.getUniqueNumber())
                    });
                }
            }
        } else {
            // Work with all card panels currently visible

            // Global cards
            for (CardPanel c : cardPanels) {
                if (!c.isShowing()) {
                    continue;
                }

                // Enchantments
                Card enchanting = c.getCard().getEnchantingCard();
                if (enchanting != null) {
                    if (enchanting.getController().equals(c.getCard().getController())) {
                        continue;
                    }
                    arcs.add(new Point[]{
                                endpoints.get(enchanting.getUniqueNumber()),
                                endpoints.get(c.getCard().getUniqueNumber())
                            });
                }
            }

            // Combat cards
            for (Card attackingCard : Singletons.getModel().getGame().getCombat().getAttackers()) {
                temp = Singletons.getModel().getGame().getCombat().getBlockers(attackingCard);
                for (Card blockingCard : temp) {
                    arcs.add(new Point[]{
                                endpoints.get(attackingCard.getUniqueNumber()),
                                endpoints.get(blockingCard.getUniqueNumber())
                            });
                }
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

        // Arrow drawing code by the MAGE team, used with permission.
        private Area getArrow (float length, float bendPercent) {
            float p1x = 0, p1y = 0;
            float p2x = length, p2y = 0;
            float cx = length / 2, cy = length / 8f * bendPercent;

            int bodyWidth = 10;
            float headSize = 17;

            float adjSize, ex, ey, abs_e;
            adjSize = (float)(bodyWidth / 2 / Math.sqrt(2));
            ex = p2x - cx;
            ey = p2y - cy;
            abs_e = (float)Math.sqrt(ex * ex + ey * ey);
            ex /= abs_e;
            ey /= abs_e;
            GeneralPath bodyPath = new GeneralPath();
            bodyPath.moveTo(p2x + (ey - ex) * adjSize, p2y - (ex + ey) * adjSize);
            bodyPath.quadTo(cx, cy, p1x, p1y - bodyWidth / 2);
            bodyPath.lineTo(p1x, p1y + bodyWidth / 2);
            bodyPath.quadTo(cx, cy, p2x - (ey + ex) * adjSize, p2y + (ex - ey) * adjSize);
            bodyPath.closePath();

            adjSize = (float)(headSize / Math.sqrt(2));
            ex = p2x - cx;
            ey = p2y - cy;
            abs_e = (float)Math.sqrt(ex * ex + ey * ey);
            ex /= abs_e;
            ey /= abs_e;
            GeneralPath headPath = new GeneralPath();
            headPath.moveTo(p2x - (ey + ex) * adjSize, p2y + (ex - ey) * adjSize);
            headPath.lineTo(p2x, p2y);
            headPath.lineTo(p2x + (ey - ex) * adjSize, p2y - (ex + ey) * adjSize);
            headPath.closePath();

            Area area = new Area(headPath);
            area.add(new Area(bodyPath));
            return area;
        }

        private void drawArrow(Graphics2D g2d, int startX, int startY, int endX, int endY, Color color) {
            float ex = endX - startX;
            float ey = endY - startY;
            if (ex == 0 && ey == 0) { return; }

            float length = (float)Math.sqrt(ex * ex + ey * ey);
            float bendPercent = (float)Math.asin(ey / length);

            if (endX > startX) bendPercent = -bendPercent;

            Area arrow = getArrow(length, bendPercent);
            AffineTransform af = g2d.getTransform();

            g2d.translate(startX, startY);
            g2d.rotate(Math.atan2(ey, ex));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            g2d.setColor(color);
            g2d.fill(arrow);
            g2d.setColor(Color.BLACK);
            g2d.draw(arrow);

            g2d.setTransform(af);
        }

        @Override
        public void paintComponent(final Graphics g) {
            // No need for this except in match view
            if (FControl.SINGLETON_INSTANCE.getState() != 1) { return; }

            super.paintComponent(g);

            // 0 is off
            int overlaystate = CDock.SINGLETON_INSTANCE.getArcState();
            if (overlaystate == 0) { return; }

            // Arc drawing
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            assembleArcs();
            if (arcs.size() < 1) { return; }

            
            for (Point[] p : arcs) {
                if (p[0] == null || p[1] == null) {
                    continue;
                }
                
                int endX = (int)p[0].getX();
                int endY = (int)p[0].getY();
                int startX = (int)p[1].getX();
                int startY = (int)p[1].getY();

                Color color = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
                drawArrow(g2d, startX, startY, endX, endY, color);
            }

            FView.SINGLETON_INSTANCE.getFrame().repaint(); // repaint the match UI
        }

    }
}
