package forge.screens.match.arena;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;

/**
 * Everything that moves in the Arena look runs off this one clock, ticked once per rendered frame by
 * {@code Forge.render()} (no threads, no timers). Progress is measured in time, so a slow frame costs smoothness rather
 * than length, and a stall (a texture loading, a GC) advances an animation by at most {@link #LONGEST_STEP_MS}.
 * Port of the Swing {@code forge.toolbox.ArenaMotion} (same eases, same Tween/Fade API) plus {@link Spring} and frame
 * requests for libGDX.
 *
 * <h2>Frames</h2>
 * The libGDX client renders on demand (continuous rendering is off unless something asks). While any {@link Tween},
 * {@link Fade} or {@link Spring} is moving, or anything is {@link #hold held}, the clock asks for the next frame
 * itself; nothing else is needed. A view that animates by itself (computing its look from the time in
 * {@code draw}) calls {@link #requestFrame()} from {@code draw} for as long as it is not at rest; {@link #pulse(float)}
 * does that for you. Everything here must be used on the render thread, except {@link #play} which may be called from
 * any thread (it is then started on the render thread).
 */
public final class ArenaMotion {
    private ArenaMotion() { }

    // ---- timing constants (from the Swing Arena code) ---------------------------------------------------------------
    /** The frame rate the Swing clock aimed for; the GPU client simply runs at the display rate. */
    public static final int TARGET_FPS = 120;
    /** The most one frame may advance an animation by. */
    public static final int LONGEST_STEP_MS = 50;
    /** A card flying between zones (ArenaFx.FLIGHT_MS), cards that leave together go this far apart, and the gather. */
    public static final int FLIGHT_MS = 340;
    public static final int STAGGER_MS = 70;
    public static final int GATHER_MS = 40;
    /** Hover / press feedback. */
    public static final int HOVER_MS = 120;
    public static final int PRESS_MS = 90;
    /** Something opening (dialog, browser) and what it showed fading off when it closes. */
    public static final int OPEN_MS = 220;
    public static final int CLOSE_MS = 200;
    public static final int DIALOG_GONE_MS = 240;
    /** One screen into the next (Swing FControl: 380 after a held curtain, 460 otherwise). */
    public static final int SCREEN_FADE_MS = 380;
    public static final int SCREEN_FADE_LONG_MS = 460;
    /** A held curtain fades by itself after this long. */
    public static final int HOLD_MOST_MS = 3000;
    /** A beat of an ambient effect, and a flash delay. */
    public static final int BEAT_MS = 650;
    public static final int FLASH_DELAY_MS = 80;

    // ---- eases ------------------------------------------------------------------------------------------------------
    /** Shapes progress from 0 to 1. */
    public interface Ease {
        float at(float t);
    }

    /** Is told how far along the animation is, after easing. */
    public interface Step {
        void apply(float t);
    }

    public static final Ease LINEAR = t -> t;
    /** Sets off fast and settles: for what answers the player (cubic out). */
    public static final Ease OUT = t -> 1f - (1f - t) * (1f - t) * (1f - t);
    /** Starts slowly (cubic in): for what leaves. */
    public static final Ease IN = t -> t * t * t;
    public static final Ease IN_OUT = t -> t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    /** Goes a little too far and comes back: for what lands. */
    public static final Ease BACK = t -> {
        final float c = 1.70158f, u = t - 1f;
        return 1f + (c + 1f) * u * u * u + c * u * u;
    };
    /** Smoothstep. */
    public static final Ease SMOOTH = t -> t * t * (3f - 2f * t);

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

    /** Where {@code v} lies between {@code a} and {@code b}, clamped to 0..1. */
    public static float unlerp(final float a, final float b, final float v) {
        return a == b ? (v >= b ? 1f : 0f) : clamp01((v - a) / (b - a));
    }

    // ---- the clock --------------------------------------------------------------------------------------------------
    private static final List<Tween> tweens = new ArrayList<>();
    private static final List<Spring> springs = new ArrayList<>();
    private static final Map<Object, Boolean> holds = new IdentityHashMap<>();
    private static final long LONGEST_STEP_NANOS = LONGEST_STEP_MS * 1_000_000L;
    private static long lastTick;
    private static boolean wasRunning;
    private static long frame;
    private static float frameStep;
    private static Thread renderThread;
    private static final long START = System.nanoTime();

