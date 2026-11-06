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
package forge.screens.match.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import forge.deck.CommanderBracketCalculator;
import forge.deck.Deck;
import forge.game.GameType;
import forge.game.card.CounterEnumType;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.localinstance.skin.FSkinProp;
import forge.screens.match.ArenaFx;
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CField;
import forge.toolbox.ArenaArt;
import forge.toolbox.ArenaMotion;
import forge.toolbox.ArenaTheme;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.special.PhaseIndicator;
import forge.toolbox.special.PlayerDetailsPanel;
import forge.util.AITimeoutTracker;
import forge.util.CardTranslation;
import forge.util.Localizer;
import forge.view.FView;
import forge.view.arcane.PlayArea;
import net.miginfocom.swing.MigLayout;

/** 
 * Assembles Swing components of a player field instance.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VField implements IVDoc<CField> {
    private final static int LIFE_CRITICAL = 5;
    private final static int POISON_CRITICAL = 8;

    // Fields used with interface IVDoc
    private final CField control;
    private DragCell parentCell;
    private final EDocID docID;
    private final DragTab tab = new DragTab(Localizer.getInstance().getMessage("lblField"));

    // Other fields
    private final CMatchUI matchUI;
    private final PlayerView player;
    private boolean commanderBracketTooltipCalculated = false;
    private String commanderBracketTooltipLine;

    // Top-level containers
    private final FScrollPane scroller = new FScrollPane(false);
    private final PlayArea tabletop;
    private final boolean mirror;
    private boolean avatarHovered;
    private JPanel[] arenaLadder;
    private JPanel arenaZones;
    private JPanel arenaExtras;
    private JComponent arenaHandFan;
    private boolean arenaSeat;
    private final Rectangle arenaPortrait = new Rectangle(); // where the portrait goes inside its area
    private final SkinnedPanel avatarArea = new SkinnedPanel() {
        @Override
        public void paint(final Graphics g) {
            // a dialog's button sits where the local portrait is: it is left out of the dialog's backdrop
            if (ArenaTheme.snapshotting && !mirror) {
                return;
            }
            super.paint(g);
        }

        @Override
        public boolean contains(final int x, final int y) {
            if (!ArenaTheme.isEnabled() || arenaPortrait.isEmpty()) {
                return super.contains(x, y);
            }
            // only the portrait and its plate take the mouse; the frame's glow leaves the cards around it clickable
            final int cx = arenaPortrait.x + arenaPortrait.width / 2, cy = arenaPortrait.y + arenaPortrait.height / 2;
            final int r = arenaPortrait.width * 3 / 5;
            return (x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r
                    || (Math.abs(x - cx) < r && y >= cy && y <= cy + arenaPortrait.height * 3 / 4);
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            if (ArenaTheme.isEnabled()) {
                paintArenaAvatarFrame((Graphics2D) g);
            }
        }

        @Override
        protected void paintChildren(final Graphics g) {
            super.paintChildren(g);
            if (ArenaTheme.isEnabled()) {
                paintArenaLifeDisc((Graphics2D) g);
            }
            paintAITimeoutBar(g);
        }
    };
    private final JPanel arenaCommanderDamage = new JPanel(new MigLayout("insets 0, gap 3, hidemode 3"));

    // ---- AI "thinking" timer bar over the avatar (shrinks to 0 at the AI timeout) ----
    private static final Color BAR_BG = new Color(0, 0, 0, 165);
    private static final Color BAR_AMBER = new Color(255, 165, 0);
    private static final Set<VField> LIVE_FIELDS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    static {
        // AITimeoutTracker calls this when a bar is due to appear and whenever an AI decision ends
        AITimeoutTracker.setRepaintHook(() -> SwingUtilities.invokeLater(() -> {
            final List<VField> fields;
            synchronized (LIVE_FIELDS) { fields = new ArrayList<>(LIVE_FIELDS); }
            for (final VField f : fields) { f.refreshAITimeoutBar(); }
        }));
    }
    private final Timer aiTimeoutTimer = new Timer(50, e -> refreshAITimeoutBar());
    private boolean aiTimeoutBarShown;

    /** (game, player) key of this field's player; the tracker is shared by every game in the JVM. */
    private long aiTimeoutKey() {
        final GameView gv = matchUI == null ? null : matchUI.getGameView();
        return gv == null ? Long.MIN_VALUE : AITimeoutTracker.key(gv.getId(), player.getId()); // MIN_VALUE: no session
    }

    private void refreshAITimeoutBar() {
        if (player == null) { return; }
        final boolean show = AITimeoutTracker.getRemainingFraction(aiTimeoutKey()) >= 0f;
        if (show) {
            if (!aiTimeoutTimer.isRunning()) { aiTimeoutTimer.start(); }
            avatarArea.repaint();
        } else {
            aiTimeoutTimer.stop();
            if (aiTimeoutBarShown) { avatarArea.repaint(); } // clear it
        }
        aiTimeoutBarShown = show;
    }

    /** Square the circular portrait occupies inside the avatar label (the icon is scaled to the label height). */
    private Rectangle getAvatarCircleBounds() {
        final Rectangle r = lblAvatar.getBounds();
        final int d = Math.min(r.width, r.height);
        return new Rectangle(r.x + (r.width - d) / 2, r.y + (r.height - d) / 2, d, d);
    }

    /** Behind the portrait: its sculpted plinth, with a sunburst that comes up and breathes while it is this player's turn. */
    private void paintArenaAvatarFrame(final Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final Rectangle c = getAvatarCircleBounds();
        if (c.width > 0) {
            final int cx = c.x + c.width / 2, cy = c.y + c.height / 2;
            final boolean active = !ArenaTheme.snapshotting && matchUI.getGameView() != null && matchUI.getGameView().getPlayerTurn() == player;
            if (ArenaTheme.snapshotting) {
                ArenaArt.paintPortraitFrame(g, cx, cy, c.width, false, mirror);
                return;
            }
            turnLight.to(active ? 1f : 0f, active ? 420 : 300);
            final float light = turnLight.get();
            ArenaArt.paintPortraitFrame(g, cx, cy, c.width, false, mirror);
            if (light > 0f) {
                final float breath = active && !turnLight.isMoving() ? 0.82f + 0.18f * ArenaMotion.pulse(avatarArea, 3f) : 1f;
                final java.awt.Composite solid = g.getComposite();
                g.setComposite(java.awt.AlphaComposite.SrcOver.derive(light * breath));
                ArenaArt.paintPortraitFrame(g, cx, cy, c.width, true, mirror);
                g.setComposite(solid);
            }
        }
    }

    /** Whether what is on top of the stack is aimed at this player. */
    private boolean isArenaStackTarget() {
        final GameView game = matchUI.getGameView();
        forge.game.spellability.StackItemView item = game == null ? null : game.peekStack();
        while (item != null) {
            if (item.getTargetPlayers() != null) {
                for (final PlayerView target : item.getTargetPlayers()) {
                    if (player.equals(target)) {
                        return true;
                    }
                }
            }
            item = item.getSubInstance();
        }
        return false;
    }

    /** Over the portrait: its ring, glowing when it can be targeted or is under the mouse, and the life total on its plate. */
    private void paintArenaLifeDisc(final Graphics2D g) {
        final Rectangle c = getAvatarCircleBounds();
        if (c.width <= 0) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color accent = null;
        if (isHighlighted()) {
            accent = ArenaTheme.CHOSEN;
        } else if (control.getMatchUI().isSelectable(player)) {
            accent = ArenaTheme.SELECTABLE;
        } else if (isArenaStackTarget()) {
            accent = new Color(0xBFEFFF);
        } else if (avatarHovered) {
            accent = ArenaTheme.HOVER;
        }
        final int cx = c.x + c.width / 2, cy = c.y + c.height / 2;
        final java.awt.Composite solid = g.getComposite();
        if (ArenaTheme.snapshotting) {
            ArenaArt.paintPortraitRing(g, cx, cy, c.width, accent);
        } else {
            // the ring's light comes up rather than being switched, and breathes while the player can be picked
            final boolean changed = accent == null ? ringAccent != null : !accent.equals(ringAccent);
            if (changed) {
                ringAccentWas = ringAccent;
                ringAccent = accent;
                ringMix.set(0f);
                ringMix.to(1f, 180);
            }
            final float mix = ringMix.get();
            final float breath = accent == ArenaTheme.SELECTABLE ? 0.6f + 0.4f * ArenaMotion.pulse(avatarArea, 1.5f) : 1f;
            if (mix < 1f || breath < 1f || accent == null) {
                ArenaArt.paintPortraitRing(g, cx, cy, c.width, mix < 1f ? ringAccentWas : null);
            }
            if (accent != null) {
                g.setComposite(java.awt.AlphaComposite.SrcOver.derive(mix * breath));
                ArenaArt.paintPortraitRing(g, cx, cy, c.width, accent);
                g.setComposite(solid);
            }
            final float flash = lifeFlash.get();
            if (flash > 0f) {
                g.setComposite(java.awt.AlphaComposite.SrcOver.derive(flash));
                ArenaArt.paintPortraitRing(g, cx, cy, c.width, lifeFlashColour);
                g.setComposite(solid);
            }
        }
        // a seat's portrait is small, and its life is what matters most about it
        final int plate = Math.max(22, Math.round(c.width * (arenaSeat ? 0.58f : 0.42f)));
        final int plateY = cy + c.width / 2;
        final java.awt.geom.AffineTransform flat = g.getTransform();
        final float popped = ArenaTheme.snapshotting ? 0f : lifePop.get();
        if (popped > 0f) {
            final float grow = 1f + 0.22f * popped;
            g.translate(cx, plateY);
            g.scale(grow, grow);
            g.translate(-cx, -plateY);
        }
        final String shownLife = arenaLifeSeen == Integer.MIN_VALUE ? lblLife.getText() : String.valueOf(arenaLifeShown);
        ArenaArt.paintLifePlate(g, cx, plateY, plate, shownLife, ArenaTheme.numeralFont(Font.BOLD, plate * 0.74f));
        g.setTransform(flat);
    }

    /** The life total counts its way to the new figure, the plate jumps, the ring flashes and the difference floats off. */
    private void noticeArenaLife(final int life) {
        if (arenaLifeSeen != Integer.MIN_VALUE && life != arenaLifeSeen && avatarArea.isShowing()) {
            final int delta = life - arenaLifeSeen;
            final int from = arenaLifeShown;
            if (lifeRoll != null) {
                lifeRoll.cancel();
            }
            lifeRoll = ArenaMotion.play(Math.min(800, 320 + Math.abs(delta) * 40), ArenaMotion.OUT, t -> {
                arenaLifeShown = Math.round(ArenaMotion.lerp(from, life, t));
                avatarArea.repaint();
            });
            lifePop.set(1f);
            lifePop.to(0f, 460);
            lifeFlashColour = delta < 0 ? LIFE_LOST : LIFE_GAINED;
            lifeFlash.set(1f);
            lifeFlash.to(0f, 560);
            final Rectangle c = getAvatarCircleBounds();
            ArenaFx.floatingNumber(avatarArea, new Rectangle(c.x, c.y, c.width, c.height), (delta > 0 ? "+" : "") + delta,
                    delta < 0 ? new Color(0xFF6A5C) : new Color(0x7BEA8B), Math.max(26f, c.width * 0.3f));
        } else if (lifeRoll == null || lifeRoll.isOver()) {
            arenaLifeShown = life;
        }
        arenaLifeSeen = life;
    }

    private void paintAITimeoutBar(final Graphics g) {
        if (player == null) { return; }
        final float f = AITimeoutTracker.getRemainingFraction(aiTimeoutKey());
        if (f < 0f) { return; }
        if (ArenaTheme.isEnabled()) {
            paintThinkingBubble((Graphics2D) g);
            return;
        }
        final Rectangle r = lblAvatar.getBounds();
        final int barH = Math.max(4, r.height / 10);
        final int y = r.y + r.height - barH;
        g.setColor(BAR_BG);
        g.fillRect(r.x, y, r.width, barH);
        g.setColor(f > 0.5f ? Color.GREEN : f > 0.25f ? BAR_AMBER : Color.RED);
        g.fillRect(r.x, y, Math.round(r.width * f), barH);
    }

    /** Arena shows an opponent who is deciding as a speech bubble with an ellipsis beside the portrait. */
    private void paintThinkingBubble(final Graphics2D g) {
        final Rectangle c = getAvatarCircleBounds();
        final int bh = Math.max(16, c.height / 4), bw = bh * 2;
        final int bx = Math.min(avatarArea.getWidth() - bw - 1, c.x + c.width - bw / 3);
        final int by = Math.max(0, c.y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(10, 12, 18, 225));
        g.fill(new RoundRectangle2D.Float(bx, by, bw, bh, bh, bh));
        g.setColor(Color.WHITE);
        final int dot = Math.max(3, bh / 5);
        // the dots ripple along, one after the other, while the opponent thinks
        final double beat = System.nanoTime() / 1e9d * 2d * Math.PI / 0.9d;
        for (int i = 0; i < 3; i++) {
            final int hop = (int) Math.round(Math.max(0d, Math.sin(beat - i * 0.9d)) * dot * 0.8d);
            g.fillOval(bx + bw / 2 - dot * 2 + i * dot * 3 / 2, by + (bh - dot) / 2 - hop, dot, dot);
        }
    }

    private static final Color LIFE_LOST = new Color(0xFF4A3C);
    private static final Color LIFE_GAINED = new Color(0x58E06C);
    private final ArenaMotion.Fade turnLight = new ArenaMotion.Fade(() -> avatarArea.repaint(), 0f);
    private final ArenaMotion.Fade seatLight = new ArenaMotion.Fade(() -> this.arenaSeatBack.repaint(), 0f);
    private final ArenaMotion.Fade ringMix = new ArenaMotion.Fade(() -> avatarArea.repaint(), 1f);
    private final ArenaMotion.Fade lifePop = new ArenaMotion.Fade(() -> avatarArea.repaint(), 0f);
    private final ArenaMotion.Fade lifeFlash = new ArenaMotion.Fade(() -> avatarArea.repaint(), 0f);
    private Color ringAccent;
    private Color ringAccentWas;
    private Color lifeFlashColour = LIFE_LOST;
    private int arenaLifeSeen = Integer.MIN_VALUE;
    private int arenaLifeShown;
    private ArenaMotion.Tween lifeRoll;

    private final PlayerDetailsPanel detailsPanel;

    // Avatar area
    private final FLabel lblAvatar     = new FLabel.Builder().fontAlign(SwingConstants.CENTER).iconScaleFactor(1.0f).build();
    private final FLabel lblLife       = new FLabel.Builder().fontAlign(SwingConstants.CENTER).fontStyle(Font.BOLD).build();
    private final FLabel lblPoison     = new FLabel.Builder().fontAlign(SwingConstants.CENTER).fontStyle(Font.BOLD).icon(FSkin.getImage(FSkinProp.IMG_POISON)).iconInBackground().build();
    private final FLabel lblEnergy     = new FLabel.Builder().fontAlign(SwingConstants.CENTER).fontStyle(Font.BOLD).icon(FSkin.getImage(FSkinProp.IMG_ENERGY)).iconInBackground().build();
    private final FLabel lblExperience = new FLabel.Builder().fontAlign(SwingConstants.CENTER).fontStyle(Font.BOLD).icon(FSkin.getImage(FSkinProp.IMG_EXPERIENCE)).iconInBackground().build();
    private final FLabel lblTicket     = new FLabel.Builder().fontAlign(SwingConstants.CENTER).fontStyle(Font.BOLD).icon(FSkin.getImage(FSkinProp.IMG_TICKET)).iconInBackground().build();
    private final FLabel lblRad        = new FLabel.Builder().fontAlign(SwingConstants.CENTER).fontStyle(Font.BOLD).icon(FSkin.getImage(FSkinProp.IMG_RAD)).iconInBackground().build();

    private final PhaseIndicator phaseIndicator = new PhaseIndicator();

    private final Border borderAvatarSimple = new LineBorder(new Color(0, 0, 0, 0), 1);
    private final Border borderAvatarHighlighted = new LineBorder(Color.red, 2);
    // dashed while the player can be picked as a target, solid red once picked
    private final Border borderAvatarSelectable = BorderFactory.createDashedBorder(new Color(255, 215, 0), 2f, 5f, 3f, false);

    private final TabDiffTracker tabDiff = new TabDiffTracker();


    //========= Constructor
    /**
     * Assembles Swing components of a player field instance.
     * 
     * @param p &emsp; {@link forge.game.player.Player}
     * @param id0 &emsp; {@link forge.gui.framework.EDocID}
     */
    public VField(final CMatchUI matchUI, final EDocID id0, final PlayerView p, final boolean mirror) {
        this.docID = id0;

        this.matchUI = matchUI;
        this.player = p;
        this.mirror = mirror;
        LIVE_FIELDS.add(this);
        if (p != null) { tab.setText(Localizer.getInstance().getMessage("lblPlayField", p.getName())); }
        else { tab.setText(Localizer.getInstance().getMessage("lblNoPlayerForEDocID", docID.toString())); }

        detailsPanel = new PlayerDetailsPanel(player, CMatchUI.FLOATING_ZONE_TYPES);

        // TODO player is hard-coded into tabletop...should be dynamic
        // (haven't looked into it too deeply). Doublestrike 12-04-12
        tabletop = new PlayArea(matchUI, scroller, mirror, player, ZoneType.Battlefield);

        control = new CField(matchUI, player, this);

        lblAvatar.setFocusable(false);
        lblLife.setFocusable(false);
        lblPoison.setFocusable(false);
        lblEnergy.setFocusable(false);
        lblExperience.setFocusable(false);
        lblTicket.setFocusable(false);
        lblRad.setFocusable(false);

        avatarArea.setOpaque(false);
        avatarArea.setBackground(FSkin.getColor(FSkin.Colors.CLR_HOVER));
        avatarArea.setLayout(new MigLayout("insets 0, gap 0"));
        if (ArenaTheme.isEnabled()) {
            avatarArea.setLayout(new MigLayout("insets 0, gap 0, hidemode 3"));
            lblLife.setVisible(false); // painted as a disc over the portrait
            avatarArea.add(lblAvatar, "w 100%!, h 100%-20px!, wrap, gaptop 2px");
        } else {
            avatarArea.add(lblAvatar, "w 100%-6px!, h 100%-23px!, wrap, gap 3 3 3 0");
        }
        avatarArea.add(lblLife, "w 100%!, h 20px!, wrap");

        // Player area hover effect
        avatarArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                if (ArenaTheme.isEnabled()) {
                    avatarHovered = true;
                    avatarArea.repaint();
                    return;
                }
                avatarArea.setOpaque(true);
                if (!isHighlighted()) {
                    avatarArea.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
                }
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                if (ArenaTheme.isEnabled()) {
                    avatarHovered = false;
                    avatarArea.repaint();
                    return;
                }
                avatarArea.setOpaque(false);
                if (!isHighlighted()) {
                    avatarArea.setBorder(control.getMatchUI().isSelectable(player) ? borderAvatarSelectable : borderAvatarSimple);
                }
            }
        });

        if (!ArenaTheme.isEnabled()) {
            tabletop.setBorder(new FSkin.MatteSkinBorder(0, 1, 0, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        }
        tabletop.setOpaque(false);

        scroller.setViewportView(this.tabletop);

        updateDetails();
    }

    @Override
    public void populate() {
        final JPanel pnl = parentCell.getBody();
        if (ArenaTheme.isEnabled()) {
            populateArena(pnl);
            return;
        }
        pnl.setLayout(new MigLayout("insets 0, gap 0"));

        pnl.add(avatarArea, "w 10%!, h 35%!");
        pnl.add(phaseIndicator, "w 5%!, h 100%!, span 1 2");
        pnl.add(scroller, "w 85%!, h 100%!, span 1 2, wrap");
        pnl.add(detailsPanel, "w 10%!, h 64%!, gapleft 1px");
    }

    /**
     * Arena layout: the battlefield fills the cell and everything about the player floats over
     * it. The portrait with its phase glyphs sits centred on the outer edge, with the back row of
     * permanents either side of it; the library and graveyard are in the corner of the window,
     * bottom left for the local player and top right for an opponent, whose hand hangs beside them.
     */
    private void populateArena(final JPanel pnl) {
        final Supplier<Iterable<PlayerView>> viewers = () -> matchUI.getLocalPlayers();
        final GameView gameView = matchUI.getGameView();
        final boolean commander = (gameView != null && gameView.isCommander())
                || (player.getCommanders() != null && !player.getCommanders().isEmpty());
        // opponents in a multiplayer game share the top of the board, each in a seat of their own
        arenaSeat = mirror && gameView != null && gameView.getPlayers().size() > 2;
        if (arenaLadder == null) {
            arenaLadder = phaseIndicator.createArenaLadder();
        }
        arenaZones = detailsPanel.createArenaZones(viewers, commander, arenaSeat, mirror);
        arenaCommanderDamage.setOpaque(false);
        arenaExtras = new JPanel(new MigLayout("insets 0, gap 6, ax center, ay center, hidemode 3"));
        arenaExtras.setOpaque(false);
        arenaExtras.add(arenaCommanderDamage);
        arenaExtras.add(detailsPanel.createArenaManaPool());
        updateArenaCommanderDamage();

        // the board fits itself to the window: no part of it ever scrolls
        scroller.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroller.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        avatarArea.setLayout(new ArenaAvatarLayout());
        pnl.setLayout(new ArenaFieldLayout());
        // what is added first is in front
        pnl.add(arenaName);
        pnl.add(arenaExtras);
        pnl.add(arenaZones);
        if (mirror) {
            arenaHandFan = detailsPanel.createArenaHandFan(viewers);
            pnl.add(arenaHandFan);
        }
        if (!arenaSeat) {
            pnl.add(arenaLadder[0]);
            pnl.add(arenaLadder[1]);
        }
        pnl.add(avatarArea);
        pnl.add(scroller);
        if (arenaSeat) {
            pnl.add(arenaSeatBack);
        }
    }

    /** Puts the floating parts of the player's area where they belong, sized from the window's height. */
    private final class ArenaFieldLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(final String name, final Component comp) { }

        @Override
        public void removeLayoutComponent(final Component comp) { }

        @Override
        public Dimension preferredLayoutSize(final Container c) {
            return c.getSize();
        }

        @Override
        public Dimension minimumLayoutSize(final Container c) {
            return new Dimension(0, 0);
        }

        @Override
        public void layoutContainer(final Container c) {
            final JPanel content = FView.SINGLETON_INSTANCE.getPnlContent();
            final int w = c.getWidth(), h = c.getHeight();
            final int wh = Math.max(1, content.getHeight());
            final int top = SwingUtilities.isDescendingFrom(c, content) ? SwingUtilities.convertPoint(c, 0, 0, content).y : 0;
            final int cx = w / 2;
            scroller.setBounds(0, 0, w, h);
            // a gutter between neighbouring seats
            final int gutter = Math.round(content.getWidth() * 0.0075f);
            arenaSeatBack.setBounds(gutter, 2, Math.max(0, w - 2 * gutter), Math.round(wh * 0.345f));

            final int d = Math.round(wh * (arenaSeat ? 0.066f : 0.125f));
            final int portraitY;
            if (!mirror) {
                portraitY = Math.round(wh * 0.778f) - top;
            } else if (arenaSeat) {
                portraitY = Math.round(wh * 0.008f) + d / 2 - top;
            } else {
                portraitY = Math.round(d * 0.62f) - top; // hangs from the top of the window
            }
            // big enough for the frame's sunburst; contains() keeps the mouse to the portrait itself
            final int areaW = Math.round(d * 4.5f);
            final int areaTop = portraitY - Math.round(d * 1.2f);
            arenaPortrait.setBounds(areaW / 2 - d / 2, portraitY - areaTop - d / 2, d, d);
            avatarArea.setBounds(cx - areaW / 2, areaTop, areaW, Math.round(d * 2.4f));
            avatarArea.revalidate();

            if (!arenaSeat) {
                final Dimension left = arenaLadder[0].getPreferredSize(), right = arenaLadder[1].getPreferredSize();
                arenaLadder[0].setBounds(cx - Math.round(d * 0.62f) - left.width, portraitY - left.height / 2, left.width, left.height);
                arenaLadder[1].setBounds(cx + Math.round(d * 0.62f), portraitY - right.height / 2, right.width, right.height);
            }

            final int cog = Math.round(wh * 0.075f); // the settings buttons keep the very corner
            final int nameH = Math.round(wh * 0.03f);
            if (!mirror) {
                final int zonesH = Math.round(wh * 0.135f);
                arenaZones.setBounds(Math.round(wh * 0.012f), Math.round(wh * 0.822f) - top, Math.round(w * 0.16f), zonesH);
                arenaName.setBounds(Math.round(wh * 0.014f), Math.round(wh * 0.962f) - top, Math.round(w * 0.16f), nameH);
            } else if (arenaSeat) {
                final int zonesH = Math.round(wh * 0.076f);
                final int zonesX = cx + Math.round(d * 0.7f);
                // the seat under the corner buttons stops short of them
                final int cellRight = SwingUtilities.isDescendingFrom(c, content) ? SwingUtilities.convertPoint(c, w, 0, content).x : w;
                final int reserve = cellRight >= content.getWidth() - 4 ? Math.max(Math.round(wh * 0.075f), Math.round(content.getWidth() * 0.06f)) : 14;
                arenaZones.setBounds(zonesX, Math.round(wh * 0.008f), Math.max(0, w - zonesX - reserve), zonesH);
                final int fanW = Math.round(wh * 0.1f);
                arenaHandFan.setBounds(cx - Math.round(d * 0.7f) - fanW, Math.round(wh * -0.012f), fanW, Math.round(wh * 0.085f));
                arenaName.setBounds(Math.round(content.getWidth() * 0.0075f) + 10, 8, Math.max(40, cx - Math.round(d * 0.7f) - fanW - 16), nameH);
            } else {
                // piles alone in the corner, clear of the settings buttons; the hand hangs beside the portrait
                final int zonesH = Math.round(wh * 0.118f);
                final int zonesW = Math.round(w * 0.15f);
                final int zonesRight = w - Math.max(cog, Math.round(w * 0.06f));
                arenaZones.setBounds(zonesRight - zonesW, Math.round(wh * 0.006f), zonesW, zonesH);
                final int fanW = Math.round(wh * 0.26f);
                arenaHandFan.setBounds(cx - Math.round(d * 1.6f) - Math.round(content.getWidth() * 0.015f) - fanW, Math.round(wh * -0.045f), fanW, Math.round(wh * 0.105f));
                arenaName.setBounds(Math.round(wh * 0.014f), Math.round(wh * 0.008f), Math.round(w * 0.2f), nameH);
            }

            final Dimension extras = arenaExtras.getPreferredSize();
            if (arenaSeat) {
                arenaExtras.setBounds(cx - extras.width / 2, Math.round(wh * 0.178f), extras.width, extras.height);
            } else {
                arenaExtras.setBounds(cx + Math.round(d * 1.7f), portraitY - extras.height / 2, extras.width, extras.height);
            }
        }
    }

    /** Inside the portrait's area: the portrait where the field put it, and any poison or energy chips beside its life plate. */
    private final class ArenaAvatarLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(final String name, final Component comp) { }

        @Override
        public void removeLayoutComponent(final Component comp) { }

        @Override
        public Dimension preferredLayoutSize(final Container c) {
            return c.getSize();
        }

        @Override
        public Dimension minimumLayoutSize(final Container c) {
            return new Dimension(0, 0);
        }

        @Override
        public void layoutContainer(final Container c) {
            final Rectangle r = arenaPortrait;
            lblAvatar.setBounds(r);
            int x = r.x + r.width + 4;
            final int y = r.y + r.height / 2;
            for (final Component comp : c.getComponents()) {
                if (comp == lblAvatar) {
                    continue;
                }
                if (comp == lblLife) {
                    comp.setBounds(0, 0, 0, 0); // the life total is painted on its plate
                } else {
                    comp.setBounds(x, y, 46, 22);
                    x += 48;
                }
            }
        }
    }

    /** The player's name in the corner on a dark plate, whatever the board behind it is doing. */
    private final JComponent arenaName = new JComponent() {
        @Override
        protected void paintComponent(final Graphics g0) {
            final Graphics2D g = (Graphics2D) g0.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(ArenaTheme.displayFont(Font.BOLD, Math.max(13f, getHeight() * 0.62f)));
                final FontMetrics fm = g.getFontMetrics();
                final String name = player.getName();
                final int h = getHeight() - 2, d = Math.max(6, h / 3);
                final int textX = 12 + d + 8;
                g.setColor(new Color(6, 7, 11, 150));
                g.fillRoundRect(0, 0, Math.min(getWidth() - 1, textX + fm.stringWidth(name) + 14), h, h, h);
                g.setColor(ArenaTheme.GOLD);
                final int mx = 12 + d / 2, my = h / 2;
                g.fillPolygon(new int[] {mx - d / 2, mx, mx + d / 2, mx}, new int[] {my, my - d / 2 - 1, my, my + d / 2 + 1}, 4);
                g.setColor(Color.WHITE);
                g.drawString(name, textX, (h + fm.getAscent() - fm.getDescent()) / 2);
            } finally {
                g.dispose();
            }
        }
    };

    /**
     * Each seat is a pane of its own: dark glass that fades out towards the middle of the board, so it
     * has sides and a top but no hard bottom edge. The seat whose turn it is has a gold rim and a pool of light.
     */
    private final JComponent arenaSeatBack = new JComponent() {
        @Override
        protected void paintComponent(final Graphics g0) {
            final int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            final boolean active = matchUI.getGameView() != null && matchUI.getGameView().getPlayerTurn() == player;
            seatLight.to(active ? 1f : 0f, active ? 420 : 300);
            final float light = seatLight.get();
            final Graphics2D g = (Graphics2D) g0.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                final int arc = Math.max(12, h / 14);
                final Color glass = new Color(6, 8, 13);
                g.setPaint(new java.awt.GradientPaint(0, 0, ArenaTheme.alpha(glass, 85), 0, h, ArenaTheme.alpha(glass, 0)));
                g.fillRoundRect(0, 0, w, h + arc, arc, arc);
                if (light > 0f) {
                    g.setPaint(new java.awt.RadialGradientPaint(w / 2f, 0f, w * 0.62f, new float[] {0f, 1f},
                            new Color[] {ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, Math.round(80 * light)), ArenaTheme.alpha(ArenaTheme.GOLD_BRIGHT, 0)}));
                    g.fillRect(0, 0, w, h);
                }
                if (light > 0f) {
                    final Color rim = new Color(0xFFC85A);
                    g.setPaint(new java.awt.GradientPaint(0, 0, ArenaTheme.alpha(rim, Math.round(245 * light)), 0, h * 0.86f, ArenaTheme.alpha(rim, 0)));
                    g.setStroke(new java.awt.BasicStroke(2.6f));
                    g.drawRoundRect(1, 1, w - 3, h + arc, arc, arc);
                }
            } finally {
                g.dispose();
            }
        }
    };

    /** Arena: where an arc aimed at this player ends, on screen: the rim of the portrait facing the board. */
    public Point getArenaTargetPoint() {
        final Point p = avatarArea.getLocationOnScreen();
        final int d = arenaPortrait.width;
        p.translate(arenaPortrait.x + d / 2, arenaPortrait.y + d / 2 + (mirror ? Math.round(d * 0.82f) : -Math.round(d * 0.56f)));
        return p;
    }

    @Override
    public EDocID getDocumentID() {
        return docID;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CField getLayoutControl() {
        return control;
    }

    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    public PlayArea getTabletop() {
        return this.tabletop;
    }

    public JPanel getAvatarArea() {
        return this.avatarArea;
    }

    public PhaseIndicator getPhaseIndicator() {
        return phaseIndicator;
    }

    public PlayerDetailsPanel getDetailsPanel() {
        return detailsPanel;
    }

    private boolean isHighlighted() {
        return control.getMatchUI().isHighlighted(player);
    }

    /** Solid red frame once the player is picked, dashed frame while the player can be picked, none otherwise. */
    public void updateAvatarBorder() {
        if (ArenaTheme.isEnabled()) {
            // the glow is painted with the portrait frame
            avatarArea.setBorder(borderAvatarSimple);
            avatarArea.repaint();
            return;
        }
        final boolean highlighted = isHighlighted();
        avatarArea.setBorder(highlighted ? borderAvatarHighlighted
                : control.getMatchUI().isSelectable(player) ? borderAvatarSelectable : borderAvatarSimple);
        avatarArea.setOpaque(highlighted);
    }

    public void setAvatar(final SkinImage avatar) {
        if (ArenaTheme.isEnabled() && avatar != null && avatar.getIcon() != null) {
            lblAvatar.setIcon(new FSkin.UnskinnedIcon(ArenaTheme.createCircleAvatar(avatar.getIcon().getImage(), 256)));
            lblAvatar.getResizeTimer().start();
            return;
        }
        lblAvatar.setIcon(avatar);
        lblAvatar.getResizeTimer().start();
    }

    public void updateManaPool() {
        detailsPanel.updateManaPool();
    }
    public void updateZones() {
        detailsPanel.updateZones();
    }

    public void updateTabLabel() {
        if (player == null) {
            return;
        }
        final int delta = tabDiff.update(tabletop.getCardPanels().size(), tab);
        String label = Localizer.getInstance().getMessage("lblPlayField", player.getName());
        if (delta != 0) {
            label += " (" + delta + " new)";
        }
        tab.setText(label);
        tab.setToolTipText(label);
    }

    private void addLblTicket() {
        if (lblTicket.isShowing() || lblExperience.isShowing() || lblEnergy.isShowing() || lblPoison.isShowing()) {
            return; // experience, energy, poison take precedence
        }
        avatarArea.remove(lblLife);
        lblLife.setIcon(FSkin.getImage(FSkinProp.ICO_QUEST_LIFE));
        avatarArea.add(lblLife, "w 50%!, h 20px!, split 2");
        avatarArea.add(lblTicket, "w 50%!, h 20px!, wrap");
    }

    private void removeLblTicket() {
        if (!lblTicket.isShowing()) {
            return;
        }
        avatarArea.remove(lblTicket);
        avatarArea.remove(lblLife);
        avatarArea.add(lblLife, "w 100%!, h 20px!, wrap");
    }

    private void addLblRad() {
        if (lblRad.isShowing() || lblExperience.isShowing() || lblEnergy.isShowing() || lblPoison.isShowing()) {
            return;
        }
        avatarArea.remove(lblLife);
        lblLife.setIcon(FSkin.getImage(FSkinProp.ICO_QUEST_LIFE));
        avatarArea.add(lblLife, "w 50%!, h 20px!, split 2");
        avatarArea.add(lblRad, "w 50%!, h 20px!, wrap");
    }

    private void removeLblRad() {
        if (!lblRad.isShowing()) {
            return;
        }
        avatarArea.remove(lblRad);
        avatarArea.remove(lblLife);
        avatarArea.add(lblLife, "w 100%!, h 20px!, wrap");
    }

    private void addLblExperience() {
        if (lblExperience.isShowing() || lblEnergy.isShowing() || lblPoison.isShowing()) {
            return; // energy and poison take precedence
        }
        avatarArea.remove(lblLife);
        lblLife.setIcon(FSkin.getImage(FSkinProp.ICO_QUEST_LIFE));
        avatarArea.add(lblLife, "w 50%!, h 20px!, split 2");
        avatarArea.add(lblExperience, "w 50%!, h 20px!, wrap");
    }

    private void removeLblExperience() {
        if (!lblExperience.isShowing()) {
            return;
        }
        avatarArea.remove(lblExperience);
        avatarArea.remove(lblLife);
        avatarArea.add(lblLife, "w 100%!, h 20px!, wrap");
    }

    private void addLblEnergy() {
        if (lblEnergy.isShowing() || lblPoison.isShowing()) {
            return; // poison takes precedence
        }
        avatarArea.remove(lblLife);
        lblLife.setIcon(FSkin.getImage(FSkinProp.ICO_QUEST_LIFE));
        avatarArea.add(lblLife, "w 50%!, h 20px!, split 2");
        avatarArea.add(lblEnergy, "w 50%!, h 20px!, wrap");
    }
    
    private void removeLblEnergy() {
        if (!lblEnergy.isShowing()) {
            return;
        }
        avatarArea.remove(lblEnergy);
        avatarArea.remove(lblLife);
        avatarArea.add(lblLife, "w 100%!, h 20px!, wrap");
    }

    private void addLblPoison() {
        if (lblPoison.isShowing()) {
            return;
        }
        avatarArea.remove(lblLife);
        lblLife.setIcon(FSkin.getImage(FSkinProp.ICO_QUEST_LIFE));
        avatarArea.add(lblLife, "w 50%!, h 20px!, split 2");
        avatarArea.add(lblPoison, "w 50%!, h 20px!, wrap");
    }
    private void removeLblPoison() {
        if (!lblPoison.isShowing()) {
            return;
        }
        avatarArea.remove(lblPoison);
        avatarArea.remove(lblLife);
        avatarArea.add(lblLife, "w 100%!, h 20px!, wrap");
    }

    public void updateDetails() {
        // Update life total
        final int life = player.getLife();
        lblLife.setText(String.valueOf(life));
        if (ArenaTheme.isEnabled()) {
            noticeArenaLife(life);
        }
        if (life > LIFE_CRITICAL) {
            lblLife.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        } else {
            lblLife.setForeground(Color.RED);
        }

        // Update poison and/or energy counters, poison counters take precedence
        final int poison = player.getCounters(CounterEnumType.POISON);
        final int energy = player.getCounters(CounterEnumType.ENERGY);
        final int experience = player.getCounters(CounterEnumType.EXPERIENCE);
        final int rad = player.getCounters(CounterEnumType.RAD);
        final int ticket = player.getCounters(CounterEnumType.TICKET);

        if (poison > 0) {
            removeLblEnergy();
            removeLblExperience();
            removeLblRad();
            removeLblTicket();
            addLblPoison();
            lblPoison.setText(String.valueOf(poison));
            if (poison < POISON_CRITICAL) {
                lblPoison.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            } else {
                lblPoison.setForeground(Color.RED);
            }
        } else {
            removeLblPoison();
        }

        if (energy > 0) {
            removeLblExperience();
            removeLblRad();
            removeLblTicket();
            if (poison == 0) {
                addLblEnergy();
                lblEnergy.setText(String.valueOf(energy));
            }
        } else {
            removeLblEnergy();
        }

        if (experience > 0) {
            removeLblRad();
            removeLblTicket();
            if (poison == 0 && energy == 0) {
                addLblExperience();
                lblExperience.setText(String.valueOf(experience));
            }
        } else {
            removeLblExperience();
        }

        if (rad > 0) {
            removeLblTicket();
            if (poison == 0 && energy == 0 && experience == 0) {
                addLblRad();
                lblRad.setText(String.valueOf(rad));
            }
        } else {
            removeLblRad();
        }

        if (ticket > 0) {
            if (poison == 0 && energy == 0 && experience == 0 && rad == 0) {
                addLblTicket();
                lblTicket.setText(String.valueOf(ticket));
            }
        } else {
            removeLblTicket();
        }

        updateAvatarBorder();
        updateArenaCommanderDamage();
        this.avatarArea.setToolTipText(getPlayerDetailsHtml());
    }

    /** One chip per opposing commander that has dealt this player combat damage (21 is lethal). */
    private void updateArenaCommanderDamage() {
        if (!ArenaTheme.isEnabled()) {
            return;
        }
        arenaCommanderDamage.removeAll();
        final GameView gameView = matchUI == null ? null : matchUI.getGameView();
        if (gameView != null && gameView.isCommander()) {
            for (final PlayerView opponent : gameView.getPlayers()) {
                if (opponent == player || opponent.getCommanders() == null) {
                    continue;
                }
                for (final CardView commander : opponent.getCommanders()) {
                    final int damage = player.getCommanderDamage(commander);
                    if (damage <= 0) {
                        continue;
                    }
                    final JLabel chip = new JLabel(Localizer.getInstance().getMessage("lblArenaCommanderDamage", damage)) {
                        @Override
                        protected void paintComponent(final Graphics g) {
                            final Graphics2D g2 = (Graphics2D) g;
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(new Color(10, 12, 18, 220));
                            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                            g2.setColor(damage >= 15 ? ArenaTheme.DANGER : ArenaTheme.GOLD_DIM);
                            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                            super.paintComponent(g);
                        }
                    };
                    chip.setFont(FSkin.getBoldFont(12).getBaseFont());
                    chip.setForeground(damage >= 15 ? new Color(0xFF8A8A) : ArenaTheme.TEXT);
                    chip.setHorizontalAlignment(SwingConstants.CENTER);
                    chip.setToolTipText(Localizer.getInstance().getMessage("lblArenaCommanderDamageTooltip",
                            CardTranslation.getTranslatedName(commander.getName()), damage));
                    arenaCommanderDamage.add(chip, "h 24!, w pref+16!");
                }
            }
        }
        arenaCommanderDamage.revalidate();
        arenaCommanderDamage.repaint();
    }

    private String getPlayerDetailsHtml() {
        final String detailsHtml = player.getDetailsHtml();
        final String commanderBracketLine = getCommanderBracketTooltipLine();
        if (commanderBracketLine == null) {
            return detailsHtml;
        }

        final String nameSeparator = "<hr/>";
        final int insertIndex = detailsHtml.indexOf(nameSeparator);
        if (insertIndex < 0) {
            return detailsHtml;
        }

        final int lineIndex = insertIndex + nameSeparator.length();
        return detailsHtml.substring(0, lineIndex)
                + commanderBracketLine + "<br/>"
                + detailsHtml.substring(lineIndex);
    }

    private String getCommanderBracketTooltipLine() {
        if (commanderBracketTooltipCalculated) {
            return commanderBracketTooltipLine;
        }

        commanderBracketTooltipCalculated = true;
        if (matchUI == null || matchUI.getGameView() == null || !matchUI.getGameView().isCommander()) {
            return null;
        }
        final GameType gameType = matchUI.getGameView().getGameType();
        if (gameType == GameType.Adventure || gameType == GameType.AdventureEvent) {
            return null;
        }
        final int maximumBracket = matchUI.getMaximumCommanderBracket();
        if (maximumBracket < 1 || maximumBracket > 4) {
            return null;
        }
        final Deck deck = matchUI.getDeckForPlayer(player);
        if (deck == null) {
            return null;
        }

        commanderBracketTooltipLine = Localizer.getInstance().getMessage("lblBracket")
                + ": " + CommanderBracketCalculator.getBracket(deck);
        return commanderBracketTooltipLine;
    }

}
