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
    public static final long SHOW_AFTER_NANOS = TimeUnit.SECONDS.toNanos(2);

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
}