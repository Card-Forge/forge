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

import java.awt.BasicStroke;
import java.awt.RadialGradientPaint;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import forge.Singletons;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.controllers.CDock.ArcState;
import forge.screens.match.views.VField;
import forge.screens.match.views.VStack.StackInstanceTextArea;
import forge.toolbox.ArenaMotion;
import forge.toolbox.ArenaTheme;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.view.FView;
import forge.view.arcane.CardPanel;
import forge.view.arcane.CardPanelContainer;
import forge.view.arcane.util.CardPanelMouseListener;

/**
 * Semi-transparent overlay panel. Should be used with layered panes.
 * 
 */
@SuppressWarnings("serial")
public class TargetingOverlay {
    private final CMatchUI matchUI;
    private final OverlayPanel pnl = new OverlayPanel();
    private final List<CardPanel> cardPanels = new ArrayList<>();
    private final List<Arc> arcsFoeAtk = new ArrayList<>();
    private final List<Arc> arcsFoeDef = new ArrayList<>();
    private final List<Arc> arcsFriend = new ArrayList<>();
    private final ArcAssembler assembler = new ArcAssembler();
    private final Set<Integer> stackItemIDs = new HashSet<>();

    private static class Arc {
        private final int x1, y1, x2, y2;

        private Arc(final Point end, final Point start) {
            x1 = start.x;
            y1 = start.y;
            x2 = end.x;
            y2 = end.y;
        }
        
        @Override
        public boolean equals(Object obj)
        {
        	Arc arc = (Arc)obj;
        	return ((arc.x1 == x1) && (arc.x2 == x2) && (arc.y1 == y1) && (arc.y2 == y2));
        }
    }

    private final Set<CardView> cardsVisualized = new HashSet<>();
    private CardPanel activePanel = null;

    //private long lastUpdated = System.currentTimeMillis(); // TODO: determine if timer is needed (see below)
    private int allowedUpdates = 0;
    private List<List<Arc>> paintedArcs = List.of(); // Arena: what the window last showed, see arcsChanged
    private final List<Arc> arenaLinks = new ArrayList<>(); // Arena: the chevrons between attackers and blockers
    private final int MAX_CONSECUTIVE_UPDATES = 1;
    // Arena: arcs that have just appeared reach out to what they are aimed at
    private final List<Arc> arcsReaching = new ArrayList<>();
    private final ArenaMotion.Fade arcReach = new ArenaMotion.Fade(this::repaintReachingArcs, 1f);

    private enum ArcConnection {
        Friends,
        FoesAttacking,
        FoesBlocking,
        FriendsStackTargeting,
        FoesStackTargeting
    }

