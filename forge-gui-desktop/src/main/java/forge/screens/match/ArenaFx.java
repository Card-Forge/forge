package forge.screens.match;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.screens.match.views.VField;
import forge.toolbox.ArenaMotion;
import forge.toolbox.ArenaTheme;
import forge.toolbox.special.ArenaZonePile;
import forge.view.FView;
import forge.view.arcane.CardPanel;
import forge.view.arcane.HandArea;

/**
 * A pane over the Arena board for what only exists for a moment: cards on their way from one zone to
 * another, numbers floating off what was hurt or healed, the ripple of something landing, the banner
 * for a new turn. It takes no clicks, and costs nothing while it is empty.
 * <p>
 * A card moving between zones is seen by its old zone and its new one separately, and in either order,
 * so both only report here. A moment later, once the batch of updates is over, departures and arrivals
 * are paired up by card and the flights are worked out.
 */
@SuppressWarnings("serial")
public final class ArenaFx extends JComponent {
    private static final int FLIGHT_MS = 340;
    private static final int STAGGER_MS = 70;
    /** A board wipe or a loaded game is too much to send flying. */
    private static final int MOST_FLIGHTS = 16;
    private static final int GATHER_MS = 40;
    private static ArenaFx instance;

    private final List<Sprite> sprites = new ArrayList<>();
    private final List<Leaving> leaving = new ArrayList<>();
    private final List<CardPanel> arriving = new ArrayList<>();
    private final List<Held> held = new ArrayList<>();
    private final Timer gather = new Timer(GATHER_MS, e -> settleMoves());

    private ArenaFx() {
        setOpaque(false);
        gather.setRepeats(false);
    }