    /**
     * Advances every animation by one frame. Called once per rendered frame by {@code Forge.render()} before anything
     * is drawn; also refreshes {@link ArenaTheme#isActive()}. Do not call it yourself.
     */
    public static void tick() {
        renderThread = Thread.currentThread();
        frame++;
        ArenaTheme.refresh();
        final long now = System.nanoTime();
        final boolean running = isRunning();
        // the first frame after a rest starts from now: an idle gap is not animation time
        final long step = running && wasRunning ? Math.min(now - lastTick, LONGEST_STEP_NANOS) : 0L;
        lastTick = now;
        frameStep = step / 1e9f;
        if (!tweens.isEmpty()) {
            // finishing one animation may start another, which waits for the next frame
            final Tween[] now0 = tweens.toArray(new Tween[0]);
            for (final Tween tween : now0) {
                boolean on;
                try {
                    on = tween.advance(step);
                } catch (final RuntimeException e) {
                    // only for show: no reason to take the game down with it
                    e.printStackTrace();
                    tween.cancel();
                    on = false;
                }
                if (!on) {
                    tweens.remove(tween);
                }
            }
        }
        if (!springs.isEmpty()) {
            final Spring[] now1 = springs.toArray(new Spring[0]);
            for (final Spring s : now1) {
                if (!s.advance(step / 1e9f)) {
                    springs.remove(s);
                    s.registered = false;
                }
            }
        }
        wasRunning = isRunning();
        if (wasRunning) {
            requestFrame();
        }
    }

    private static boolean isRunning() {
        return !tweens.isEmpty() || !springs.isEmpty() || !holds.isEmpty();
    }

    /** Whether anything is moving (tweens, fades, springs or holds). */
    public static boolean isAnimating() {
        return isRunning();
    }

    /** How many frames the clock has ticked: handy as a cheap "has this been drawn this frame" stamp. */
    public static long frame() {
        return frame;
    }

    /** Seconds this frame advanced animations by (0 on the first frame after a rest). */
    public static float frameSeconds() {
        return frameStep;
    }

    /** Seconds since start-up, for effects computed from the time. Monotonic. */
    public static float seconds() {
        return (System.nanoTime() - START) / 1e9f;
    }

    /** Asks libGDX for one more frame. Safe from any thread; cheap; call it from draw() while not at rest. */
    public static void requestFrame() {
        if (Gdx.graphics != null) {
            Gdx.graphics.requestRendering();
        }
    }

    /**
     * Keeps frames coming while {@code owner} is held, for animations not built from Tweens (say a flight a view steps
     * itself). Every hold must be let go with {@link #release(Object)}. Holding the same owner twice is one hold.
     */
    public static void hold(final Object owner) {
        holds.put(owner, Boolean.TRUE);
        requestFrame();
    }

    public static void release(final Object owner) {
        holds.remove(owner);
    }

    /**
     * For a view to call as it draws something that breathes: how far through its slow cycle that is, 0..1 (a sine),
     * and a request for another frame. Nothing needs starting or stopping: once a view stops drawing it (hidden,
     * gone), it costs nothing. While something breathes the board renders at the display rate.
     */
    public static float pulse(final float seconds) {
        requestFrame();
        return (float) (0.5d + 0.5d * Math.sin((System.nanoTime() - START) / 1e9d / seconds * 2d * Math.PI));
    }

    /** {@link #pulse(float)} with a phase offset (0..1), so neighbours do not breathe in step. */
    public static float pulse(final float seconds, final float phase) {
        requestFrame();
        return (float) (0.5d + 0.5d * Math.sin(((System.nanoTime() - START) / 1e9d / seconds + phase) * 2d * Math.PI));
    }

    private static boolean onRenderThread() {
        return renderThread == null || renderThread == Thread.currentThread();
    }

    // ---- tweens -----------------------------------------------------------------------------------------------------
    /** An animation under way. */
    public static final class Tween {
        private final long duration;
        private final long delay;
        private final Ease ease;
        private final Step step;
        private final Runnable done;
        private long elapsed;
        private boolean over;
        private float progress;

        private Tween(final int millis, final int delayMillis, final Ease ease, final Step step, final Runnable done) {
            this.duration = Math.max(1L, millis) * 1_000_000L;
            this.delay = Math.max(0L, delayMillis) * 1_000_000L;
            this.ease = ease == null ? LINEAR : ease;
            this.step = step;
            this.done = done;
        }

