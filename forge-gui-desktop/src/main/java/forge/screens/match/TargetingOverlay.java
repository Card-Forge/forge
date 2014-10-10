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
package forge.screens.match;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import com.google.common.collect.Lists;

import forge.Singletons;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.gui.framework.FScreen;
import forge.match.MatchUtil;
import forge.screens.match.controllers.CDock;
import forge.screens.match.views.VField;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
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

    private final OverlayPanel pnl = new OverlayPanel();
    private final List<CardPanel> cardPanels = new ArrayList<CardPanel>();
    private final List<Point[]> arcsCombat = new ArrayList<Point[]>();
    private final List<Point[]> arcsOther = new ArrayList<Point[]>();

    private CardPanel activePanel = null;

    /**
     * Semi-transparent overlay panel. Should be used with layered panes.
     */
    private TargetingOverlay() {
        pnl.setOpaque(false);
        pnl.setVisible(false);
        pnl.setFocusTraversalKeysEnabled(false);
        pnl.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPanel() {
        return this.pnl;
    }

    // TODO - this is called every repaint, regardless if card
    // positions have changed or not.  Could perform better if
    // it checked for a state change.  Doublestrike 28-09-12
    private void assembleArcs(final CombatView combat) {
        //List<VField> fields = VMatchUI.SINGLETON_INSTANCE.getFieldViews();
        arcsCombat.clear();
        arcsOther.clear();
        cardPanels.clear();
            
        switch (CDock.SINGLETON_INSTANCE.getArcState()) {
            case 0:
                return;
            case 1:
                // Draw only hovered card
                activePanel = null;
                for (VField f : VMatchUI.SINGLETON_INSTANCE.getFieldViews()) {
                    cardPanels.addAll(f.getTabletop().getCardPanels());
                    List<CardPanel> cPanels = f.getTabletop().getCardPanels();
                    for (CardPanel c : cPanels) {
                        if (c.isSelected()) {
                            activePanel = c;
                            break;
                        }
                    }
                }
                if (activePanel == null) { return; }
                break;
            default:
                // Draw all
                for (VField f : VMatchUI.SINGLETON_INSTANCE.getFieldViews()) {
                    cardPanels.addAll(f.getTabletop().getCardPanels());
                }
        }

        //final Point docOffsets = FView.SINGLETON_INSTANCE.getLpnDocument().getLocationOnScreen();
        // Locations of arc endpoint, per card, with ID as primary key.
        final Map<Integer, Point> endpoints = new HashMap<Integer, Point>();

        Point cardLocOnScreen;
        Point locOnScreen = this.getPanel().getLocationOnScreen();

        for (CardPanel c : cardPanels) {
            if (c.isShowing()) {
	            cardLocOnScreen = c.getCardLocationOnScreen();
	            endpoints.put(c.getCard().getId(), new Point(
	                (int) (cardLocOnScreen.getX() - locOnScreen.getX() + c.getWidth() / 4),
	                (int) (cardLocOnScreen.getY() - locOnScreen.getY() + c.getHeight() / 2)
	            ));
            }
        }

        if (CDock.SINGLETON_INSTANCE.getArcState() == 1) {
            // Only work with the active panel
            final CardView c = activePanel.getCard();
            addArcsForCard(c, endpoints, combat);
        } else {
            // Work with all card panels currently visible
            final List<CardView> visualized = Lists.newArrayList();
            for (final CardPanel c : cardPanels) {
                if (!c.isShowing()) {
                    continue;
                }
                final CardView card = c.getCard();
                if (visualized.contains(card)) { continue; }

                visualized.addAll(addArcsForCard(card, endpoints, combat));
            }
        }
    }

    private List<CardView> addArcsForCard(final CardView c, final Map<Integer, Point> endpoints, final CombatView combat) {
        final List<CardView> cardsVisualized = Lists.newArrayList(c);

        final CardView enchanting = c.getEnchantingCard();
        final CardView equipping = c.getEquipping();
        final CardView fortifying = c.getFortifying();
        final Iterable<CardView> enchantedBy = c.getEnchantedBy();
        final Iterable<CardView> equippedBy = c.getEquippedBy();
        final Iterable<CardView> fortifiedBy = c.getFortifiedBy();
        final CardView paired = c.getPairedWith();

        if (null != enchanting) {
            if (enchanting.getController() != null && !enchanting.getController().equals(c.getController())) {
                arcsOther.add(new Point[] {
                    endpoints.get(enchanting.getId()),
                    endpoints.get(c.getId())
                });
                cardsVisualized.add(enchanting);
            }
        }
        if (null != equipping) {
            if (equipping.getController() != null && !equipping.getController().equals(c.getController())) {
                arcsOther.add(new Point[] {
                    endpoints.get(equipping.getId()),
                    endpoints.get(c.getId())
                });
                cardsVisualized.add(equipping);
            }
        }
        if (null != fortifying) {
            if (fortifying.getController() != null && !fortifying.getController().equals(c.getController())) {
                arcsOther.add(new Point[] {
                    endpoints.get(fortifying.getId()),
                    endpoints.get(c.getId())
                });
                cardsVisualized.add(fortifying);
            }
        }
        if (null != enchantedBy) {
            for (final CardView enc : enchantedBy) {
                if (enc.getController() != null && !enc.getController().equals(c.getController())) {
                    arcsOther.add(new Point[] {
                        endpoints.get(c.getId()),
                        endpoints.get(enc.getId())
                    });
                    cardsVisualized.add(enc);
                }
            }
        }
        if (null != equippedBy) {
            for (final CardView eq : equippedBy) {
                if (eq.getController() != null && !eq.getController().equals(c.getController())) {
                    arcsOther.add(new Point[] {
                        endpoints.get(c.getId()),
                        endpoints.get(eq.getId())
                    });
                    cardsVisualized.add(eq);
                }
            }
        }
        if (null != fortifiedBy) {
            for (final CardView eq : fortifiedBy) {
                if (eq.getController() != null && !eq.getController().equals(c.getController())) {
                    arcsOther.add(new Point[] {
                        endpoints.get(c.getId()),
                        endpoints.get(eq.getId())
                    });
                    cardsVisualized.add(eq);
                }
            }
        }
        if (null != paired) {
            arcsOther.add(new Point[] {
                endpoints.get(paired.getId()),
                endpoints.get(c.getId())
            });
            cardsVisualized.add(paired);
        }
        if (null != combat) {
            final GameEntityView defender = combat.getDefender(c);
            // if c is attacking a planeswalker
            if (defender instanceof CardView) {
                arcsCombat.add(new Point[] {
                        endpoints.get(((CardView)defender).getId()),
                        endpoints.get(c.getId())
                    });
            }
            // if c is a planeswalker that's being attacked
            final Iterable<CardView> attackers = combat.getAttackersOf(c);
            for (final CardView pwAttacker : attackers) {
                arcsCombat.add(new Point[] {
                    endpoints.get(c.getId()),
                    endpoints.get(pwAttacker.getId())
                });
            }
            for (final CardView attackingCard : combat.getAttackers()) {
                final Iterable<CardView> cards = combat.getPlannedBlockers(attackingCard);
                if (cards == null) continue;
                for (final CardView blockingCard : cards) {
                    if (!attackingCard.equals(c) && !blockingCard.equals(c)) { continue; }
                    arcsCombat.add(new Point[] {
                        endpoints.get(attackingCard.getId()),
                        endpoints.get(blockingCard.getId())
                    });
                    cardsVisualized.add(blockingCard);
                }
                cardsVisualized.add(attackingCard);
            }
        }

        return cardsVisualized;
    }

    private class OverlayPanel extends SkinnedPanel {
        // Arrow drawing code by the MAGE team, used with permission.
        private Area getArrow(float length, float bendPercent) {
            float p1x = 0, p1y = 0;
            float p2x = length, p2y = 0;
            float cx = length / 2, cy = length / 8f * bendPercent;

            int bodyWidth = 15;
            float headSize = 20;

            float adjSize, ex, ey, abs_e;
            adjSize = (float) (bodyWidth / 2 / Math.sqrt(2));
            ex = p2x - cx;
            ey = p2y - cy;
            abs_e = (float) Math.sqrt(ex * ex + ey * ey);
            ex /= abs_e;
            ey /= abs_e;
            GeneralPath bodyPath = new GeneralPath();
            bodyPath.moveTo(p2x + (ey - ex) * adjSize, p2y - (ex + ey) * adjSize);
            bodyPath.quadTo(cx, cy, p1x, p1y - bodyWidth / 2);
            bodyPath.lineTo(p1x, p1y + bodyWidth / 2);
            bodyPath.quadTo(cx, cy, p2x - (ey + ex) * adjSize, p2y + (ex - ey) * adjSize);
            bodyPath.closePath();

            adjSize = (float) (headSize / Math.sqrt(2));
            ex = p2x - cx;
            ey = p2y - cy;
            abs_e = (float) Math.sqrt(ex * ex + ey * ey);
            ex /= abs_e;
            ey /= abs_e;
            GeneralPath headPath = new GeneralPath();
            headPath.moveTo(p2x - (ey + ex) * adjSize, p2y + (ex - ey) * adjSize);
            headPath.lineTo(p2x + headSize / 2, p2y);
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

            float length = (float) Math.sqrt(ex * ex + ey * ey);
            float bendPercent = (float) Math.asin(ey / length);

            if (endX > startX) {
                bendPercent = -bendPercent;
            }

            Area arrow = getArrow(length, bendPercent);
            AffineTransform af = g2d.getTransform();

            g2d.translate(startX, startY);
            g2d.rotate(Math.atan2(ey, ex));
            g2d.setColor(color); 
            g2d.fill(arrow);
            g2d.setColor(Color.BLACK);
            g2d.draw(arrow);

            g2d.setTransform(af);
        }

        public void drawArcs(Graphics2D g2d, Color color, List<Point[]> arcs) {
            for (Point[] p : arcs) {
                if (p[0] == null || p[1] == null) {
                    continue;
                }

                int endX = (int) p[0].getX();
                int endY = (int) p[0].getY();
                int startX = (int) p[1].getX();
                int startY = (int) p[1].getY();

                drawArrow(g2d, startX, startY, endX, endY, color);
            }

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
            // No need for this except in match view
            if (Singletons.getControl().getCurrentScreen() != FScreen.MATCH_SCREEN) { return; }

            super.paintComponent(g);

            // 0 is off
            int overlaystate = CDock.SINGLETON_INSTANCE.getArcState();
            if (overlaystate == 0) { return; }

            // Arc drawing
            final GameView gameView = MatchUtil.getGameView();
            if (gameView != null) {
                assembleArcs(gameView.getCombat());
            }

            if (arcsCombat.isEmpty() && arcsOther.isEmpty()) { return; }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Get arrow colors from the theme or use default colors if the theme does not have them defined
            Color colorOther = FSkin.getColor(FSkin.Colors.CLR_NORMAL_TARGETING_ARROW).getColor();
            if (colorOther.getAlpha() == 0) {
                colorOther = FSkin.getColor(FSkin.Colors.CLR_ACTIVE).alphaColor(153).getColor();
            }
            Color colorCombat = FSkin.getColor(FSkin.Colors.CLR_COMBAT_TARGETING_ARROW).getColor();
            if (colorCombat.getAlpha() == 0) {
                colorCombat = new Color(255, 0, 0, 153); 
            }

            drawArcs(g2d, colorOther, arcsOther);
            drawArcs(g2d, colorCombat, arcsCombat);

            FView.SINGLETON_INSTANCE.getFrame().repaint(); // repaint the match UI
            
        }

    }
}
