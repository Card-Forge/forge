package forge.game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import forge.util.IHasForgeLog;

/**
 * Decides which thread may run engine code for one game at a time.
 * A thread takes ownership when it starts running engine code and gives it up again whenever it
 * blocks, so a thread waiting on a player, a client or a timer holds nothing. That is what lets
 * one blocking input sit inside another without any special handling.
 *
 * <p>A thread that still cannot get in after {@link #ACQUIRE_TIMEOUT_MS} logs and runs anyway.
 * That means some blocking call is missing its {@link #park}, and running without the guarantee
 * is better than freezing the game until someone restarts it.
 */
public final class EngineOwner implements IHasForgeLog {
    private static final int MAX_REPORTS_PER_SITE = 5;
    private static final long ACQUIRE_TIMEOUT_MS = 3000;
    private static final long SLOW_WAIT_MS = 100;
    private static final int MAX_SLOW_LOGS = 8;
    private static final long WORKER_TIMEOUT_MS = 3000;

    /** Lets static call sites park the game the calling thread entered, without a game reference. */
    private static final ThreadLocal<EngineOwner> ENTERED = new ThreadLocal<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private final ThreadLocal<int[]> depth = ThreadLocal.withInitial(() -> new int[1]);
    private final Map<String, AtomicInteger> waited = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> unowned = new ConcurrentHashMap<>();
    private final AtomicInteger lapses = new AtomicInteger();
    private final AtomicInteger slowLogs = new AtomicInteger();
    private ExecutorService worker;

    public void enter(final String site) {
        if (depth.get()[0]++ > 0) {
            return;
        }
        ENTERED.set(this);
        acquire(site);
    }

    public void exit() {
        final int[] d = depth.get();
        if (d[0] > 0 && --d[0] == 0) {
            ENTERED.remove();
            release();
        }
    }

    /** Returns the depth to hand back to {@link #unpark}; zero means this thread owned nothing. */
    public int park() {
        final int[] d = depth.get();
        final int held = d[0];
        if (held > 0) {
            d[0] = 0;
            release();
        }
        return held;
    }

    public void unpark(final int held, final String site) {
        if (held > 0) {
            depth.get()[0] = held;
            acquire(site);
        }
    }

    /** Park whichever game the calling thread entered, for call sites with no game to hand. */
    public static int parkCurrent() {
        final EngineOwner entered = ENTERED.get();
        return entered == null ? 0 : entered.park();
    }

    public static void unparkCurrent(final int held, final String site) {
        final EngineOwner entered = ENTERED.get();
        if (entered != null) {
            entered.unpark(held, site);
        }
    }

    /**
     * Report a change made by a thread that does not own the game. A thread that never calls
     * {@link #enter} never touches the lock, so this is the only place it can be noticed.
     */
    public void checkMutation(final String site) {
        if (depth.get()[0] > 0) {
            return;
        }
        record(unowned, site, owner.get());
    }

