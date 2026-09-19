package forge.util;

import java.util.concurrent.*;

public class ThreadUtil {
    // Reusable ThreadPool for AI Timeout
    public static final ThreadPoolExecutor AIExecutor = new ThreadPoolExecutor(
            0, Runtime.getRuntime().availableProcessors(),
            1L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            r -> {
                Thread t = new Thread(r, "AI ThreadPool");
                t.setDaemon(true);
                return t;
            },
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
        System.out.printf("(ThreadUtil first call): Running with priority %d%n", Thread.currentThread().getPriority());
        // Allow core threads to die when they have no work
        AIExecutor.allowCoreThreadTimeOut(true);
    }

    private static class WorkerThreadFactory implements ThreadFactory {
        private int countr = 0;
        private String prefix = "";

        public WorkerThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + countr++);
            t.setDaemon(true); //set daemon to true for succesfully exiting the game while disposing assets
            return t;
        }
    }

    private final static ExecutorService gameThreadPool = Executors.newCachedThreadPool(new WorkerThreadFactory("Game"));
    private static ExecutorService getGameThreadPool() { return gameThreadPool; }
    private final static ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(2, new WorkerThreadFactory("Delayed"));
    private static ScheduledExecutorService getScheduledPool() { return scheduledPool; }

    // This pool is designed to parallel CPU or IO intensive tasks like parse cards or download images, assuming a load factor of 0.5
    public final static ExecutorService getComputingPool(float loadFactor) {
        return Executors.newFixedThreadPool((int) (Runtime.getRuntime().availableProcessors() / (1 - loadFactor)), r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
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

    private static ExecutorService service = Executors.newWorkStealingPool();
    public static ExecutorService getServicePool() {
        return service;
    }
    public static void refreshServicePool() {
        service = Executors.newWorkStealingPool();
    }
    public static <T> T limit(Callable<T> task, long millis){
        Future<T> future = null;
        T result;
        try {
            future = service.submit(task);
            result = future.get(millis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            result = null;
        } finally {
            if (future != null)
                future.cancel(true);
        }
        return result;
    }
    public static <T> T executeWithTimeout(Callable<T> task, int milliseconds) {
        ExecutorService executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        Future<T> future = executor.submit(task);
        T result;
        try {
            result = future.get(milliseconds, TimeUnit.MILLISECONDS); 
        }
        catch (Exception e) { //handle timeout and other exceptions
            e.printStackTrace();
            result = null;
        }
        finally {
           future.cancel(true);
        }
        return result;
    }
}