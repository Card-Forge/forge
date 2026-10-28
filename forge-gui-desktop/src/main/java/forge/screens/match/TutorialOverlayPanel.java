package forge.screens.match;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import forge.Singletons;
import forge.game.GameView;
import forge.game.player.PlayerView;
import forge.gamemodes.tutorial.TutorialTarget;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.framework.SDisplayUtil;
import forge.screens.match.views.VField;
import forge.screens.match.views.VHand;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FButton;
import forge.toolbox.FSkin;
import forge.toolbox.FTextArea;
import forge.util.Localizer;
import forge.view.FView;

/**
 * The desktop side of the "How to Play" tutorial's note and outline.
 *
 * <p>A transparent panel over the whole window: it dims everything except the parts of the match screen the tutorial is
 * pointing at and outlines them in gold, and it holds the coach note. The note is put where it covers none of the
 * outlined parts, on the opponent's side of the board when that is free, since that is the part of the screen the player
 * needs least. Mouse events pass straight through everywhere except on the note itself, so the game stays usable.
 */
@SuppressWarnings("serial")
class TutorialOverlayPanel extends JPanel {
    private static final Color DIM = new Color(0, 0, 0, 140);
    private static final Color GOLD = new Color(255, 209, 89);
    private static final Color GLOW = new Color(255, 209, 89, 90);
    private static final int MARGIN = 8;
    private static final int POLL_MS = 250;

    private final CMatchUI matchUI;
    private final Coach coach = new Coach();
    private final Timer watcher = new Timer(POLL_MS, e -> sync());
    private final ComponentAdapter resizer = new ComponentAdapter() {
        @Override
        public void componentResized(final ComponentEvent e) {
            fitToWindow();
            sync();
        }
    };

    private EnumSet<TutorialTarget> targets = EnumSet.noneOf(TutorialTarget.class);
    private List<Rectangle> holes = new ArrayList<>();
    private boolean attached;