        /** Stops it where it is. What it would have done on finishing is not done. */
        public void cancel() {
            over = true;
        }

        /** Jumps to the end now, running the last step and the finishing callback. */
        public void finish() {
            if (over) {
                return;
            }
            elapsed = delay + duration;
            advance(0L);
        }

        public boolean isOver() {
            return over;
        }

        /** Plain (un-eased) progress 0..1. */
        public float progress() {
            return progress;
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
            progress = clamp01((elapsed - delay) / (float) duration);
            if (step != null) {
                step.apply(ease.at(progress));
            }
            if (progress >= 1f && !over) {
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

    /**
     * Starts an animation: {@code step} is told the eased progress once per frame, the first time on the next frame
     * (at progress 0 then, unless delayed), the last time at 1, then {@code done} runs. Steps always run on the render
     * thread; from another thread the tween starts on the render thread a moment later.
     */
    public static Tween play(final int millis, final int delayMillis, final Ease ease, final Step step, final Runnable done) {
        final Tween tween = new Tween(millis, delayMillis, ease, step, done);
        if (onRenderThread()) {
            add(tween);
        } else {
            Gdx.app.postRunnable(() -> add(tween));
        }
        return tween;
    }

    private static void add(final Tween tween) {
        if (!tween.over) {
            tweens.add(tween);
        }
        requestFrame();
    }

    // ---- fades ------------------------------------------------------------------------------------------------------
    /** A number that eases towards whatever it is told to be (cubic out). Read it with {@link #get()} in draw(). */
    public static final class Fade {
        private float value;
        private float goal;
        private Tween tween;

        public Fade(final float start) {
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
            to(target, millis, delayMillis, OUT, done);
        }

        /** Eases from wherever it is now to {@code target}. Asking for the goal it already has does nothing. */
        public void to(final float target, final int millis, final int delayMillis, final Ease ease, final Runnable done) {
            if (target == goal && (isMoving() || value == goal)) {
                return;
            }
            if (tween != null) {
                tween.cancel();
            }
            goal = target;
            final float from = value;
            tween = play(millis, delayMillis, ease, t -> value = lerp(from, target, t), done);
        }
    }

    // ---- springs ----------------------------------------------------------------------------------------------------
    /**
     * A value that follows a target smoothly with no fixed duration (critically damped): for things whose goal keeps
     * changing, such as a card following the pointer or a hover lift. {@code halfLifeMs} is how long it takes to close
     * half the distance. It stops (and stops asking for frames) once within {@code epsilon} of the target.
     */
    public static final class Spring {
        private float value, velocity, target;
        private final float omega;
        private final float epsilon;
        boolean registered;

        public Spring(final float start, final int halfLifeMs) {
            this(start, halfLifeMs, 0.001f);
        }

        public Spring(final float start, final int halfLifeMs, final float epsilon) {
            this.value = start;
            this.target = start;
            // critically damped: x(t) ~ (1 + w t) e^{-w t}; half the distance closes at about 1.678 / w
            this.omega = 1.678f / Math.max(0.001f, halfLifeMs / 1000f);
            this.epsilon = epsilon;
        }

        public float get() {
            return value;
        }

        public float target() {
            return target;
        }

        public boolean isMoving() {
            return registered;
        }

        /** Sets a new target; it starts moving on the next frame. */
        public void to(final float newTarget) {
            if (newTarget == target && !registered) {
                return;
            }
            target = newTarget;
            if (!registered && Math.abs(value - target) > epsilon) {
                registered = true;
                springs.add(this);
                requestFrame();
            }
        }

        /** Jumps to {@code now} and rests there. */
        public void set(final float now) {
            value = now;
            target = now;
            velocity = 0f;
            if (registered) {
                springs.remove(this);
                registered = false;
            }
        }

        private boolean advance(final float dt) {
            if (dt <= 0f) {
                return true;
            }
            final float x = value - target;
            final float exp = (float) Math.exp(-omega * dt);
            final float temp = (velocity + omega * x) * dt;
            velocity = (velocity - omega * temp) * exp;
            value = target + (x + temp) * exp;
            if (Math.abs(value - target) <= epsilon && Math.abs(velocity) <= epsilon * omega) {
                value = target;
                velocity = 0f;
                return false;
            }
            return true;
        }
    }
}