    private void acquire(final String site) {
        if (lock.tryLock()) {
            owner.set(Thread.currentThread());
            return;
        }
        record(waited, site, owner.get());
        try {
            if (lock.tryLock(SLOW_WAIT_MS, TimeUnit.MILLISECONDS)) {
                owner.set(Thread.currentThread());
                return;
            }
            reportSlowHolder(site);
            if (lock.tryLock(ACQUIRE_TIMEOUT_MS - SLOW_WAIT_MS, TimeUnit.MILLISECONDS)) {
                owner.set(Thread.currentThread());
                return;
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        lapses.incrementAndGet();
        netLog.error("[EngineOwner] {} gave up after {}ms and ran unowned on {} — owner={}",
                site, ACQUIRE_TIMEOUT_MS, Thread.currentThread().getName(), kind(owner.get()));
    }

    /** Taken while the other thread still holds the game; afterwards its stack tells us nothing. */
    private void reportSlowHolder(final String site) {
        final Thread holder = owner.get();
        if (holder == null || slowLogs.incrementAndGet() > MAX_SLOW_LOGS) {
            return;
        }
        final StringBuilder sb = new StringBuilder("[EngineOwner] ").append(site)
                .append(" on ").append(Thread.currentThread().getName())
                .append(" still waiting after ").append(SLOW_WAIT_MS).append("ms for ")
                .append(holder.getName()).append(", which is at:");
        final StackTraceElement[] stack = holder.getStackTrace();
        for (int i = 0; i < Math.min(18, stack.length); i++) {
            sb.append("\n\tat ").append(stack[i]);
        }
        netLog.warn(sb.toString());
    }

    private void release() {
        if (lock.isHeldByCurrentThread()) {
            owner.compareAndSet(Thread.currentThread(), null);
            lock.unlock();
        }
    }

    private void record(final Map<String, AtomicInteger> bucket, final String site, final Thread holder) {
        final String key = site + " on " + kind(Thread.currentThread()) + " vs " + kind(holder);
        final int n = bucket.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (n <= MAX_REPORTS_PER_SITE) {
            netLog.warn("[EngineOwner] {} (x{})", key, n);
        }
    }

    /** Thread names end in a number that differs every time, so group them by the part before it. */
    private static String kind(final Thread t) {
        if (t == null) {
            return "nobody";
        }
        final String name = t.getName();
        int end = name.length();
        while (end > 0 && Character.isDigit(name.charAt(end - 1))) {
            end--;
        }
        while (end > 0 && (name.charAt(end - 1) == '-' || name.charAt(end - 1) == ' ')) {
            end--;
        }
        return name.substring(0, end);
    }

    /**
     * Run the task here if this thread already owns the game, otherwise hand it to the game's
     * worker and wait. That keeps the UI thread from walking the graph itself. The caller waits
     * either way, so everything still happens in the same order.
     */
    public void run(final Runnable task) {
        if (depth.get()[0] > 0) {
            task.run();
            return;
        }
        // Claimed by whichever side gets there first, so the task never runs twice over
        final AtomicBoolean claimed = new AtomicBoolean();
        final Future<?> pending = worker().submit(() -> {
            if (!claimed.compareAndSet(false, true)) {
                return;
            }
            enter("worker");
            try {
                task.run();
            } finally {
                exit();
            }
        });
        try {
            pending.get(WORKER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            if (claimed.compareAndSet(false, true)) {
                // The worker never started, so it is stuck waiting for the game. Run it here
                // rather than wait for something that may never come.
                pending.cancel(false);
                lapses.incrementAndGet();
                netLog.error("[EngineOwner] worker could not start within {}ms, running on {} instead — owner={}",
                        WORKER_TIMEOUT_MS, Thread.currentThread().getName(), kind(owner.get()));
                task.run();
            } else {
                // Already running, so it holds the game and is getting on with it. Waiting is
                // slow; running it here as well would put two threads through the same work.
                netLog.warn("[EngineOwner] worker still busy after {}ms, waiting on {}",
                        WORKER_TIMEOUT_MS, Thread.currentThread().getName());
                awaitQuietly(pending);
            }
        } catch (final InterruptedException e) {
            pending.cancel(false);
            netLog.warn("[EngineOwner] interrupted while handing work to the worker on {}",
                    Thread.currentThread().getName());
            Thread.currentThread().interrupt();
        } catch (final ExecutionException e) {
            // Deliberate: the inline path would have thrown the cause where it stood
            throw new IllegalStateException(e.getCause());
        }
    }

    private static void awaitQuietly(final Future<?> pending) {
        try {
            pending.get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    private synchronized ExecutorService worker() {
        if (worker == null) {
            worker = Executors.newSingleThreadExecutor(r -> {
                // "Game" prefix so ThreadUtil.isGameThread stays true for anything it calls
                final Thread t = new Thread(r, "Game Worker");
                t.setDaemon(true);
                return t;
            });
        }
        return worker;
    }

    /** Report what happened and reap the worker; one per game accumulates otherwise. */
    public synchronized void shutdown() {
        if (worker != null) {
            worker.shutdown();
            worker = null;
        }
        netLog.info("[EngineOwner] lapses: {}", lapses.get());
        waited.forEach((site, n) -> netLog.warn("[EngineOwner] waited {} x{}", site, n.get()));
        unowned.forEach((site, n) -> netLog.warn("[EngineOwner] unowned {} x{}", site, n.get()));
    }
}