    /**
     * Semi-transparent overlay panel. Should be used with layered panes.
     */
    public TargetingOverlay(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        pnl.setOpaque(false);
        pnl.setVisible(true);
        pnl.setFocusTraversalKeysEnabled(false);
        pnl.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getPanel() {
        return this.pnl;
    }

    // The original version of assembleArcs, without code to throttle it.
    // Re-added as the new version was causing issues for at least one user.
    private void assembleArcs(final CombatView combat) {
        //List<VField> fields = VMatchUI.SINGLETON_INSTANCE.getFieldViews();
        arcsFoeAtk.clear();
        arcsFoeDef.clear();
        arcsFriend.clear();
        cardPanels.clear();
        cardsVisualized.clear();

        final StackInstanceTextArea activeStackItem = matchUI.getCStack().getView().getHoveredItem();

        switch (matchUI.getCDock().getArcState()) {
            case OFF:
                return;
            case MOUSEOVER:
                // Draw only hovered card
                activePanel = null;
                for (final VField f : matchUI.getFieldViews()) {
                    cardPanels.addAll(f.getTabletop().getCardPanels());
                    final List<CardPanel> cPanels = f.getTabletop().getCardPanels();
                    for (final CardPanel c : cPanels) {
                        if (c.isSelected()) {
                            activePanel = c;
                            break;
                        }
                    }
                }
                if (activePanel == null && activeStackItem == null) { return; }
                break;
            case ON:
                // Draw all
                for (final VField f : matchUI.getFieldViews()) {
                    cardPanels.addAll(f.getTabletop().getCardPanels());
                }
        }

        //final Point docOffsets = FView.SINGLETON_INSTANCE.getLpnDocument().getLocationOnScreen();
        // Locations of arc endpoint, per card, with ID as primary key.
        final Map<Integer, Point> endpoints = new HashMap<>();

        Point cardLocOnScreen;
        Point locOnScreen = this.getPanel().getLocationOnScreen();

        for (CardPanel c : cardPanels) {
            if (c.isShowing()) {
	            cardLocOnScreen = c.getCardLocationOnScreen();
            endpoints.put(c.getCard().getId(), new Point(
                (int) (cardLocOnScreen.getX() - locOnScreen.getX() + (float)c.getWidth() * CardPanel.TARGET_ORIGIN_FACTOR_X),
	                (int) (cardLocOnScreen.getY() - locOnScreen.getY() + (float)c.getHeight() * CardPanel.TARGET_ORIGIN_FACTOR_Y)
            ));
           }
       }

        if (matchUI.getCDock().getArcState() == ArcState.MOUSEOVER) {
            // Only work with the active panel
            if (activePanel != null) {
                addArcsForCard(activePanel.getCard(), endpoints, combat);
            }
        }
        else {
            // Work with all card panels currently visible
            for (final CardPanel c : cardPanels) {
                if (!c.isShowing()) {
                    continue;
                }
                addArcsForCard(c.getCard(), endpoints, combat);
            }
        }

        //draw arrow connecting active item on stack
        if (activeStackItem != null) {
            Point itemLocOnScreen = activeStackItem.getLocationOnScreen();
            if (itemLocOnScreen != null) {
                itemLocOnScreen.x += StackInstanceTextArea.CARD_WIDTH * CardPanel.TARGET_ORIGIN_FACTOR_X + StackInstanceTextArea.PADDING - locOnScreen.getX();
                itemLocOnScreen.y += StackInstanceTextArea.CARD_HEIGHT * CardPanel.TARGET_ORIGIN_FACTOR_Y + StackInstanceTextArea.PADDING - locOnScreen.getY();
    
                StackItemView instance = activeStackItem.getItem();
                PlayerView activator = instance.getActivatingPlayer();
                while (instance != null) {
                    for (CardView c : instance.getTargetCards()) {
                        addArc(endpoints.get(c.getId()), itemLocOnScreen, activator.isOpponentOf(c.getController()) ?
                                ArcConnection.FoesStackTargeting : ArcConnection.FriendsStackTargeting);
                    }
                    for (PlayerView p : instance.getTargetPlayers()) {
                        Point point = getPlayerTargetingArrowPoint(p, locOnScreen);
                        if(point != null) {
                            addArc(point, itemLocOnScreen, activator.isOpponentOf(p) ?
                                    ArcConnection.FoesStackTargeting : ArcConnection.FriendsStackTargeting);
                        }
                    }
                    instance = instance.getSubInstance();
                }
            }
        }
    }

    // A throttled version of assembleArcs. Though it is still called on every
    // repaint, we take means to avoid it fully running every time (to reduce CPU usage).
    private boolean assembleArcs(final CombatView combat, boolean forceAssemble) {
        if (!this.getPanel().isShowing()) {
            return false;
        }

        if (!forceAssemble) {
            /* -- Minimum update frequency timer, currently disabled --
            long now = System.currentTimeMillis();
            if (now - lastUpdated <= 10) {
                // TODO: Minimum timer needed? How bad are CPU spikes without this on fast machines?
                return false;
            }
            */
            if (allowedUpdates >= MAX_CONSECUTIVE_UPDATES) {
                // Reduce update spam by blocking every Nth attempt (zero-based).
                // N should be adjusted to as low as possible to keep CPU usage low,
                // while being high enough to avoid visual artifacts.
                allowedUpdates = 0;
                return false;
            } else {
                allowedUpdates++;
                //lastUpdated = now; // Uncomment if enabling timer above
            }
        }

        if (!assembler.isListening() && matchUI != null && matchUI.getFieldViews() != null) {
            assembler.setListening(true);
            for (final VField f : matchUI.getFieldViews()) {
                f.getTabletop().addLayoutListener(assembler);
                f.getTabletop().addCardPanelMouseListener(assembler);
            }
        }
        //List<VField> fields = VMatchUI.SINGLETON_INSTANCE.getFieldViews();
        arcsFoeAtk.clear();
        arcsFoeDef.clear();
        arcsFriend.clear();
        cardPanels.clear();
        cardsVisualized.clear();

        try {
            switch (matchUI.getCDock().getArcState()) {
                case OFF:
                    return true;
                case MOUSEOVER:
                    // Draw only hovered card
                    activePanel = null;
                    for (final VField f : matchUI.getFieldViews()) {
                        cardPanels.addAll(f.getTabletop().getCardPanels());
                        final List<CardPanel> cPanels = f.getTabletop().getCardPanels();
                        for (final CardPanel c : cPanels) {
                            if (c.isSelected()) {
                                activePanel = c;
                                break;
                            }
                        }
                    }
                    if (activePanel == null) { return true; }
                    break;
                case ON:
                    // Draw all
                    for (final VField f : matchUI.getFieldViews()) {
                        cardPanels.addAll(f.getTabletop().getCardPanels());
                    }
            }

            //final Point docOffsets = FView.SINGLETON_INSTANCE.getLpnDocument().getLocationOnScreen();
            // Locations of arc endpoint, per card, with ID as primary key.
            final Map<Integer, Point> endpoints = getCardEndpoints();

            if (matchUI.getCDock().getArcState() == ArcState.MOUSEOVER) {
                // Only work with the active panel
                if (activePanel != null) {
                    addArcsForCard(activePanel.getCard(), endpoints, combat);
                }
            }
            else {
                // Work with all card panels currently visible
                for (final CardPanel c : cardPanels) {
                    if (!c.isShowing()) {
                        continue;
                    }
                    addArcsForCard(c.getCard(), endpoints, combat);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private Map<Integer, Point> getCardEndpoints() {
        final Map<Integer, Point> endpoints = new HashMap<>();

        Point cardLocOnScreen;
        Point locOnScreen = this.getPanel().getLocationOnScreen();

        for (CardPanel c : cardPanels) {
            if (c.isShowing()) {
	            cardLocOnScreen = c.getCardLocationOnScreen();
	            final float originX = ArenaTheme.isEnabled() ? c.getCardWidth() * 0.5f : c.getWidth() * CardPanel.TARGET_ORIGIN_FACTOR_X;
	            final float originY = ArenaTheme.isEnabled() ? c.getCardHeight() * 0.5f : c.getHeight() * CardPanel.TARGET_ORIGIN_FACTOR_Y;
	            endpoints.put(c.getCard().getId(), new Point(
	                (int) (cardLocOnScreen.getX() - locOnScreen.getX() + originX),
	                (int) (cardLocOnScreen.getY() - locOnScreen.getY() + originY)
	            ));
            }
        }
        return endpoints;
    }

    // This section is a refactored portion of the new-style assembleArcs.
    private void assembleStackArrows() {
        if (!this.getPanel().isShowing()) {
            return;
        }

        final StackInstanceTextArea activeStackItem = matchUI.getCStack().getView().getHoveredItem();

        if (activeStackItem != null) {
            // Add event listeners to the stack item to repaint on mouse
            // entry/exit, to clear up visual artifacts of our arrows
            if (stackItemIDs.add(activeStackItem.hashCode())) {
                activeStackItem.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(final MouseEvent e) {
                    	if (matchUI.getCDock().getArcState() == ArcState.MOUSEOVER)	{
                    		assembleStackArrows();
                    		FView.SINGLETON_INSTANCE.getFrame().repaint();
                    	}
                    }
                    @Override
                    public void mouseExited(final MouseEvent e) {
                    	if (matchUI.getCDock().getArcState() == ArcState.MOUSEOVER)	{
                    		assembleStackArrows();
                    		FView.SINGLETON_INSTANCE.getFrame().repaint();
                    	}
                    }
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                    	if (matchUI.getCDock().getArcState() == ArcState.ON) {
                    		assembleStackArrows();
                    		FView.SINGLETON_INSTANCE.getFrame().repaint();
                    	}
                    }
                });
            }
            final Map<Integer, Point> endpoints = getCardEndpoints();
            Point locOnScreen = this.getPanel().getLocationOnScreen();
            Point itemLocOnScreen = activeStackItem.getLocationOnScreen();
            if (itemLocOnScreen != null) {
                itemLocOnScreen.x += StackInstanceTextArea.CARD_WIDTH * CardPanel.TARGET_ORIGIN_FACTOR_X + StackInstanceTextArea.PADDING - locOnScreen.getX();
                itemLocOnScreen.y += StackInstanceTextArea.CARD_HEIGHT * CardPanel.TARGET_ORIGIN_FACTOR_Y + StackInstanceTextArea.PADDING - locOnScreen.getY();
    
                StackItemView instance = activeStackItem.getItem();
                PlayerView activator = instance.getActivatingPlayer();
                while (instance != null) {
                    for (CardView c : instance.getTargetCards()) {
                        addArc(endpoints.get(c.getId()), itemLocOnScreen, activator.isOpponentOf(c.getController()) ?
                                ArcConnection.FoesStackTargeting : ArcConnection.FriendsStackTargeting);
                    }
                    for (PlayerView p : instance.getTargetPlayers()) {
                        Point point = getPlayerTargetingArrowPoint(p, locOnScreen);
                        if (point != null) {
                            addArc(point, itemLocOnScreen, activator.isOpponentOf(p) ?
                                    ArcConnection.FoesStackTargeting : ArcConnection.FriendsStackTargeting);
                        }
                    }
                    instance = instance.getSubInstance();
                }
            }
        }
    }

    private Point getPlayerTargetingArrowPoint(final PlayerView p, final Point locOnScreen) {
        final JPanel avatarArea = matchUI.getFieldViewFor(p).getAvatarArea();
        if(!avatarArea.isShowing()) {
            return null;
        }

        if (ArenaTheme.isEnabled()) {
            final Point rim = matchUI.getFieldViewFor(p).getArenaTargetPoint();
            rim.translate(-locOnScreen.x, -locOnScreen.y);
            return rim;
        }
        final Point point = avatarArea.getLocationOnScreen();
        point.x += avatarArea.getWidth() / 2 - locOnScreen.x;
        point.y += avatarArea.getHeight() / 2 - locOnScreen.y;
        return point;
    }

    private void addArc(Point end, Point start, ArcConnection connects) {
        if (start == null || end == null) {
            return;
        }
        
        Arc newArc = new Arc(end,start);
        
        switch (connects) {
            case Friends:
            case FriendsStackTargeting:
            	if (!arcsFriend.contains(newArc)) {
            		arcsFriend.add(newArc);
            	}
                break;
            case FoesAttacking:
                if (!arcsFoeAtk.contains(newArc)) {
                	arcsFoeAtk.add(newArc);
                }
                break;
            case FoesBlocking:
            case FoesStackTargeting:
            	if (!arcsFoeDef.contains(newArc)) {
            		arcsFoeDef.add(newArc);
            	}
            	break;
        }
    }

    private void addArcsForCard(final CardView c, final Map<Integer, Point> endpoints, final CombatView combat) {
        if (!cardsVisualized.add(c)) {
            return; //don't add arcs for cards if card already visualized
        }

        final CardView attachedTo = c.getAttachedTo();
        final CardView paired = c.getPairedWith();

        if (null != attachedTo) {
            if (attachedTo.getController() != null && !attachedTo.getController().equals(c.getController())) {
                addArc(endpoints.get(attachedTo.getId()), endpoints.get(c.getId()), ArcConnection.Friends);
                cardsVisualized.add(attachedTo);
            }
        }

        for (final CardView enc : c.getAttachedCards()) {
            if (enc.getController() != null && !enc.getController().equals(c.getController())) {
                addArc(endpoints.get(c.getId()), endpoints.get(enc.getId()), ArcConnection.Friends);
                cardsVisualized.add(enc);
            }
        }

        if (null != paired) {
            addArc(endpoints.get(paired.getId()), endpoints.get(c.getId()), ArcConnection.Friends);
            cardsVisualized.add(paired);
        }
        if (null != combat) {
            final GameEntityView defender = combat.getDefender(c);
            // if c is attacking a planeswalker
            if (defender instanceof CardView) {
                addArc(endpoints.get(defender.getId()), endpoints.get(c.getId()), ArcConnection.FoesAttacking);
            }
            // if c is attacking a player
            final boolean blockedOnBoard = ArenaTheme.isMatchBoard()
                    && (hasAny(combat.getBlockers(c)) || hasAny(combat.getPlannedBlockers(c)));
            if (defender instanceof PlayerView && !blockedOnBoard) {
                final JPanel avatarArea = matchUI.getFieldViewFor((PlayerView)defender).getAvatarArea();
                if(avatarArea.isShowing()) {
                    Point locOnScreen = this.getPanel().getLocationOnScreen();
                    Point point = getPlayerTargetingArrowPoint((PlayerView)defender, locOnScreen);
                    addArc(point, endpoints.get(c.getId()), ArcConnection.FoesAttacking);
                }
            }
            // if c is a planeswalker that's being attacked
            for (final CardView pwAttacker : combat.getAttackersOf(c)) {
                addArc(endpoints.get(c.getId()), endpoints.get(pwAttacker.getId()), ArcConnection.FoesAttacking);
            }
            for (final CardView attackingCard : combat.getAttackers()) {
                final Iterable<CardView> cards = combat.getPlannedBlockers(attackingCard);
                if (cards == null || ArenaTheme.isMatchBoard()) continue; // a blocker stands opposite its attacker
                for (final CardView blockingCard : cards) {
                    if (!attackingCard.equals(c) && !blockingCard.equals(c)) { continue; }
                    addArc(endpoints.get(attackingCard.getId()), endpoints.get(blockingCard.getId()), ArcConnection.FoesBlocking);
                    cardsVisualized.add(blockingCard);
                    cardsVisualized.add(attackingCard);
                }
            }
        }
    }

    private static boolean hasAny(final Iterable<CardView> cards) {
        return cards != null && cards.iterator().hasNext();
    }

    /**
     * Arena: whether the arcs differ from what the window last showed. The whole window needs repainting only
     * when they do, to clear the old ones away; a full repaint of the Arena board is too dear to do otherwise.
     */
    private boolean arcsChanged() {
        final List<List<Arc>> now = List.of(new ArrayList<>(arcsFoeAtk), new ArrayList<>(arcsFoeDef), new ArrayList<>(arcsFriend), new ArrayList<>(arenaLinks));
        if (now.equals(paintedArcs)) {
            return false;
        }
        paintedArcs = now;
        return true;
    }

    private void repaintReachingArcs() {
        if (arcsReaching.isEmpty()) {
            return;
        }
        final java.awt.Rectangle area = new java.awt.Rectangle(arcsReaching.get(0).x1, arcsReaching.get(0).y1, 0, 0);
        for (final Arc arc : arcsReaching) {
            area.add(arc.x1, arc.y1);
            area.add(arc.x2, arc.y2);
        }
        // the arc bows out of the line between its ends, and lands in a burst
        area.grow(Math.max(90, Math.max(area.width, area.height) / 5), Math.max(90, Math.max(area.width, area.height) / 5));
        getPanel().repaint(area);
    }

    /** Arcs that were not there before and do not just replace ones that moved. */
    private void noticeNewArcs(final List<List<Arc>> before) {
        final List<Arc> now = new ArrayList<>(arcsFoeAtk);
        now.addAll(arcsFoeDef);
        now.addAll(arcsFriend);
        final List<Arc> was = new ArrayList<>();
        for (int i = 0; i < Math.min(3, before.size()); i++) {
            was.addAll(before.get(i));
        }
        if (now.size() <= was.size()) {
            return;
        }
        now.removeAll(was);
        arcsReaching.clear();
        arcsReaching.addAll(now);
        arcReach.set(0f);
        arcReach.to(1f, 320);
    }

    /** Arena: what is on top of the stack reaches out from the large card to its targets. */
    private void addArenaStackArcs() {
        if (!ArenaTheme.isMatchBoard() || !this.getPanel().isShowing()) {
            return;
        }
        final GameView gameView = matchUI.getGameView();
        final StackItemView top = gameView == null ? null : gameView.peekStack();
        final Point locOnScreen = this.getPanel().getLocationOnScreen();
        final Point from = top == null ? null : ArenaFocusCard.getArcOrigin();
        if (from == null) {
            return;
        }
        from.translate(-locOnScreen.x, -locOnScreen.y);
        if (cardPanels.isEmpty()) {
            for (final VField f : matchUI.getFieldViews()) {
                cardPanels.addAll(f.getTabletop().getCardPanels());
            }
        }
        final Map<Integer, Point> endpoints = getCardEndpoints();
        final PlayerView activator = top.getActivatingPlayer();
        StackItemView instance = top;
        while (instance != null) {
            if (instance.getTargetCards() != null) {
                for (final CardView c : instance.getTargetCards()) {
                    addArc(endpoints.get(c.getId()), from, ArcConnection.FriendsStackTargeting);
                }
            }
            if (instance.getTargetPlayers() != null) {
                for (final PlayerView p : instance.getTargetPlayers()) {
                    addArc(getPlayerTargetingArrowPoint(p, locOnScreen), from, ArcConnection.FriendsStackTargeting);
                }
            }
            instance = instance.getSubInstance();
        }
    }

    private class OverlayPanel extends SkinnedPanel {
        private final boolean useThrottling = FModel.getPreferences().getPrefBoolean(FPref.UI_TIMED_TARGETING_OVERLAY_UPDATES);

        // Arena: one full repaint once things settle, so that anything which changed without asking to be
        // repainted still comes right, without every small repaint costing a whole window as it otherwise did
        private final javax.swing.Timer settled = new javax.swing.Timer(150, e -> FView.SINGLETON_INSTANCE.getFrame().repaint());
        private long settlingSince;

        private void repaintOnceSettled(final Graphics g) {
            final java.awt.Rectangle clip = g.getClipBounds();
            if (clip != null && clip.width >= getWidth() && clip.height >= getHeight()) {
                return; // this is the whole window being painted, so there is nothing left to settle
            }
            if (ArenaMotion.isAmbientPaint(this, clip)) {
                return; // something breathing does not need the window repainted once a second to go with it
            }
            settled.setRepeats(false);
            final long now = System.currentTimeMillis();
            if (!settled.isRunning()) {
                settlingSince = now;
                settled.start();
            } else if (now - settlingSince < 1000) {
                settled.restart(); // still busy; but not put off for ever, while say an AI's timer ticks away
            }
        }

        // Arrow drawing code by the XMage team, used with permission.
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

        private void drawArcs(Graphics2D g2d, Color color, List<Arc> arcs) {
            for (Arc arc : arcs) {
                if (ArenaTheme.isEnabled()) {
                    drawArenaArc(g2d, arc.x1, arc.y1, arc.x2, arc.y2, color, arcsReaching.contains(arc) ? arcReach.get() : 1f);
                } else {
                    drawArrow(g2d, arc.x1, arc.y1, arc.x2, arc.y2, color);
                }
            }
        }

        /** A curved ribbon of light that narrows as it goes: wide faint layers round a bright core, a head, and a ring on what it reaches. */
        private void drawArenaArc(final Graphics2D g2d, final int x10, final int y10, final int x20, final int y20, final Color color, final float reached) {
            final float dx0 = x20 - x10, dy0 = y20 - y10;
            final float length0 = (float) Math.hypot(dx0, dy0);
            if (length0 < 2f) {
                return;
            }
            // starts from the edge of what it comes from rather than the middle of its art
            final float skip = Math.min(70f, length0 * 0.3f);
            final int x1 = Math.round(x10 + dx0 / length0 * skip), y1 = Math.round(y10 + dy0 / length0 * skip);
            final float dx = x20 - x1, dy = y20 - y1;
            final float length = (float) Math.hypot(dx, dy);
            // bows upwards, away from the row of cards it crosses
            final float bow = length * 0.16f * (dx >= 0 ? -1f : 1f);
            final float wholeCx = (x1 + x20) / 2f - dy / length * bow, wholeCy = (y1 + y20) / 2f + dx / length * bow;
            // so far as it has reached yet: the same curve, cut off short
            final float left = 1f - reached;
            final float cx = x1 + (wholeCx - x1) * reached, cy = y1 + (wholeCy - y1) * reached;
            final float x2 = left * left * x1 + 2f * left * reached * wholeCx + reached * reached * x20;
            final float y2 = left * left * y1 + 2f * left * reached * wholeCy + reached * reached * y20;
            // thin where it leaves, full where it lands, in a soft body of light
            final float start = 6f, end = 6f + 11f * reached;
            final int steps = 9;
            for (int i = steps; i >= 1; i--) {
                final float grow = 1f + i * 0.42f;
                g2d.setColor(ArenaTheme.alpha(color, Math.round(6f + 2.4f * (steps - i))));
                g2d.fill(ribbon(x1, y1, cx, cy, x2, y2, start * grow, end * grow));
            }
            g2d.setColor(ArenaTheme.alpha(color, 235));
            g2d.fill(ribbon(x1, y1, cx, cy, x2, y2, start, end));
            g2d.setColor(new Color(255, 255, 255, 240));
            g2d.fill(ribbon(x1, y1, cx, cy, x2, y2, start * 0.36f, end * 0.42f));
            // it lands in a burst of its own light rather than ending square
            final float burst = end * 3.2f;
            g2d.setPaint(new RadialGradientPaint(x2, y2, burst, new float[] {0f, 0.3f, 1f},
                    new Color[] {new Color(255, 255, 255, 245), ArenaTheme.alpha(color, 205), ArenaTheme.alpha(color, 0)}));
            g2d.fillOval(Math.round(x2 - burst), Math.round(y2 - burst), Math.round(2 * burst), Math.round(2 * burst));
        }

        /** Between an attacker and whatever blocks it: a pair of chevrons pointing at the blocker. */
        private void drawArenaCombatLinks(final Graphics2D g2d, final CombatView combat) {
            if (combat == null || !TargetingOverlay.this.getPanel().isShowing() || combat.getAttackers() == null) {
                return;
            }
            if (cardPanels.isEmpty()) {
                for (final VField f : matchUI.getFieldViews()) {
                    cardPanels.addAll(f.getTabletop().getCardPanels());
                }
            }
            final Map<Integer, Point> endpoints = getCardEndpoints();
            for (final CardView attacker : combat.getAttackers()) {
                final Point from = endpoints.get(attacker.getId());
                if (from == null) {
                    continue;
                }
                final Set<CardView> blockers = new HashSet<>();
                if (combat.getBlockers(attacker) != null) {
                    blockers.addAll(combat.getBlockers(attacker));
                }
                if (combat.getPlannedBlockers(attacker) != null) {
                    blockers.addAll(combat.getPlannedBlockers(attacker));
                }
                for (final CardView blocker : blockers) {
                    final Point to = endpoints.get(blocker.getId());
                    if (to != null) {
                        arenaLinks.add(new Arc(to, from));
                        drawArenaChevrons(g2d, from, to);
                    }
                }
            }
        }

        private void drawArenaChevrons(final Graphics2D g2d, final Point from, final Point to) {
            final float dx = to.x - from.x, dy = to.y - from.y;
            final float length = Math.max(1f, (float) Math.hypot(dx, dy));
            final float ux = dx / length, uy = dy / length;
            final float mx = (from.x + to.x) / 2f, my = (from.y + to.y) / 2f;
            final float s = 13f;
            for (int pass = 0; pass < 2; pass++) {
                g2d.setStroke(new BasicStroke(pass == 0 ? 8.5f : 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.setColor(pass == 0 ? new Color(10, 6, 2, 200) : new Color(0xFFB067));
                for (final float offset : new float[] {-s * 0.55f, s * 0.55f}) {
                    final float cx = mx + ux * offset, cy = my + uy * offset;
                    final GeneralPath chevron = new GeneralPath();
                    chevron.moveTo(cx - ux * s * 0.5f - uy * s, cy - uy * s * 0.5f + ux * s);
                    chevron.lineTo(cx + ux * s * 0.5f, cy + uy * s * 0.5f);
                    chevron.lineTo(cx - ux * s * 0.5f + uy * s, cy - uy * s * 0.5f - ux * s);
                    g2d.draw(chevron);
                }
            }
            g2d.setStroke(new BasicStroke(1f));
        }

        /** The outline of a quadratic curve thickened to a width that changes along it. */
        private GeneralPath ribbon(final float x1, final float y1, final float cx, final float cy, final float x2, final float y2, final float w1, final float w2) {
            final int steps = 36;
            final float[] lx = new float[steps + 1], ly = new float[steps + 1], rx = new float[steps + 1], ry = new float[steps + 1];
            for (int i = 0; i <= steps; i++) {
                final float t = i / (float) steps, m = 1f - t;
                final float px = m * m * x1 + 2 * m * t * cx + t * t * x2;
                final float py = m * m * y1 + 2 * m * t * cy + t * t * y2;
                float dx = 2 * m * (cx - x1) + 2 * t * (x2 - cx);
                float dy = 2 * m * (cy - y1) + 2 * t * (y2 - cy);
                final float len = Math.max(0.001f, (float) Math.hypot(dx, dy));
                dx /= len;
                dy /= len;
                final float half = (w1 + (w2 - w1) * t) / 2f;
                lx[i] = px - dy * half;
                ly[i] = py + dx * half;
                rx[i] = px + dy * half;
                ry[i] = py - dx * half;
            }
            final GeneralPath path = new GeneralPath();
            path.moveTo(lx[0], ly[0]);
            for (int i = 1; i <= steps; i++) {
                path.lineTo(lx[i], ly[i]);
            }
            for (int i = steps; i >= 0; i--) {
                path.lineTo(rx[i], ry[i]);
            }
            path.closePath();
            return path;
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
            if (Singletons.getControl().getCurrentScreen() != matchUI.getScreen()) {
                return;
            }

            super.paintComponent(g);
            if (ArenaTheme.isEnabled()) {
                repaintOnceSettled(g);
            }

            final ArcState overlaystate = matchUI.getCDock().getArcState();

            // Arcs are off
            if (overlaystate == ArcState.OFF) { return; }

            // Arc drawing
            boolean assembled = false;
            final GameView gameView = matchUI.getGameView();
            if (gameView != null) {
                if (useThrottling) {
                    assembled = assembleArcs(gameView.getCombat(), false);
                    assembleStackArrows();
                } else {
                    assembleArcs(gameView.getCombat());
                }
            }

            addArenaStackArcs();
            final boolean arena = ArenaTheme.isEnabled();
            arenaLinks.clear();
            if (arena && gameView != null) {
                final Graphics2D links = (Graphics2D) g.create();
                links.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawArenaCombatLinks(links, gameView.getCombat());
                links.dispose();
            }
            final List<List<Arc>> arcsBefore = paintedArcs;
            final boolean changed = arena && arcsChanged();
            if (changed) {
                noticeNewArcs(arcsBefore);
            }
            if (arcsFoeAtk.isEmpty() && arcsFoeDef.isEmpty() && arcsFriend.isEmpty()) {
                if (arena ? changed : assembled) {
                    // We still need to repaint to get rid of visual artifacts
                    // The original (non-throttled) code did not do this repaint.
                    FView.SINGLETON_INSTANCE.getFrame().repaint();
                }
                return;
            }

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
            Color colorCombatAtk = FSkin.getColor(FSkin.Colors.CLR_PWATTK_TARGETING_ARROW).getColor();
            if (colorCombatAtk.getAlpha() == 0) {
                colorCombatAtk = new Color(255,138,1,153);
            }

            if (ArenaTheme.isEnabled()) {
                // attacks burn orange; anything aimed from the stack is cool blue
                colorOther = new Color(120, 205, 255);
                colorCombatAtk = new Color(255, 130, 60);
                colorCombat = colorOther;
            }
            drawArcs(g2d, colorOther, arcsFriend);
            drawArcs(g2d, colorCombatAtk, arcsFoeAtk);
            drawArcs(g2d, colorCombat, arcsFoeDef);

            if (arena ? changed : assembled || !useThrottling) {
                FView.SINGLETON_INSTANCE.getFrame().repaint(); // repaint the match UI
            }
        }
    }

    // A class for listening to CardPanelContainer events, so that we can redraw arcs in response
    class ArcAssembler implements CardPanelContainer.LayoutEventListener, CardPanelMouseListener {
        // An "initialized"-type variable is needed since we can't initialize on construction
        // (because CardPanelContainers aren't ready at that point)
	    private boolean isListening = false;
        private boolean isDragged = false;
	    public boolean isListening() { return isListening; }
	    public void setListening(boolean listening) { isListening = listening; }

        private void assembleAndRepaint() {
            if (isDragged) { return; }

            final GameView gameView = matchUI.getGameView();
            if (gameView != null) {
                assembleArcs(gameView.getCombat(), true); // Force update despite timer
                FView.SINGLETON_INSTANCE.getFrame().repaint(); // repaint the match UI
            }
        }
        @Override
        public void doingLayout() {
            // Arena: cards see to their own repainting as they move, so the window only needs it if the arcs changed
            if (ArenaTheme.isEnabled()) {
                assembleOnHover();
            } else {
                assembleAndRepaint();
            }
        }
        @Override
        public void mouseOver(CardPanel panel, MouseEvent evt) {
            assembleOnHover();
        }
        @Override
        public void mouseOut(CardPanel panel, MouseEvent evt) {
            assembleOnHover();
        }

        /** Arena: the mouse going over a card is no reason to repaint the window, unless that changes the arcs. */
        private void assembleOnHover() {
            if (!ArenaTheme.isEnabled()) {
                assembleAndRepaint();
                return;
            }
            final GameView gameView = matchUI.getGameView();
            if (isDragged || gameView == null) {
                return;
            }
            assembleArcs(gameView.getCombat(), true);
            addArenaStackArcs();
            if (arcsChanged()) {
                FView.SINGLETON_INSTANCE.getFrame().repaint();
            }
        }

        // Do not aggressively assemble/repaint when dragging around card panels
        @Override
        public void mouseDragStart(CardPanel dragPanel, MouseEvent evt) { isDragged = true; }
        @Override
        public void mouseDragEnd(CardPanel dragPanel, MouseEvent evt) { isDragged = false; }

        // We don't need the other mouse events the interface provides; stub them out
        @Override
        public void mouseLeftClicked(CardPanel panel, MouseEvent evt) {}
        @Override
        public void mouseRightClicked(CardPanel panel, MouseEvent evt) {}
        @Override
        public void mouseDragged(CardPanel dragPanel, int dragOffsetX, int dragOffsetY, MouseEvent evt) {}
    }
}