    TutorialOverlayPanel(final CMatchUI matchUI0) {
        matchUI = matchUI0;
        setOpaque(false);
        setLayout(null);
        setFocusTraversalKeysEnabled(false);
        add(coach);
        coach.setVisible(false);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Attaching to the window
    // ---------------------------------------------------------------------------------------------------------

    private void attach() {
        if (attached) {
            return;
        }
        attached = true;
        final JComponent lpn = Singletons.getView().getLpnDocument();
        // index 0 puts it above the targeting arcs, which share its layer
        lpn.add(this, FView.TARGETING_LAYER, 0);
        lpn.addComponentListener(resizer);
        fitToWindow();
        watcher.start();
    }

    /** Takes the overlay off the window; safe to call more than once. */
    void detach() {
        watcher.stop();
        if (!attached) {
            return;
        }
        attached = false;
        final JComponent lpn = Singletons.getView().getLpnDocument();
        lpn.removeComponentListener(resizer);
        lpn.remove(this);
        lpn.repaint();
    }

    private void fitToWindow() {
        final Dimension size = Singletons.getView().getLpnDocument().getSize();
        setBounds(0, 0, size.width, size.height);
    }

    /** Only the note takes the mouse; everything else falls through to the game underneath. */
    @Override
    public boolean contains(final int x, final int y) {
        return coach.isVisible() && coach.getBounds().contains(x, y);
    }

    // ---------------------------------------------------------------------------------------------------------
    // What the tutorial asks for
    // ---------------------------------------------------------------------------------------------------------

    /** Shows the note. {@code onNext} runs when its button is pressed. Call on the UI thread. */
    void showCoach(final String title, final String text, final String nextLabel, final Runnable onNext) {
        attach();
        coach.show(title, text, nextLabel, onNext);
        sync();
    }

    /** Hides the note. Call on the UI thread. */
    void hideCoach() {
        coach.hidePanel();
        stopIfIdle();
    }

    /** Outlines the given parts of the screen; an empty collection clears the outline. Call on the UI thread. */
    void highlight(final EnumSet<TutorialTarget> next) {
        targets = next;
        if (!next.isEmpty()) {
            attach();
            bringPartsIntoView(next);
        }
        sync();
        stopIfIdle();
    }

    private void stopIfIdle() {
        if (targets.isEmpty() && !coach.isVisible()) {
            detach();
        }
    }

    /** A part that is on a tab behind another tab is brought forward so it can be seen. */
    private void bringPartsIntoView(final EnumSet<TutorialTarget> wanted) {
        if (wanted.contains(TutorialTarget.STACK)) {
            show(matchUI.getTutorialDoc(EDocID.REPORT_STACK));
        }
        if (wanted.contains(TutorialTarget.PROMPT) || wanted.contains(TutorialTarget.BUTTONS)) {
            show(matchUI.getTutorialDoc(EDocID.REPORT_MESSAGE));
        }
        if (wanted.contains(TutorialTarget.CARD_INFO)) {
            show(matchUI.getTutorialDoc(EDocID.CARD_PICTURE));
        }
        if (wanted.contains(TutorialTarget.MENU)) {
            show(matchUI.getTutorialDoc(EDocID.BUTTON_DOCK));
        }
    }

    private static void show(final IVDoc<? extends ICDoc> doc) {
        if (doc != null && doc.getParentCell() != null) {
            SDisplayUtil.showTab(doc);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Finding the parts on screen
    // ---------------------------------------------------------------------------------------------------------

    private PlayerView me() {
        return matchUI.getCurrentPlayer();
    }

    private PlayerView opponent() {
        final GameView game = matchUI.getGameView();
        final PlayerView me = me();
        if (game == null) {
            return null;
        }
        for (final PlayerView p : game.getPlayers()) {
            if (!Objects.equals(p, me)) {
                return p;
            }
        }
        return null;
    }

    private void addField(final List<Component> out, final PlayerView p, final boolean avatar, final boolean phases,
                          final boolean tabletop, final boolean details) {
        final VField field = p == null ? null : matchUI.getFieldViewFor(p);
        if (field == null) {
            return;
        }
        if (avatar) { out.add(field.getAvatarArea()); }
        if (phases) { out.add(field.getPhaseIndicator()); }
        if (details) { out.add(field.getDetailsPanel()); }
        if (tabletop) {
            final Component scroller = SwingUtilities.getAncestorOfClass(JScrollPane.class, field.getTabletop());
            out.add(scroller != null ? scroller : field.getTabletop());
        }
    }

    private static void addCell(final List<Component> out, final IVDoc<? extends ICDoc> doc) {
        if (doc == null) {
            return;
        }
        final DragCell cell = doc.getParentCell();
        if (cell != null) {
            out.add(cell.getBody());
        }
    }

    private List<Component> componentsFor(final TutorialTarget target) {
        final List<Component> out = new ArrayList<>();
        switch (target) {
            case YOUR_PLAYER -> addField(out, me(), true, false, false, false);
            case OPPONENT -> addField(out, opponent(), true, false, false, false);
            case HAND -> {
                final VHand hand = matchUI.getHandFor(me());
                if (hand != null) { out.add(hand.getHandArea()); }
            }
            case BATTLEFIELD -> addField(out, me(), false, false, true, false);
            case ZONES -> addField(out, me(), false, false, false, true);
            case STACK -> addCell(out, matchUI.getTutorialDoc(EDocID.REPORT_STACK));
            case PHASES -> {
                addField(out, me(), false, true, false, false);
                addField(out, opponent(), false, true, false, false);
            }
            case PROMPT -> {
                final VPrompt prompt = matchUI.getCPrompt().getView();
                out.add(prompt.getTarMessage());
            }
            case BUTTONS -> {
                final VPrompt prompt = matchUI.getCPrompt().getView();
                out.add(prompt.getBtnOK());
                out.add(prompt.getBtnCancel());
            }
            case MENU -> addCell(out, matchUI.getTutorialDoc(EDocID.BUTTON_DOCK));
            case CARD_INFO -> addCell(out, matchUI.getTutorialDoc(EDocID.CARD_PICTURE));
        }
        return out;
    }

    /** The outlined parts as rectangles in this panel's coordinates; parts that are not showing are left out. */
    private List<Rectangle> findHoles() {
        final List<Rectangle> found = new ArrayList<>();
        for (final TutorialTarget target : targets) {
            for (final Component c : componentsFor(target)) {
                if (c == null || !c.isShowing() || c.getWidth() <= 0 || c.getHeight() <= 0) {
                    continue;
                }
                final Rectangle r = SwingUtilities.convertRectangle(c, new Rectangle(0, 0, c.getWidth(), c.getHeight()), this);
                final Rectangle clipped = r.intersection(new Rectangle(0, 0, getWidth(), getHeight()));
                if (!clipped.isEmpty() && !found.contains(clipped)) {
                    found.add(clipped);
                }
            }
        }
        return found;
    }

    /** Re-reads where everything is and updates the outline and the note when something moved. */
    private void sync() {
        if (!attached) {
            return;
        }
        final List<Rectangle> now = findHoles();
        final boolean moved = !now.equals(holes);
        holes = now;
        if (coach.isVisible() && (moved || coach.needsPlacing())) {
            coach.place();
        }
        if (moved) {
            repaint();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Painting the outline
    // ---------------------------------------------------------------------------------------------------------

    @Override
    protected void paintComponent(final Graphics g) {
        if (holes.isEmpty()) {
            return;
        }
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final Area dim = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
            final List<Rectangle> padded = new ArrayList<>();
            for (final Rectangle r : holes) {
                final Rectangle p = new Rectangle(r.x - 3, r.y - 3, r.width + 6, r.height + 6);
                padded.add(p);
                dim.subtract(new Area(new RoundRectangle2D.Float(p.x, p.y, p.width, p.height, 8, 8)));
            }
            g2.setColor(DIM);
            g2.fill(dim);
            for (final Rectangle p : padded) {
                final RoundRectangle2D.Float outline = new RoundRectangle2D.Float(p.x, p.y, p.width, p.height, 8, 8);
                g2.setStroke(new BasicStroke(7f));
                g2.setColor(GLOW);
                g2.draw(outline);
                g2.setStroke(new BasicStroke(3f));
                g2.setColor(GOLD);
                g2.draw(outline);
            }
        } finally {
            g2.dispose();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // The note
    // ---------------------------------------------------------------------------------------------------------

    /** The floating note: title, text, and Next / Hide buttons. */
    private final class Coach extends JPanel {
        private static final int PAD = 10;
        private static final int BUTTON_H = 28;
        private static final int BUTTON_W = 96;
        private static final Color BACKDROP = new Color(20, 26, 34, 245);
        private static final Color EDGE = new Color(115, 133, 158, 245);
        private static final Color TITLE = new Color(255, 209, 89);

        private final FTextArea body = new FTextArea();
        private final JScrollPane scroll = new JScrollPane(body);
        private final FButton btnNext = new FButton();
        private final FButton btnHide = new FButton();
        private String title = "";
        private boolean collapsed;
        private Runnable onNext;
        private boolean placed;

        Coach() {
            setOpaque(false);
            setLayout(null);
            body.setFont(FSkin.getRelativeFont(2));
            body.setForeground(Color.WHITE);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setBorder(null);
            scroll.setFocusable(false);
            btnNext.setFocusable(false);
            btnHide.setFocusable(false);
            btnNext.addActionListener(e -> next());
            btnHide.addActionListener(e -> {
                collapsed = !collapsed;
                updateButtons();
                place();
            });
            add(scroll);
            add(btnNext);
            add(btnHide);
        }

        private String msg(final String key) {
            return Localizer.getInstance().getMessage(key);
        }

        void show(final String title0, final String text, final String nextLabel, final Runnable onNext0) {
            title = title0 == null ? "" : title0;
            onNext = onNext0;
            collapsed = false;
            body.setText(text == null ? "" : text);
            body.setCaretPosition(0);
            btnNext.setText(nextLabel == null || nextLabel.isEmpty() ? msg("btnTutNext") : nextLabel);
            btnNext.setEnabled(true);
            updateButtons();
            setVisible(true);
            placed = false;
        }

        void hidePanel() {
            onNext = null;
            setVisible(false);
        }

        private void updateButtons() {
            btnHide.setText(msg(collapsed ? "btnTutShow" : "btnTutHide"));
        }

        private void next() {
            final Runnable r = onNext;
            onNext = null;
            if (r != null) {
                btnNext.setEnabled(false); // answered; the tutorial shows the next note when it is ready
                r.run();
            }
        }

        boolean needsPlacing() {
            return !placed;
        }

        /** Sizes the note to its text and moves it where it covers none of the outlined parts. */
        void place() {
            final int areaW = TutorialOverlayPanel.this.getWidth(), areaH = TutorialOverlayPanel.this.getHeight();
            if (areaW <= 0 || areaH <= 0) {
                return;
            }
            final int width = Math.max(300, Math.min(460, areaW / 3));
            final FontMetrics tm = getFontMetrics(FSkin.getBoldFont(2).getBaseFont());
            final int titleH = tm.getHeight() + 4;
            final int textW = width - 2 * PAD;
            int height;
            if (collapsed) {
                scroll.setVisible(false);
                height = PAD + BUTTON_H + PAD;
            } else {
                scroll.setVisible(true);
                body.setSize(new Dimension(textW, 1));
                final int wanted = body.getPreferredSize().height;
                final int maxBody = Math.max(120, Math.min(areaH / 2, 340));
                final int bodyH = Math.min(wanted, maxBody);
                height = PAD + titleH + bodyH + PAD + BUTTON_H + PAD;
            }

            final Rectangle spot = chooseSpot(width, height, areaW, areaH);
            setBounds(spot);
            final int buttonsY = height - PAD - BUTTON_H;
            btnNext.setBounds(width - PAD - BUTTON_W, buttonsY, BUTTON_W, BUTTON_H);
            btnHide.setBounds(width - 2 * (PAD + BUTTON_W) + PAD, buttonsY, BUTTON_W, BUTTON_H);
            scroll.setBounds(PAD, PAD + titleH, textW, Math.max(0, buttonsY - PAD - (PAD + titleH)));
            placed = true;
            TutorialOverlayPanel.this.repaint();
        }

        /**
         * Candidate spots, in the order they are preferred: the opponent's side of the board, then the corners and the
         * middle of the window. The first one clear of every outlined part wins; if none is clear, the one that covers
         * the least of them.
         */
        private Rectangle chooseSpot(final int w, final int h, final int areaW, final int areaH) {
            final List<Rectangle> candidates = new ArrayList<>();
            final PlayerView opp = opponent();
            final VField oppField = opp == null ? null : matchUI.getFieldViewFor(opp);
            if (oppField != null && oppField.getTabletop().isShowing()) {
                final Rectangle r = SwingUtilities.convertRectangle(oppField.getTabletop(),
                        new Rectangle(0, 0, oppField.getTabletop().getWidth(), oppField.getTabletop().getHeight()),
                        TutorialOverlayPanel.this);
                candidates.add(new Rectangle(r.x + (r.width - w) / 2, r.y + MARGIN, w, h));
            }
            final int cx = (areaW - w) / 2, right = areaW - w - MARGIN, bottom = areaH - h - MARGIN;
            candidates.add(new Rectangle(cx, MARGIN, w, h));
            candidates.add(new Rectangle(right, MARGIN, w, h));
            candidates.add(new Rectangle(MARGIN, MARGIN, w, h));
            candidates.add(new Rectangle(cx, (areaH - h) / 2, w, h));
            candidates.add(new Rectangle(cx, bottom, w, h));
            candidates.add(new Rectangle(right, bottom, w, h));
            candidates.add(new Rectangle(MARGIN, bottom, w, h));

            final Rectangle window = new Rectangle(0, 0, areaW, areaH);
            Rectangle best = null;
            long bestOverlap = Long.MAX_VALUE;
            for (final Rectangle c : candidates) {
                final Rectangle inside = new Rectangle(Math.max(0, Math.min(c.x, areaW - w)),
                        Math.max(0, Math.min(c.y, areaH - h)), w, h);
                if (!window.contains(inside)) {
                    continue;
                }
                long overlap = 0;
                for (final Rectangle hole : holes) {
                    final Rectangle i = inside.intersection(new Rectangle(hole.x - 4, hole.y - 4, hole.width + 8, hole.height + 8));
                    if (!i.isEmpty()) {
                        overlap += (long) i.width * i.height;
                    }
                }
                if (overlap == 0) {
                    return inside;
                }
                if (overlap < bestOverlap) {
                    bestOverlap = overlap;
                    best = inside;
                }
            }
            return best != null ? best : new Rectangle(MARGIN, MARGIN, w, h);
        }

        @Override
        protected void paintComponent(final Graphics g) {
            final Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                final int w = getWidth(), h = getHeight();
                g2.setColor(EDGE);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(BACKDROP);
                g2.fillRoundRect(1, 1, w - 2, h - 2, 13, 13);
                g2.setColor(TITLE);
                g2.setFont(FSkin.getBoldFont(2).getBaseFont());
                final FontMetrics fm = g2.getFontMetrics();
                final int titleWidth = collapsed ? w - 2 * (PAD + BUTTON_W) - PAD : w - 2 * PAD;
                final int y = collapsed ? h - PAD - BUTTON_H / 2 + fm.getAscent() / 2 - 2 : PAD + fm.getAscent();
                g2.drawString(fit(title, fm, titleWidth), PAD, y);
            } finally {
                g2.dispose();
            }
        }

        private String fit(final String text, final FontMetrics fm, final int maxWidth) {
            if (fm.stringWidth(text) <= maxWidth) {
                return text;
            }
            String t = text;
            while (t.length() > 1 && fm.stringWidth(t + "…") > maxWidth) {
                t = t.substring(0, t.length() - 1);
            }
            return t + "…";
        }
    }
}
