package forge.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Tracks the single shared deadline of an AI player's current "thinking" session, so that
 *  - every nested AI call (spell choice -> predicted combat -> attackers ...) stops at the SAME instant, and
 *  - the UI can draw a shrinking timer bar without touching AI internals.
 *
 * Writers: AI code (begin/end, always in try/finally).  Readers: UI (getRemainingFraction).
 * Nested begin() calls for the same player are counted; the first one fixes the deadline.
 * Allocation: one tiny Slot per player, ever. Reads are lock free.
 */
public final class AITimeoutTracker {
    /** The bar becomes visible once this much time has elapsed. */
    public static final long SHOW_AFTER_NANOS = TimeUnit.SECONDS.toNanos(1);

    private static final class Slot {
        volatile long startNanos;
        volatile long timeoutNanos;
        volatile int depth;
    }

    private static final ConcurrentHashMap<Integer, Slot> SLOTS = new ConcurrentHashMap<>();
    private static volatile Runnable repaintHook;

    private AITimeoutTracker() { }

    /** UI registers this so a repaint is requested when the bar is due to appear (non-continuous rendering). */
    public static void setRepaintHook(final Runnable hook) {
        repaintHook = hook;
    }

    public static void begin(final int playerId, final long timeoutNanos) {
        Slot s = SLOTS.get(playerId);
        if (s == null) {
            s = new Slot();
            final Slot prev = SLOTS.putIfAbsent(playerId, s);
            if (prev != null) {
                s = prev;
            }
        }
        boolean outermost = false;
        synchronized (s) {
            if (s.depth++ == 0) {
                s.startNanos = System.nanoTime();
                s.timeoutNanos = timeoutNanos;
                outermost = true;
            }
        }
        final Runnable hook = repaintHook;
        if (outermost && hook != null && timeoutNanos > SHOW_AFTER_NANOS) {
            // wake the renderer exactly when the bar should show up (only allocates when a UI hook is registered)
            ThreadUtil.delay((int) TimeUnit.NANOSECONDS.toMillis(SHOW_AFTER_NANOS), hook);
        }
    }

    public static void end(final int playerId) {
        final Slot s = SLOTS.get(playerId);
        if (s == null) {
            return;
        }
        synchronized (s) {
            if (s.depth > 0) {
                s.depth--;
            }
        }
        final Runnable hook = repaintHook;
        if (hook != null) {
            hook.run(); // one last repaint so the bar disappears immediately
        }
    }

    public static boolean isActive(final int playerId) {
        final Slot s = SLOTS.get(playerId);
        return s != null && s.depth > 0;
    }

    /** Absolute System.nanoTime() deadline of the active session, or fallbackNanos from now when none is active. */
    public static long deadlineNanos(final int playerId, final long fallbackTimeoutNanos) {
        final Slot s = SLOTS.get(playerId);
        if (s != null && s.depth > 0) {
            return s.startNanos + s.timeoutNanos;
        }
        return System.nanoTime() + fallbackTimeoutNanos;
    }

    /**
     * 1.0 when the bar first appears (2s elapsed) shrinking to 0.0 at the timeout.
     * Returns -1 when the bar must be hidden (no session, under 2s elapsed, finished or timed out).
     */
    public static float getRemainingFraction(final int playerId) {
        final Slot s = SLOTS.get(playerId);
        if (s == null || s.depth == 0) {
            return -1f;
        }
        final long elapsed = System.nanoTime() - s.startNanos;
        final long window = s.timeoutNanos - SHOW_AFTER_NANOS;
        if (window <= 0 || elapsed < SHOW_AFTER_NANOS || elapsed >= s.timeoutNanos) {
            return -1f;
        }
        return 1f - (float) (elapsed - SHOW_AFTER_NANOS) / (float) window;
    }

    // ---- hard deadline for the CURRENT thread (cooperative abort) ----

    /** Thrown by {@link #check()} when the thread's AI deadline has passed. Carries no stack trace (cheap). */
    public static final class Expired extends RuntimeException {
        private static final long serialVersionUID = 1L;
        Expired() { super("AI deadline reached", null, false, false); }
    }

    private static final class Limit {
        boolean armed;
        boolean honourInterrupt;
        long deadlineNanos;
    }

    private static final ThreadLocal<Limit> LIMIT = ThreadLocal.withInitial(Limit::new);

    /**
     * Arms this thread with an absolute System.nanoTime() deadline; returns a token for {@link #disarm}.
     * Nested arming can only tighten the deadline, never extend it.
     * @param honourInterrupt also abort when the thread is interrupted (for worker threads that get cancel(true))
     */
    public static Object arm(final long deadlineNanos, final boolean honourInterrupt) {
        final Limit l = LIMIT.get();
        final long[] prev = {l.armed ? 1 : 0, l.deadlineNanos, l.honourInterrupt ? 1 : 0};
        if (!l.armed || deadlineNanos - l.deadlineNanos < 0) {
            l.deadlineNanos = deadlineNanos;
        }
        l.armed = true;
        l.honourInterrupt |= honourInterrupt;
        return prev;
    }

    public static void disarm(final Object token) {
        final long[] prev = (long[]) token;
        final Limit l = LIMIT.get();
        l.armed = prev[0] == 1;
        l.deadlineNanos = prev[1];
        l.honourInterrupt = prev[2] == 1;
    }

    /** Cheap: call it inside AI loops. Throws {@link Expired} once the armed deadline passed (no-op when not armed). */
    public static void check() {
        final Limit l = LIMIT.get();
        if (l.armed && (System.nanoTime() - l.deadlineNanos >= 0 || (l.honourInterrupt && Thread.currentThread().isInterrupted()))) {
            throw new Expired();
        }
    }

    /** Nanoseconds left of this thread's armed deadline (Long.MAX_VALUE when not armed). */
    public static long remainingNanos() {
        final Limit l = LIMIT.get();
        return l.armed ? l.deadlineNanos - System.nanoTime() : Long.MAX_VALUE;
    }
}
