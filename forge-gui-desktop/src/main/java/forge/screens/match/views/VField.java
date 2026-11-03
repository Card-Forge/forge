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
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
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
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CField;
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
    private final SkinnedPanel avatarArea = new SkinnedPanel() {
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

    /** Arena portrait frame: a glow for target, hover and active-turn states. */
    private void paintArenaAvatarFrame(final Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final Rectangle c = getAvatarCircleBounds();
        Color glow = null;
        if (isHighlighted()) {
            glow = ArenaTheme.CHOSEN;
        } else if (control.getMatchUI().isSelectable(player)) {
            glow = ArenaTheme.SELECTABLE;
        } else if (avatarHovered) {
            glow = ArenaTheme.HOVER;
        } else if (matchUI.getGameView() != null && matchUI.getGameView().getPlayerTurn() == player) {
            glow = ArenaTheme.PLAYABLE;
        }
        if (glow != null && c.width > 0) {
            ArenaTheme.paintGlow(g, c.x, c.y, c.width, c.height, c.width, glow, Math.max(5, c.width / 7), 240);
        }
    }

    /** The life total in a dark disc overlapping the bottom of the portrait. */
    private void paintArenaLifeDisc(final Graphics2D g) {
        final Rectangle c = getAvatarCircleBounds();
        if (c.width <= 0) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        final int d = Math.max(26, Math.round(c.width * 0.46f));
        final int dx = c.x + (c.width - d) / 2;
        final int dy = Math.min(avatarArea.getHeight() - d - 1, c.y + c.height - d * 2 / 3);
        final Ellipse2D disc = new Ellipse2D.Float(dx, dy, d, d);
        g.setPaint(new GradientPaint(0, dy, new Color(0x1E2633), 0, dy + d, new Color(0x07090E)));
        g.fill(disc);
        g.setStroke(new BasicStroke(Math.max(1.5f, d / 18f)));
        g.setColor(ArenaTheme.GOLD);
        g.draw(disc);
        final String life = lblLife.getText();
        g.setFont(FSkin.getBoldFont(Math.max(12, Math.round(d * 0.5f))).getBaseFont());
        final FontMetrics fm = g.getFontMetrics();
        g.setColor(player.getLife() > LIFE_CRITICAL ? Color.WHITE : new Color(0xFF6B6B));
        g.drawString(life, dx + (d - fm.stringWidth(life)) / 2, dy + (d + fm.getAscent() - fm.getDescent()) / 2);
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
        for (int i = 0; i < 3; i++) {
            g.fillOval(bx + bw / 2 - dot * 2 + i * dot * 3 / 2, by + (bh - dot) / 2, dot, dot);
        }
    }

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
     * Arena layout: the battlefield fills the cell and a player bar runs along the outer
     * edge (top for opponents, bottom for the local player). The portrait sits in the
     * middle of the bar between the two halves of the phase ladder, the library,
     * graveyard and exile piles are on the left, and floating mana (plus an opponent's
     * hand of card backs) is on the right.
     */
    private void populateArena(final JPanel pnl) {
        final Supplier<Iterable<PlayerView>> viewers = () -> matchUI.getLocalPlayers();
        final GameView gameView = matchUI.getGameView();
        final boolean commander = (gameView != null && gameView.isCommander())
                || (player.getCommanders() != null && !player.getCommanders().isEmpty());
        // opponents in a multiplayer game share the top of the board, so their bars drop the phase ladder
        final boolean compact = mirror && gameView != null && gameView.getPlayers().size() > 2;
        if (arenaLadder == null) {
            arenaLadder = phaseIndicator.createArenaLadder();
        }

        final JPanel center = new JPanel(new MigLayout("insets 0, gap 8, ay center"));
        center.setOpaque(false);
        if (!compact) {
            center.add(arenaLadder[0]);
        }
        center.add(avatarArea, compact ? "w 92!, h 100%!" : "w 112!, h 100%!");
        if (!compact) {
            center.add(arenaLadder[1]);
        }

        arenaCommanderDamage.setOpaque(false);
        final JPanel right = new JPanel(new MigLayout("insets 0, gap 6, ax right, ay center, hidemode 3"));
        right.setOpaque(false);
        right.add(arenaCommanderDamage);
        right.add(detailsPanel.createArenaManaPool());
        if (mirror) {
            right.add(detailsPanel.createArenaHandFan(viewers), compact ? "w 96!, h 100%!" : "w 150!, h 100%!");
        }

        final String columns = compact ? "[40%][grow, center][34%]" : "[30%][grow, center][30%]";
        final JPanel bar = new JPanel(new MigLayout("insets 2 8 2 8, gap 0", columns, "[grow, fill]"));
        bar.setOpaque(false);
        bar.add(detailsPanel.createArenaZones(viewers, commander, compact), "al left, h 100%!");
        bar.add(center, "h 100%!");
        bar.add(right, "al right, h 100%!");
        updateArenaCommanderDamage();

        pnl.setLayout(new MigLayout("insets 0, gap 0, wrap 1"));
        if (mirror) {
            pnl.add(bar, "w 100%!, h 27%!");
            pnl.add(scroller, "w 100%!, h 73%!");
        } else {
            // the local battlefield is taller, so the same bar height sits lower on screen
            pnl.add(scroller, "w 100%!, h 76%!");
            pnl.add(bar, "w 100%!, h 24%!");
        }
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
