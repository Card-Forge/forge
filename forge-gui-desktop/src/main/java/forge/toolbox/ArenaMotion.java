package forge.toolbox;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Everything that moves on the Arena board runs off this one clock on the event thread, which
 * only ticks while something is moving. Progress is measured in time, so a slow frame costs
 * smoothness rather than length, and a stall (an image loading, a garbage collection) slows the
 * animation down for that frame rather than making it jump ahead.
 */
public final class ArenaMotion {
    private static final int FRAME_MS = 15;
    /** The most one frame may advance an animation by. */
    private static final long LONGEST_STEP_NANOS = 50_000_000L;
    private static final int AMBIENT_MIN_MS = 33;
    private static final int AMBIENT_MAX_MS = 250;
    /** Idle effects may take about this share of the event thread; the slower the machine, the fewer frames they get. */
    private static final float AMBIENT_SHARE = 0.15f;

    private static final List<Tween> tweens = new ArrayList<>();
    private static final Timer clock = new Timer(FRAME_MS, e -> tick());
    private static long lastTick;

    private static final Set<JComponent> ambientAsked = new LinkedHashSet<>();
    private static final List<Rectangle> ambientPainted = new ArrayList<>();
    private static long lastAmbient;
    private static long ambientGap = AMBIENT_MIN_MS * 1_000_000L;

    private ArenaMotion() {
    }

    /** Shapes progress from 0 to 1. */
    public interface Ease {
        float at(float t);
    }

    /** Is told how far along the animation is, after easing. */
    public interface Step {
        void apply(float t);
    }

    public static final Ease LINEAR = t -> t;
    /** Sets off fast and settles: for what answers the player. */
    public static final Ease OUT = t -> 1f - (1f - t) * (1f - t) * (1f - t);
    public static final Ease IN_OUT = t -> t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    /** Goes a little too far and comes back: for what lands. */
    public static final Ease BACK = t -> {
        final float c = 1.70158f, u = t - 1f;
        return 1f + (c + 1f) * u * u * u + c * u * u;
    };

    /** Up to 1 and back to 0 again, quickly up and slowly down: for a bump or a flash, given plain progress. */
    public static float pop(final float t) {
        return t < 0.25f ? t / 0.25f : (1f - t) / 0.75f;
    }