    private static ArenaFx get() {
        if (instance == null) {
            instance = new ArenaFx();
            final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
            lpn.add(instance, FView.TARGETING_LAYER);
            lpn.moveToFront(instance);
            instance.setBounds(0, 0, lpn.getWidth(), lpn.getHeight());
            lpn.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(final ComponentEvent e) {
                    instance.setBounds(0, 0, lpn.getWidth(), lpn.getHeight());
                }
            });
        }
        return instance;
    }

    /** Clicks go straight through to the board. */
    @Override
    public boolean contains(final int x, final int y) {
        return false;
    }

    @Override
    protected void paintComponent(final Graphics g0) {
        if (sprites.isEmpty() || ArenaTheme.snapshotting) {
            return;
        }
        final Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            for (final Sprite sprite : sprites) {
                sprite.paint(g);
            }
        } finally {
            g.dispose();
        }
    }

    // ---- what is on the pane

    private abstract class Sprite {
        float t;

        /** What it covers at the moment; everything it paints stays inside this. */
        abstract Rectangle bounds();

        abstract void paint(Graphics2D g);

        void start(final int millis, final int delay, final ArenaMotion.Ease ease, final Runnable done) {
            sprites.add(this);
            ArenaMotion.play(millis, delay, ease, this::moveTo, () -> {
                sprites.remove(this);
                repaintArea(bounds());
                if (done != null) {
                    done.run();
                }
            });
        }

        void moveTo(final float next) {
            final Rectangle was = bounds();
            t = next;
            repaintArea(was.union(bounds()));
        }
    }

    private void repaintArea(final Rectangle area) {
        repaint(area.x - 2, area.y - 2, area.width + 4, area.height + 4);
    }

    /** A card that has left its zone, still standing where it was until it is known where it went. */
    private final class Held extends Sprite {
        private final BufferedImage image;
        private final Rectangle at;

        Held(final BufferedImage image, final Rectangle at) {
            this.image = image;
            this.at = at;
        }

        @Override
        Rectangle bounds() {
            return at;
        }

        @Override
        void paint(final Graphics2D g) {
            g.drawImage(image, at.x, at.y, at.width, at.height, null);
        }
    }

    /** A picture of a card, going from one place to another along a slight arc. */
    private final class Flight extends Sprite {
        private final BufferedImage image;
        private final float marginX, marginY; // how much of the picture lies outside the card, as a share of the card
        private final Rectangle from, to;
        private final float fadeInUntil, fadeOutFrom;
        private final float arc;

        Flight(final BufferedImage image, final float marginX, final float marginY, final Rectangle from, final Rectangle to,
                final float fadeInUntil, final float fadeOutFrom) {
            this.image = image;
            this.marginX = marginX;
            this.marginY = marginY;
            this.from = from;
            this.to = to;
            this.fadeInUntil = fadeInUntil;
            this.fadeOutFrom = fadeOutFrom;
            this.arc = (float) Math.hypot(to.x - from.x, to.y - from.y) * 0.1f;
        }

        private Rectangle card() {
            final int w = Math.round(ArenaMotion.lerp(from.width, to.width, t)), h = Math.round(ArenaMotion.lerp(from.height, to.height, t));
            final int x = Math.round(ArenaMotion.lerp(from.x, to.x, t));
            final int y = Math.round(ArenaMotion.lerp(from.y, to.y, t) - arc * (float) Math.sin(Math.PI * t));
            return new Rectangle(x, y, w, h);
        }

        private Rectangle picture() {
            final Rectangle c = card();
            final int mx = Math.round(c.width * marginX), my = Math.round(c.height * marginY);
            return new Rectangle(c.x - mx, c.y - my, c.width + 2 * mx, c.height + 2 * my);
        }

        @Override
        Rectangle bounds() {
            return picture();
        }

        @Override
        void paint(final Graphics2D g) {
            final float in = fadeInUntil <= 0f ? 1f : ArenaMotion.clamp01(t / fadeInUntil);
            final float out = fadeOutFrom >= 1f ? 1f : ArenaMotion.clamp01((1f - t) / (1f - fadeOutFrom));
            final float alpha = Math.min(in, out);
            if (alpha <= 0f) {
                return;
            }
            final Rectangle p = picture();
            final Graphics2D c = (Graphics2D) g.create();
            c.setComposite(AlphaComposite.SrcOver.derive(alpha));
            c.drawImage(image, p.x, p.y, p.width, p.height, null);
            c.dispose();
        }
    }

    /** A card that has gone nowhere that can be shown: it lifts a little and fades away. */
    private final class Dissolve extends Sprite {
        private final BufferedImage image;
        private final Rectangle at;

        Dissolve(final BufferedImage image, final Rectangle at) {
            this.image = image;
            this.at = at;
        }

        @Override
        Rectangle bounds() {
            final int rise = Math.round(at.height * 0.06f);
            return new Rectangle(at.x, at.y - rise, at.width, at.height + rise);
        }

        @Override
        void paint(final Graphics2D g) {
            final Graphics2D c = (Graphics2D) g.create();
            c.setComposite(AlphaComposite.SrcOver.derive(ArenaMotion.clamp01(1f - t)));
            c.drawImage(image, at.x, at.y - Math.round(at.height * 0.06f * t), at.width, at.height, null);
            c.dispose();
        }
    }

    /** A number rising off what it happened to. */
    private final class FloatingNumber extends Sprite {
        private final String text;
        private final Color colour;
        private final int cx, cy;
        private final float size;

        FloatingNumber(final String text, final Color colour, final int cx, final int cy, final float size) {
            this.text = text;
            this.colour = colour;
            this.cx = cx;
            this.cy = cy;
            this.size = size;
        }

        @Override
        Rectangle bounds() {
            final int w = Math.round(size * (text.length() + 2)), h = Math.round(size * 2.2f);
            return new Rectangle(cx - w / 2, cy - h - Math.round(size * 1.6f), w, h + Math.round(size * 2f));
        }

        @Override
        void paint(final Graphics2D g) {
            final float appear = ArenaMotion.clamp01(t / 0.12f);
            final float alpha = appear * ArenaMotion.clamp01((1f - t) / 0.35f);
            final float grow = 1f + 0.35f * ArenaMotion.pop(ArenaMotion.clamp01(t / 0.3f));
            final Graphics2D c = (Graphics2D) g.create();
            c.setComposite(AlphaComposite.SrcOver.derive(alpha));
            c.setFont(ArenaTheme.numeralFont(Font.BOLD, size * grow));
            final FontMetrics fm = c.getFontMetrics();
            final float x = cx - fm.stringWidth(text) / 2f, y = cy - size * 1.5f * ArenaMotion.OUT.at(t);
            c.setColor(new Color(0, 0, 0, 200));
            for (final int[] off : new int[][] {{-2, 0}, {2, 0}, {0, -2}, {0, 2}, {1, 2}, {-1, 2}}) {
                c.drawString(text, x + off[0], y + off[1]);
            }
            c.setColor(colour);
            c.drawString(text, x, y);
            c.dispose();
        }
    }

    /** A ring spreading out from where something landed. */
    private final class Ripple extends Sprite {
        private final int cx, cy, reach;
        private final Color colour;

        Ripple(final int cx, final int cy, final int reach, final Color colour) {
            this.cx = cx;
            this.cy = cy;
            this.reach = reach;
            this.colour = colour;
        }

        @Override
        Rectangle bounds() {
            final int r = reach + 8;
            return new Rectangle(cx - r, cy - r / 2, 2 * r, r);
        }

        @Override
        void paint(final Graphics2D g) {
            final float grown = ArenaMotion.lerp(0.45f, 1f, t) * reach;
            final Graphics2D c = (Graphics2D) g.create();
            c.setComposite(AlphaComposite.SrcOver.derive(0.75f * ArenaMotion.clamp01(1f - t)));
            c.setColor(colour);
            c.setStroke(new BasicStroke(ArenaMotion.lerp(5f, 1f, t)));
            // it lies flat on the board
            c.drawOval(Math.round(cx - grown), Math.round(cy - grown * 0.42f), Math.round(2 * grown), Math.round(2 * grown * 0.42f));
            c.dispose();
        }
    }

    /** Words on a band across the board, going past on their way somewhere else. */
    private final class Banner extends Sprite {
        private static final float IN_UNTIL = 0.2f, OUT_FROM = 0.8f;
        private final String text;
        private final Color accent;
        private float shownT = -1f;

        Banner(final String text, final Color accent) {
            this.text = text;
            this.accent = accent;
        }

        @Override
        Rectangle bounds() {
            final int h = bandHeight() + 4;
            return new Rectangle(0, bandY() - 2, ArenaFx.this.getWidth(), h);
        }

        private int bandHeight() {
            return Math.max(44, Math.round(ArenaFx.this.getHeight() * 0.1f));
        }

        private int bandY() {
            return Math.round(ArenaFx.this.getHeight() * 0.44f) - bandHeight() / 2;
        }

        /** It only moves coming and going, so that in between it costs nothing. */
        @Override
        void moveTo(final float next) {
            t = next;
            if (next < IN_UNTIL || next > OUT_FROM || shownT < IN_UNTIL) {
                shownT = next;
                repaintArea(bounds());
            }
        }

        @Override
        void paint(final Graphics2D g) {
            final float in = ArenaMotion.OUT.at(ArenaMotion.clamp01(t / IN_UNTIL));
            final float out = ArenaMotion.clamp01((t - OUT_FROM) / (1f - OUT_FROM));
            final float alpha = in * (1f - out);
            if (alpha <= 0f) {
                return;
            }
            final int w = ArenaFx.this.getWidth(), h = bandHeight(), y = bandY();
            final Graphics2D c = (Graphics2D) g.create();
            c.setComposite(AlphaComposite.SrcOver.derive(alpha));
            final float[] stops = {0f, 0.28f, 0.72f, 1f};
            final Color dark = new Color(4, 6, 10, 215), clear = new Color(4, 6, 10, 0);
            c.setPaint(new LinearGradientPaint(0f, 0f, w, 0f, stops, new Color[] {clear, dark, dark, clear}));
            c.fillRect(0, y, w, h);
            final Color line = ArenaTheme.alpha(accent, 235), noLine = ArenaTheme.alpha(accent, 0);
            c.setPaint(new LinearGradientPaint(0f, 0f, w, 0f, stops, new Color[] {noLine, line, line, noLine}));
            c.fillRect(0, y, w, 2);
            c.fillRect(0, y + h - 2, w, 2);
            c.setFont(ArenaTheme.displayFont(Font.BOLD, h * 0.56f));
            final FontMetrics fm = c.getFontMetrics();
            final float x = (w - fm.stringWidth(text)) / 2f + (1f - in) * -w * 0.03f + out * w * 0.03f;
            final float ty = y + (h + fm.getAscent() - fm.getDescent()) / 2f;
            c.setColor(new Color(0, 0, 0, 190));
            c.drawString(text, x + 1, ty + 2);
            c.setColor(Color.WHITE);
            c.drawString(text, x, ty);
            c.dispose();
        }
    }

    // ---- reported from the board

    /** A hand or battlefield is about to let go of this panel. Call it while the panel can still be painted. */
    public static void noteLeaving(final CardPanel panel) {
        if (!ArenaTheme.isMatchBoard() || panel.getCard() == null || !panel.isShowing() || !panel.isDisplayEnabled()) {
            return;
        }
        final ArenaFx fx = get();
        if (fx.leaving.size() >= MOST_FLIGHTS) {
            return;
        }
        final BufferedImage picture = panel.arenaSnapshot();
        final Rectangle pictureAt = fx.panelRect(panel);
        fx.leaving.add(new Leaving(panel.getMatchUI(), panel.getCard(), picture, fx.cardRect(panel), pictureAt,
                panel.getParent() instanceof HandArea area && area.isFanExtra(panel)));
        final Held held = fx.new Held(picture, pictureAt);
        fx.held.add(held);
        fx.sprites.add(held);
        fx.repaintArea(pictureAt);
        fx.gather.start();
    }

    /** A panel has just been made for a card new to its zone; it stays out of sight until it is known how it arrives. */
    public static void noteArriving(final CardPanel panel) {
        if (!ArenaTheme.isMatchBoard()) {
            return;
        }
        final ArenaFx fx = get();
        panel.setArenaHidden(true);
        fx.arriving.add(panel);
        fx.gather.start();
    }

    private record Leaving(CMatchUI matchUI, CardView card, BufferedImage picture, Rectangle cardAt, Rectangle pictureAt, boolean fromBesideHand) { }

    private Rectangle cardRect(final CardPanel panel) {
        final Point p = SwingUtilities.convertPoint(panel.getParent(), panel.getCardX(), panel.getCardY(), this);
        return new Rectangle(p.x, p.y, panel.getCardWidth(), panel.getCardHeight());
    }

    private Rectangle panelRect(final CardPanel panel) {
        return SwingUtilities.convertRectangle(panel.getParent(), panel.getBounds(), this);
    }

    private void settleMoves() {
        final List<CardPanel> arrivals = new ArrayList<>(arriving);
        final List<Leaving> departures = new ArrayList<>(leaving);
        arriving.clear();
        leaving.clear();
        for (final Held h : held) {
            sprites.remove(h);
            repaintArea(h.bounds());
        }
        held.clear();
        final boolean tooMany = arrivals.size() > MOST_FLIGHTS;
        int stagger = 0;
        for (final CardPanel panel : arrivals) {
            final CardView card = panel.getCard();
            if (card == null) {
                continue; // gone again already
            }
            if (tooMany || !panel.isShowing()) {
                panel.setArenaHidden(false);
                continue;
            }
            final Leaving from = takeLeaving(departures, card);
            if (from != null) {
                flyToPanel(from.picture(), from.pictureAt(), from.cardAt(), panel, 0);
            } else if (ZoneType.Hand.equals(card.getZone())) {
                drawIntoHand(panel, stagger);
                stagger += STAGGER_MS;
            } else {
                final ArenaFocusCard.Shown shown = ArenaFocusCard.lastShown(card);
                if (shown != null && shown.image() != null) {
                    final Rectangle at = SwingUtilities.convertRectangle(FView.SINGLETON_INSTANCE.getLpnDocument(), shown.at(), this);
                    flyToPanel(shown.image(), at, at, panel, 0);
                } else {
                    panel.arenaEnter();
                }
            }
        }
        for (final Leaving gone : departures) {
            sendOff(gone);
        }
    }

    private static Leaving takeLeaving(final List<Leaving> departures, final CardView card) {
        for (final Iterator<Leaving> it = departures.iterator(); it.hasNext();) {
            final Leaving candidate = it.next();
            if (candidate.card().getId() == card.getId()) {
                it.remove();
                return candidate;
            }
        }
        return null;
    }

    /** The picture goes to where the panel stands, which fades in under it as it arrives. */
    private void flyToPanel(final BufferedImage picture, final Rectangle pictureAt, final Rectangle cardAt, final CardPanel panel, final int delay) {
        final Rectangle to = cardRect(panel);
        final Rectangle start = new Rectangle(cardAt);
        final float mx = Math.max(0f, (cardAt.x - pictureAt.x) / (float) Math.max(1, cardAt.width));
        final float my = Math.max(0f, (cardAt.y - pictureAt.y) / (float) Math.max(1, cardAt.height));
        new Flight(picture, mx, my, start, to, 0f, 0.72f).start(FLIGHT_MS, delay, ArenaMotion.IN_OUT, null);
        panel.arenaRevealAfter(delay + Math.round(FLIGHT_MS * 0.6f), Math.round(FLIGHT_MS * 0.4f));
    }

    /** A card drawn: it comes off the top of the library, small, and takes its place in the hand. */
    private void drawIntoHand(final CardPanel panel, final int delay) {
        final PlayerView player = panel.getCard().getController() != null ? panel.getCard().getController() : panel.getCard().getOwner();
        final Rectangle pile = pileRect(panel.getMatchUI(), player, ZoneType.Library);
        if (pile == null) {
            panel.arenaEnter();
            return;
        }
        final Rectangle to = cardRect(panel);
        panel.setArenaHidden(false);
        final BufferedImage picture = panel.arenaSnapshot();
        panel.setArenaHidden(true);
        final Rectangle whole = panelRect(panel);
        final float mx = (to.x - whole.x) / (float) Math.max(1, to.width), my = (to.y - whole.y) / (float) Math.max(1, to.height);
        new Flight(picture, mx, my, pile, to, 0.15f, 0.8f).start(FLIGHT_MS + 60, delay, ArenaMotion.IN_OUT, null);
        panel.arenaRevealAfter(delay + Math.round((FLIGHT_MS + 60) * 0.7f), Math.round((FLIGHT_MS + 60) * 0.3f));
    }

    /** A card with no panel to go to: to its pile, or the large card for a spell, or else it just fades. */
    private void sendOff(final Leaving gone) {
        final CardView card = gone.card();
        final ZoneType zone = card.getZone();
        Rectangle to = null;
        ArenaZonePile pile = null;
        if (zone == ZoneType.Stack) {
            to = ArenaFocusCard.getCardBounds(card);
            if (to != null) {
                to = SwingUtilities.convertRectangle(FView.SINGLETON_INSTANCE.getLpnDocument(), to, this);
            }
        } else if (zone != null && !gone.fromBesideHand()) {
            // cast from beside the hand it flies like any spell; merely no longer castable, it just goes
            pile = pile(gone.matchUI(), card.getOwner(), zone);
            to = pile == null ? null : boundsOf(pile);
        }
        if (to == null) {
            new Dissolve(gone.picture(), gone.pictureAt()).start(240, 0, ArenaMotion.OUT, null);
            return;
        }
        final Rectangle cardAt = gone.cardAt(), pictureAt = gone.pictureAt();
        final float mx = (cardAt.x - pictureAt.x) / (float) Math.max(1, cardAt.width), my = (cardAt.y - pictureAt.y) / (float) Math.max(1, cardAt.height);
        final ArenaZonePile landedOn = pile;
        new Flight(gone.picture(), mx, my, cardAt, to, 0f, 0.7f).start(FLIGHT_MS, 0, ArenaMotion.IN_OUT, () -> {
            if (landedOn != null) {
                landedOn.bump();
            }
        });
    }

    private static ArenaZonePile pile(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        if (player == null || matchUI == null) {
            return null;
        }
        final VField field = matchUI.getFieldViewFor(player);
        return field == null ? null : field.getDetailsPanel().getArenaPile(zone);
    }

    private Rectangle boundsOf(final Component comp) {
        if (!comp.isShowing() || comp.getWidth() <= 0 || comp.getHeight() <= 0) {
            return null;
        }
        return SwingUtilities.convertRectangle(comp.getParent(), comp.getBounds(), this);
    }

    private Rectangle pileRect(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final ArenaZonePile pile = pile(matchUI, player, zone);
        return pile == null ? null : boundsOf(pile);
    }

    // ---- effects asked for directly

    /** A ripple under a card that has just landed. */
    public static void ripple(final CardPanel panel, final Color colour) {
        if (!ArenaTheme.isMatchBoard() || !panel.isShowing() || panel.getParent() == null) {
            return;
        }
        final ArenaFx fx = get();
        final Rectangle c = fx.cardRect(panel);
        fx.new Ripple(c.x + c.width / 2, c.y + c.height, Math.round(c.width * 0.72f), colour).start(420, 0, ArenaMotion.OUT, null);
    }

    /** A number rising from the middle of a component's area, or of part of it. */
    public static void floatingNumber(final Component over, final Rectangle part, final String text, final Color colour, final float size) {
        if (!ArenaTheme.isMatchBoard() || !over.isShowing()) {
            return;
        }
        final ArenaFx fx = get();
        final Rectangle at = SwingUtilities.convertRectangle(over, part, fx);
        fx.new FloatingNumber(text, colour, at.x + at.width / 2, at.y + at.height / 2, size).start(950, 0, ArenaMotion.LINEAR, null);
    }

    /** Whose turn it is, across the middle of the board. */
    public static void turnBanner(final String text, final Color accent) {
        if (!ArenaTheme.isMatchBoard()) {
            return;
        }
        final ArenaFx fx = get();
        if (fx.getWidth() <= 0 || fx.getHeight() <= 0) {
            return;
        }
        fx.new Banner(text, accent).start(1150, 0, ArenaMotion.LINEAR, null);
    }
}
