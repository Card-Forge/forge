package forge.util;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadUtil {

    // Idle threads are not kept around: Android's heap/thread budget is small and AI work comes in short bursts.
    // Every pool below that this class owns lets ALL of its threads die after this long, so an idle pool holds zero
    // threads (AITimeoutTest asserts AIExecutor.getPoolSize() == 0 shortly after a burst).
    private static final long IDLE_KEEPALIVE_SECONDS = 1L;

    // ART reports "Dalvik" as java.vm.name for historical reasons, and "The Android Project" as vendor.
    private static final boolean IS_ANDROID =
            "Dalvik".equals(System.getProperty("java.vm.name"))
                    || String.valueOf(System.getProperty("java.vendor")).contains("Android");

    // On phones availableProcessors() counts every little/efficiency core too, and the render + UI threads
    // need headroom. Desktop keeps the old behaviour (one thread per core).
    // MINIMUM IS 2 ON PURPOSE: AI code can nest (a worker evaluating a spell calls getPredictedCombat(), which
    // submits per-attacker tasks to this same pool and blocks on a latch). With a single thread that nested wait can
    // never be served and stalls for the whole AI timeout. Android may also report fewer cores in power-save mode.
    private static final int AI_THREADS = IS_ANDROID
            ? Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() - 1))
            : Math.max(2, Runtime.getRuntime().availableProcessors());

    /** Marker so code running on the AI pool can detect it and avoid blocking on the same pool. */
    private static final class AIWorkerThread extends Thread {
        AIWorkerThread(Runnable r) {
            super(r, "AI ThreadPool");
            setDaemon(true);
        }
    }

    /** True when called from an AI pool worker. */
    public static boolean isAIThread() {
        return Thread.currentThread() instanceof AIWorkerThread;
    }

    // Reusable ThreadPool for AI Timeout.
    public static final ThreadPoolExecutor AIExecutor = new ThreadPoolExecutor(
            AI_THREADS, AI_THREADS,
            IDLE_KEEPALIVE_SECONDS, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            AIWorkerThread::new,
            new ThreadPoolExecutor.DiscardPolicy()
    ) {
        // This hooks into .submit() or .execute() automatically
        @Override
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            return new TrackableFutureTask<>(callable);
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
            return new TrackableFutureTask<>(runnable, value);
        }

        // Ensure execute() tasks are also cleaned
        @Override
        public void execute(Runnable command) {
            super.execute(new SafeInterruptWrapper(command));
        }

    };

    public static class TrackableFutureTask<V> extends FutureTask<V> {
        private volatile Thread runnerThread;

        public TrackableFutureTask(Callable<V> callable) {
            super(callable);
        }

        public TrackableFutureTask(Runnable runnable, V result) {
            super(runnable, result);
        }

        @Override
        public void run() {
            runnerThread = Thread.currentThread();
            try {
                super.run();
            } finally {
                runnerThread = null;
                // Clear interrupt flag before returning thread to pool
                Thread.interrupted();
            }
        }

        public Thread getRunnerThread() {
            return runnerThread;
        }
    }

    public static class SafeInterruptWrapper implements Runnable {
        private final Runnable delegate;

        public SafeInterruptWrapper(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try {
                delegate.run();
            } finally {
                Thread.interrupted(); // clear interrupt flag
            }
        }
    }

    public static void checkInterrupt() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            Thread.interrupted(); // so we don't need to remember where we miss to clear the flag
            throw new InterruptedException("AI evaluation interrupted");
        }
    }

    static {
        System.out.printf("(ThreadUtil first call): Running with priority %d, AI threads %d%n",
                Thread.currentThread().getPriority(), AI_THREADS);
        // 'core' threads expire too: the pool shrinks back to zero threads IDLE_KEEPALIVE_SECONDS after the last task
        AIExecutor.allowCoreThreadTimeOut(true);
    }

    private static class WorkerThreadFactory implements ThreadFactory {
        private final AtomicInteger countr = new AtomicInteger();
        private final String prefix;

        public WorkerThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + countr.getAndIncrement());
            t.setDaemon(true); //set daemon to true for succesfully exiting the game while disposing assets
            return t;
        }
    }

    private final static ExecutorService gameThreadPool = Executors.newCachedThreadPool(new WorkerThreadFactory("Game"));
    private static ExecutorService getGameThreadPool() { return gameThreadPool; }
    private final static ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(2, new WorkerThreadFactory("Delayed"));
    private static ScheduledExecutorService getScheduledPool() { return scheduledPool; }

    // Shared pool for executeWithTimeout(). Previously every call built (and never shut down) its own cached pool,
    // leaving one idle thread alive for 60s per call. Same behaviour as a cached pool, but idle threads expire after 1s.
    private final static ExecutorService timeoutPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            IDLE_KEEPALIVE_SECONDS, TimeUnit.SECONDS, new SynchronousQueue<>(), new WorkerThreadFactory("Timeout"));

    // This pool is designed to parallel CPU or IO intensive tasks like parse cards, assuming a load factor of 0.5
    // The caller still owns the returned pool, but idle threads now time out so a forgotten shutdown() no longer leaks them.
    public final static ExecutorService getComputingPool(float loadFactor) {
        float lf = Math.max(0f, Math.min(loadFactor, 0.9f)); // 1.0 used to divide by zero -> Integer.MAX_VALUE threads
        int threads = Math.max(1, (int) (Runtime.getRuntime().availableProcessors() / (1 - lf)));
        ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, IDLE_KEEPALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }

    public static boolean isMultiCoreSystem() {
        return Runtime.getRuntime().availableProcessors() > 1;
    }

    public static void invokeInGameThread(Runnable toRun) {
        getGameThreadPool().execute(toRun);
    }

    public static ScheduledFuture<?> delay(int milliseconds, Runnable inputUpdater) {
        return getScheduledPool().schedule(inputUpdater, milliseconds, TimeUnit.MILLISECONDS);
    }

    public static boolean isGameThread() {
        return Thread.currentThread().getName().startsWith("Game");
    }

    // this is used for IO operations used by image fetcher, set lookup so don't compete in main rendering thread and set this to minimum priority
    private final static AtomicInteger ioThreadCounter = new AtomicInteger();
    private final static ExecutorService serviceIO = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Service-IO-" + ioThreadCounter.getAndIncrement());
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    public static ExecutorService getServicePool() {
        return serviceIO;
    }

    public static <T> T limit(Callable<T> task, long millis){
        Future<T> future = null;
        T result;
        try {
            future = serviceIO.submit(task);
            result = future.get(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // don't swallow the caller's interrupt
            result = null;
        } catch (Exception e) {
            result = null;
        } finally {
            if (future != null)
                future.cancel(true);
        }
        return result;
    }

    public static <T> T executeWithTimeout(Callable<T> task, int milliseconds) {
        Future<T> future = timeoutPool.submit(task);
        T result;
        try {
            result = future.get(milliseconds, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e) {
            System.err.println("executeWithTimeout: timed out after " + milliseconds + "ms");
            result = null;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = null;
        }
        catch (Exception e) { //other failures from the task itself
            e.printStackTrace();
            result = null;
        }
        finally {
            future.cancel(true);
        }
        return result;
    }
}
