package forge.toolbox;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import forge.view.FView;
import forge.view.arcane.ArenaRepaintManager;

/**
 * What lets the Arena look change without a cut. A picture of how things looked a moment ago is laid over the
 * window and fades off, so that whatever changed beneath it (another screen, a dialog that has gone, a new game)
 * comes up through it rather than appearing all at once. What is beneath stays live: anything moving there keeps
 * moving while the picture fades.
 *
 * <p>A curtain over the whole window can also be held: it stays up, opaque and cheap, while the change beneath is
 * made over several steps, say between pressing Next Game and the new board being laid out, and fades once it is
 * let go of, or after a few seconds whatever happens.</p>
 *
 * <p>Swing has no compositing, and at full size both painting the whole window and laying a translucent picture
 * over it take far longer than a frame. So a whole-window curtain keeps its own copy of what is beneath it, in which
 * only what has asked to be repainted is painted again, and mixes the two pictures itself, which is quick.</p>
 */
@SuppressWarnings("serial")
public final class ArenaCurtain extends JComponent {
    /** A held curtain fades by itself after this long, whatever has or has not happened beneath it. */
    private static final int HOLD_MOST_MS = 3000;
    /** Over a whole window, above everything in it, the navigation bar and its dimming included. */
    private static final Integer WINDOW_LAYER = JLayeredPane.DRAG_LAYER;
    /** Places apart to paint again before they are taken as one. */
    private static final int STALE_MOST = 6;

    /** The curtain over the whole window, while one is up. */
    private static ArenaCurtain window;

    private BufferedImage picture;
    /** Whether it covers the whole window, and so goes on covering it if the window changes size. */
    private boolean wholeWindow;
    private final ComponentListener windowResized = new ComponentAdapter() {
        @Override
        public void componentResized(final ComponentEvent e) {
            if (wholeWindow) {
                // a screen may change the window's frame, the title bar coming or going: the picture is stretched to fit,
                // once, since stretching it every frame would cost more than the whole fade otherwise does
                final int w = e.getComponent().getWidth(), h = e.getComponent().getHeight();
                if (w > 0 && h > 0 && (w != picture.getWidth() || h != picture.getHeight())) {
                    final BufferedImage fitted = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    final Graphics2D g = fitted.createGraphics();
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(picture, 0, 0, w, h, null);
                    g.dispose();
                    picture = fitted;
                }
                setBounds(0, 0, w, h);
                if (compositing) {
                    makeBuffers(w, h); // and what is beneath is painted again, all of it
                }
                repaint();
            } else {
                finish(); // the picture no longer fits: what is beneath is shown as it is
            }
        }
    };
    private float shown = 1f;
    private boolean holding;
    private boolean fading;
    private ArenaMotion.Tween tween;
    private Timer giveUp;
    /** Whether it mixes its picture with its own copy of what is beneath, rather than laying it over the window. */
    private boolean compositing;
    /** Its copy of what is beneath it, and the two mixed, which is what it paints. */
    private BufferedImage under, mixed;
    /** What of {@link #under} has to be painted again, in its own coordinates: a few places apart, or one that holds them all. */
    private final java.util.List<Rectangle> stale = new java.util.ArrayList<>();
    /** Set while what is beneath is being painted into its copy, which this is not to be part of. */
    private boolean photographing;

    private ArenaCurtain(final BufferedImage picture) {
        this.picture = picture;
        setOpaque(false);
        addMouseListener(new MouseAdapter() { }); // while held, what is beneath it is not to be clicked
    }

    /** Whether this is a curtain, which a photograph of the window for a dialog's backdrop leaves out. */
    public static boolean isCurtain(final Component c) {
        return c instanceof ArenaCurtain;
    }

    private static boolean canCover() {
        if (!ArenaTheme.isEnabled() || !SwingUtilities.isEventDispatchThread()) {
            return false;
        }
        final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
        return lpn != null && lpn.isShowing() && lpn.getWidth() > 0 && lpn.getHeight() > 0;
    }

