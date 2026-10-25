package forge.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Tracks the single shared deadline of an AI player's current "thinking" session, so that
 *  - every nested AI call (spell choice -> predicted combat -> attackers ...) stops at the SAME instant, and
 *  - the UI can draw a shrinking timer bar without touching AI internals.
 *
 * Writers: AI code (begin/end, always in try/finally).  Readers: UI (getRemainingFraction).
 * Nested begin() calls for the same key (see {@link #key}) are counted; the first one fixes the deadline.
 * A session that is still open well past its own timeout is considered leaked (a begin() without end())
 * and is replaced by the next begin(), so one bug can never leave a player with an expired deadline forever.
 * Allocation: one tiny Slot per (game, player), purged once idle slots pile up. Reads are lock free.
 */
public final class AITimeoutTracker {
    /** The bar becomes visible once this much time has elapsed. */
    public static final long SHOW_AFTER_NANOS = TimeUnit.SECONDS.toNanos(2);

    /** How long past its timeout a session may stay open before it is treated as leaked (package-private for tests). */
    static volatile long staleGraceNanos = TimeUnit.SECONDS.toNanos(2);

    private static final class Slot {
        volatile long startNanos;
        volatile long timeoutNanos;
        volatile int depth;
        /** Set (under the slot's lock) once purged from SLOTS; a racing begin() must then fetch a fresh slot. */
        boolean removed;
    }

    /** Idle slots are purged once the map grows past this, so finished games do not leave slots behind forever. */
    private static final int PURGE_THRESHOLD = 64;

    /**
     * Key of one AI player's session. Player ids restart at 0 in every game while game ids are unique per JVM,
     * so two matches running side by side (or a sim and a real game) never share a deadline or a timer bar.
     */
    public static long key(final int gameId, final int playerId) {
        return ((long) gameId << 32) | (playerId & 0xFFFFFFFFL);
    }

    private static void purgeIdle() {
        for (final java.util.Map.Entry<Long, Slot> e : SLOTS.entrySet()) {
            final Slot s = e.getValue();
            synchronized (s) {
                if (s.depth == 0 && !s.removed) {
                    s.removed = true;
                    SLOTS.remove(e.getKey(), s);
                }
            }
        }
    }

    private static final ConcurrentHashMap<Long, Slot> SLOTS = new ConcurrentHashMap<>();
    private static volatile Runnable repaintHook;

    private AITimeoutTracker() { }

    /** UI registers this so a repaint is requested when the bar is due to appear (non-continuous rendering). */
    public static void setRepaintHook(final Runnable hook) {
        repaintHook = hook;
    }

    private static boolean isStale(final Slot s, final long now) {
        return s.depth > 0 && now - s.startNanos - s.timeoutNanos > staleGraceNanos;
    }

    public static void begin(final long key, final long timeoutNanos) {
        boolean outermost = false;
        long sessionStart = 0;
        Slot s;
        while (true) {
            Slot found = SLOTS.get(key);
            if (found == null) {
                if (SLOTS.size() > PURGE_THRESHOLD) {
                    purgeIdle();
                }
                found = new Slot();
                final Slot prev = SLOTS.putIfAbsent(key, found);
                if (prev != null) {
                    found = prev;
                }
            }
            s = found;
            synchronized (s) {
                if (s.removed) {
                    continue; // purged between lookup and lock: fetch (or create) a live slot
                }
                final long now = System.nanoTime();
                if (isStale(s, now)) {
                    s.depth = 0; // leaked session: start over
                }
                if (s.depth == 0) {
                    // publish start/timeout BEFORE depth so lock free readers never pair a live depth with old times
                    s.startNanos = now;
                    s.timeoutNanos = timeoutNanos;
                    outermost = true;
                }
                sessionStart = s.startNanos;
                s.depth++;
            }
            break;
        }
        final Runnable hook = repaintHook;
        if (outermost && hook != null && timeoutNanos > SHOW_AFTER_NANOS) {
            final Slot slot = s;
            final long started = sessionStart;
            // wake the renderer exactly when the bar should show up, but only if this very session is still running then
            ThreadUtil.delay((int) TimeUnit.NANOSECONDS.toMillis(SHOW_AFTER_NANOS), () -> {
                if (slot.depth > 0 && slot.startNanos == started) {
                    hook.run();
                }
            });
        }
    }

    public static void end(final long key) {
        final Slot s = SLOTS.get(key);
        if (s == null) {
            return;
        }
        boolean barMayBeVisible = false;
        synchronized (s) {
            if (s.depth > 0 && --s.depth == 0) {
                barMayBeVisible = System.nanoTime() - s.startNanos >= SHOW_AFTER_NANOS;
            }
        }
        final Runnable hook = repaintHook;
        if (barMayBeVisible && hook != null) {
            hook.run(); // one last repaint so the bar disappears immediately (not needed if it never showed)
        }
    }

    public static boolean isActive(final long key) {
        final Slot s = SLOTS.get(key);
        return s != null && s.depth > 0 && !isStale(s, System.nanoTime());
    }

    /** Absolute System.nanoTime() deadline of the active session, or fallbackNanos from now when none is active. */
    public static long deadlineNanos(final long key, final long fallbackTimeoutNanos) {
        final long now = System.nanoTime();
        final Slot s = SLOTS.get(key);
        if (s != null && s.depth > 0 && !isStale(s, now)) {
            return s.startNanos + s.timeoutNanos;
        }
        return now + fallbackTimeoutNanos;
    }

    /**
     * 1.0 when the bar first appears (SHOW_AFTER elapsed) shrinking to 0.0 at the timeout.
     * Returns -1 when the bar must be hidden (no session, too early, finished or timed out).
     */
    public static float getRemainingFraction(final long key) {
        final Slot s = SLOTS.get(key);
        if (s == null || s.depth == 0) {
            return -1f;
        }
        final long start = s.startNanos;
        final long timeout = s.timeoutNanos;
        final long elapsed = System.nanoTime() - start;
        final long window = timeout - SHOW_AFTER_NANOS;
        if (window <= 0 || elapsed < SHOW_AFTER_NANOS || elapsed >= timeout) {
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
     * Always pair with disarm() in a finally block - also on pooled threads, or the next task inherits the limit.
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

    /**
     * Nanoseconds left for a caller that may or may not be armed: the tighter of this thread's armed deadline
     * and the player's session deadline (or a fresh fallback timeout when there is no session).
     * Use this instead of {@link #remainingNanos()} for waits - an unarmed caller must not wait "forever".
     */
    public static long remainingNanos(final long key, final long fallbackTimeoutNanos) {
        return Math.min(remainingNanos(), deadlineNanos(key, fallbackTimeoutNanos) - System.nanoTime());
    }
}