    public static float clamp01(final float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    public static float lerp(final float a, final float b, final float t) {
        return a + (b - a) * t;
    }

    /** An animation under way. */
    public static final class Tween {
        private final long duration;
        private final long delay;
        private final Ease ease;
        private final Step step;
        private final Runnable done;
        private long elapsed;
        private boolean over;

        private Tween(final int millis, final int delayMillis, final Ease ease, final Step step, final Runnable done) {
            this.duration = Math.max(1L, millis) * 1_000_000L;
            this.delay = Math.max(0L, delayMillis) * 1_000_000L;
            this.ease = ease;
            this.step = step;
            this.done = done;
        }

        /** Stops it where it is. What it would have done on finishing is not done. */
        public void cancel() {
            over = true;
        }

        public boolean isOver() {
            return over;
        }

        /** Runs one frame; false once it has finished. */
        private boolean advance(final long nanos) {
            if (over) {
                return false;
            }
            elapsed += nanos;
            if (elapsed < delay) {
                return true;
            }
            final float t = clamp01((elapsed - delay) / (float) duration);
            step.apply(ease.at(t));
            if (t >= 1f && !over) {
                over = true;
                if (done != null) {
                    done.run();
                }
            }
            return !over;
        }
    }

    public static Tween play(final int millis, final Ease ease, final Step step) {
        return play(millis, 0, ease, step, null);
    }

    public static Tween play(final int millis, final Ease ease, final Step step, final Runnable done) {
        return play(millis, 0, ease, step, done);
    }

    /** Starts an animation; its steps always run on the event thread. */
    public static Tween play(final int millis, final int delayMillis, final Ease ease, final Step step, final Runnable done) {
        final Tween tween = new Tween(millis, delayMillis, ease, step, done);
        if (SwingUtilities.isEventDispatchThread()) {
            add(tween);
        } else {
            SwingUtilities.invokeLater(() -> add(tween));
        }
        return tween;
    }

    private static void add(final Tween tween) {
        tweens.add(tween);
        startClock();
    }

    private static void startClock() {
        if (!clock.isRunning()) {
            lastTick = System.nanoTime();
            clock.start();
        }
    }

    private static void tick() {
        final long now = System.nanoTime();
        final long step = Math.min(now - lastTick, LONGEST_STEP_NANOS);
        lastTick = now;
        // finishing one animation may start another, which waits for the next frame
        for (final Tween tween : new ArrayList<>(tweens)) {
            boolean running;
            try {
                running = tween.advance(step);
            } catch (final RuntimeException e) {
                // what went wrong was only for show: it is no reason to take the game down with it
                e.printStackTrace();
                tween.cancel();
                running = false;
            }
            if (!running) {
                tweens.remove(tween);
            }
        }
        tickAmbient(now);
        if (tweens.isEmpty() && ambientAsked.isEmpty()) {
            clock.stop();
        }
    }

    /**
     * For a component to call as it paints something that breathes: how far through its slow cycle
     * that is, from 0 to 1, and a promise of another repaint soon. Nothing needs starting or stopping:
     * once it stops asking, or stops being painted because it is hidden, the effect costs nothing.
     */
    public static float pulse(final JComponent component, final float seconds) {
        final float wave = (float) (0.5d + 0.5d * Math.sin(System.nanoTime() / 1e9d / seconds * 2d * Math.PI));
        if (!ArenaTheme.snapshotting && component.isShowing()) {
            ambientAsked.add(component);
            startClock();
        }
        return wave;
    }

    private static void tickAmbient(final long now) {
        ambientPainted.clear();
        if (ambientAsked.isEmpty() || now - lastAmbient < ambientGap) {
            return;
        }
        final List<JComponent> asked = new ArrayList<>(ambientAsked);
        boolean repainted = false;
        for (final JComponent component : asked) {
            final Window window = SwingUtilities.getWindowAncestor(component);
            if (!component.isShowing()) {
                ambientAsked.remove(component);
            } else if (window != null && window.isActive()) {
                // while another window is in use this stays asked for and waits, using nothing
                ambientAsked.remove(component);
                ambientPainted.add(SwingUtilities.convertRectangle(component, new Rectangle(0, 0, component.getWidth(), component.getHeight()), window));
                component.repaint();
                repainted = true;
            }
        }
        if (!repainted) {
            return;
        }
        lastAmbient = now;
        // this lands after the repaint just asked for, which says what those frames are costing
        SwingUtilities.invokeLater(() -> {
            final long cost = System.nanoTime() - now;
            final long gap = (long) (cost / AMBIENT_SHARE);
            ambientGap = Math.max(AMBIENT_MIN_MS * 1_000_000L, Math.min(AMBIENT_MAX_MS * 1_000_000L, gap));
        });
    }

    /** Whether all that is being painted now is the frame of a breathing effect, in {@code component}'s coordinates. */
    public static boolean isAmbientPaint(final Component component, final Rectangle clip) {
        if (ambientPainted.isEmpty() || clip == null) {
            return false;
        }
        final Window window = SwingUtilities.getWindowAncestor(component);
        if (window == null) {
            return false;
        }
        final Rectangle where = SwingUtilities.convertRectangle(component, clip, window);
        Rectangle all = null;
        for (final Rectangle r : ambientPainted) {
            all = all == null ? new Rectangle(r) : all.union(r);
        }
        return all != null && all.contains(where);
    }

    /** A number that eases towards whatever it is told to be, repainting as it goes. */
    public static final class Fade {
        private final Runnable repaint;
        private float value;
        private float goal;
        private Tween tween;

        public Fade(final Runnable repaint, final float start) {
            this.repaint = repaint;
            this.value = start;
            this.goal = start;
        }

        public float get() {
            return value;
        }

        public float goal() {
            return goal;
        }

        public boolean isMoving() {
            return tween != null && !tween.isOver();
        }

        /** Stops wherever it is and is at this value straight away. */
        public void set(final float now) {
            if (tween != null) {
                tween.cancel();
                tween = null;
            }
            value = now;
            goal = now;
        }

        public void to(final float target, final int millis) {
            to(target, millis, 0, null);
        }

        public void to(final float target, final int millis, final Runnable done) {
            to(target, millis, 0, done);
        }

        public void to(final float target, final int millis, final int delayMillis, final Runnable done) {
            if (target == goal && (isMoving() || value == goal)) {
                return;
            }
            if (tween != null) {
                tween.cancel();
            }
            goal = target;
            final float from = value;
            tween = play(millis, delayMillis, OUT, t -> {
                value = lerp(from, target, t);
                repaint.run();
            }, done);
        }
    }
}