    /**
     * Photographs the window as it is and holds the picture over it until {@link #release}. True if it is now
     * covered by a curtain this put up; false if the Arena look is off, or one was already being held.
     */
    public static boolean cover() {
        if (!canCover() || (window != null && window.holding)) {
            return false;
        }
        final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
        final int w = lpn.getWidth(), h = lpn.getHeight();
        final BufferedImage picture = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = picture.createGraphics();
        try {
            // a curtain still fading is part of how the window looks, and goes into this picture
            lpn.paint(g);
        } finally {
            g.dispose();
        }
        final ArenaCurtain old = window;
        final ArenaCurtain curtain = new ArenaCurtain(picture);
        curtain.holding = true;
        curtain.wholeWindow = true;
        curtain.setOpaque(true); // and so nothing beneath it is painted while it is held
        curtain.setBounds(0, 0, w, h);
        window = curtain;
        ArenaRepaintManager.overBoard(() -> {
            lpn.add(curtain, WINDOW_LAYER);
            lpn.moveToFront(curtain);
            if (old != null) {
                old.finish();
            }
        });
        lpn.addComponentListener(curtain.windowResized);
        curtain.giveUp = new Timer(HOLD_MOST_MS, e -> curtain.fade(420));
        curtain.giveUp.setRepeats(false);
        curtain.giveUp.start();
        return true;
    }

    /** Lets the held curtain fade, once what has changed beneath it has had a moment to lay itself out. */
    public static void release(final int millis) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> release(millis));
            return;
        }
        if (window != null && window.holding) {
            final ArenaCurtain curtain = window;
            curtain.stopHolding();
            SwingUtilities.invokeLater(() -> curtain.fade(millis));
        }
    }

    /**
     * For the repaint manager: something has asked to be repainted. If it lies beneath a whole-window curtain that
     * keeps its own copy of what is beneath, that part of the copy is painted again for the next frame.
     */
    public static void noteRepaint(final Component c, final int x, final int y, final int w, final int h) {
        final ArenaCurtain curtain = window;
        if (curtain == null || !curtain.compositing || curtain.photographing || c == curtain || c == null || w <= 0 || h <= 0) {
            return;
        }
        final java.awt.Container parent = curtain.getParent();
        if (parent == null) {
            return;
        }
        if (c instanceof java.awt.Window) {
            if (c == SwingUtilities.getWindowAncestor(curtain)) {
                curtain.markStale(new Rectangle(0, 0, curtain.getWidth(), curtain.getHeight()));
            }
        } else if (SwingUtilities.isDescendingFrom(c, parent)) {
            curtain.markStale(SwingUtilities.convertRectangle(c, new Rectangle(x, y, w, h), curtain));
        }
    }

    private void markStale(final Rectangle r) {
        for (final Rectangle s : stale) {
            if (s.intersects(r)) {
                s.add(r);
                return;
            }
        }
        stale.add(r);
        if (stale.size() > STALE_MOST) {
            final Rectangle all = new Rectangle(stale.get(0));
            for (final Rectangle s : stale) {
                all.add(s);
            }
            stale.clear();
            stale.add(all);
        }
    }

    private void makeBuffers(final int w, final int h) {
        under = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        mixed = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        stale.clear();
        stale.add(new Rectangle(0, 0, w, h));
    }

    /** Paints again what of its copy of what is beneath has asked to be repainted. */
    private void refreshUnder() {
        final java.awt.Container parent = getParent();
        if (stale.isEmpty() || parent == null) {
            return;
        }
        final javax.swing.JRootPane root = SwingUtilities.getRootPane(parent);
        if (root != null) {
            root.validate(); // laid out as it is about to be shown, as the repaint manager would before painting
        }
        final Rectangle all = new Rectangle(0, 0, under.getWidth(), under.getHeight());
        final java.util.List<Rectangle> places = new java.util.ArrayList<>(stale);
        stale.clear();
        photographing = true;
        try {
            for (final Rectangle place : places) {
                final Rectangle r = place.intersection(all);
                if (r.isEmpty()) {
                    continue;
                }
                final Graphics2D g = under.createGraphics();
                try {
                    g.clip(r);
                    g.translate(-getX(), -getY());
                    parent.paint(g);
                } finally {
                    g.dispose();
                }
            }
        } finally {
            photographing = false;
        }
    }

    /** Mixes the picture and the copy of what is beneath, in that proportion. */
    private void mix() {
        final int[] over = ((java.awt.image.DataBufferInt) picture.getRaster().getDataBuffer()).getData();
        final int[] beneath = ((java.awt.image.DataBufferInt) under.getRaster().getDataBuffer()).getData();
        final int[] out = ((java.awt.image.DataBufferInt) mixed.getRaster().getDataBuffer()).getData();
        final int a = Math.round(ArenaMotion.clamp01(shown) * 256f), b = 256 - a;
        final int n = Math.min(out.length, Math.min(over.length, beneath.length));
        for (int i = 0; i < n; i++) {
            final int p = over[i], q = beneath[i];
            out[i] = ((((p & 0xFF00FF) * a + (q & 0xFF00FF) * b) >>> 8) & 0xFF00FF)
                    | ((((p & 0x00FF00) * a + (q & 0x00FF00) * b) >>> 8) & 0x00FF00);
        }
    }

    /** Whether a curtain is being held over the window. */
    public static boolean isHeld() {
        return window != null && window.holding;
    }

    /**
     * For something about to go from the window: a picture of it as it is now, left where it was in its layer
     * (behind whatever else is there, such as what replaces it) to fade away. The caller removes it.
     */
    public static void fadeOut(final JComponent what, final int millis) {
        fadeOut(what, millis, true);
    }

    /** For something about to change all at once: a picture of it as it is now, laid over it to fade away. */
    public static void fadeOver(final JComponent what, final int millis) {
        fadeOut(what, millis, false);
    }

    private static void fadeOut(final JComponent what, final int millis, final boolean behind) {
        if (!canCover() || what.getWidth() <= 0 || what.getHeight() <= 0 || !what.isShowing()) {
            return;
        }
        final JLayeredPane lpn = FView.SINGLETON_INSTANCE.getLpnDocument();
        final BufferedImage picture = new BufferedImage(what.getWidth(), what.getHeight(),
                what.isOpaque() ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB_PRE);
        final Graphics2D g = picture.createGraphics();
        try {
            what.paint(g);
        } finally {
            g.dispose();
        }
        final ArenaCurtain curtain = new ArenaCurtain(picture);
        final Rectangle r = SwingUtilities.convertRectangle(what.getParent(), what.getBounds(), lpn);
        curtain.setBounds(r);
        final Integer layer = what.getParent() == lpn ? JLayeredPane.getLayer(what) : FView.OVERLAY_LAYER;
        ArenaRepaintManager.overBoard(() -> {
            lpn.add(curtain, layer);
            if (behind) {
                lpn.moveToBack(curtain); // so that anything opening in its place comes in over it
            } else {
                lpn.moveToFront(curtain);
            }
        });
        lpn.addComponentListener(curtain.windowResized);
        curtain.fade(millis);
    }

    private void stopHolding() {
        holding = false;
        setOpaque(compositing);
        if (giveUp != null) {
            giveUp.stop();
            giveUp = null;
        }
    }

    private void fade(final int millis) {
        if (fading || getParent() == null) {
            return;
        }
        // over the whole window, and told by the repaint manager what is repainted beneath: it mixes the pictures itself
        compositing = wholeWindow && javax.swing.RepaintManager.currentManager(this) instanceof ArenaRepaintManager
                && picture.getWidth() == getWidth() && picture.getHeight() == getHeight();
        if (compositing) {
            makeBuffers(getWidth(), getHeight());
        }
        stopHolding();
        fading = true;
        tween = ArenaMotion.play(millis, ArenaMotion.IN_OUT, t -> {
            shown = 1f - t;
            if (compositing) {
                refreshUnder();
                mix();
            }
            repaint();
        }, this::finish);
    }

    private void finish() {
        compositing = false;
        stopHolding();
        if (tween != null) {
            tween.cancel();
        }
        if (window == this) {
            window = null;
        }
        final java.awt.Container parent = getParent();
        if (parent != null) {
            parent.removeComponentListener(windowResized);
            final Rectangle r = getBounds();
            ArenaRepaintManager.overBoard(() -> {
                parent.remove(this);
                parent.repaint(r.x, r.y, r.width, r.height);
            });
        }
    }

    /** Not while it paints what is beneath into its copy: Swing leaves out whatever an opaque component hides. */
    @Override
    public boolean isOpaque() {
        return !photographing && super.isOpaque();
    }

    @Override
    public boolean contains(final int x, final int y) {
        return holding && super.contains(x, y); // once it fades, what is beneath is there to be used
    }

    @Override
    protected void paintComponent(final Graphics g0) {
        if (photographing) {
            return;
        }
        if (compositing) {
            g0.drawImage(mixed, 0, 0, null);
            return;
        }
        if (shown <= 0f) {
            return;
        }
        final Graphics2D g = (Graphics2D) g0.create();
        try {
            if (shown < 1f) {
                g.setComposite(AlphaComposite.SrcOver.derive(shown));
            }
            if (picture.getWidth() == getWidth() && picture.getHeight() == getHeight()) {
                g.drawImage(picture, 0, 0, null);
            } else {
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(picture, 0, 0, getWidth(), getHeight(), null);
            }
        } finally {
            g.dispose();
        }
    }
}
