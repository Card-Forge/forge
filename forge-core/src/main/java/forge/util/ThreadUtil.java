package forge.util;

import java.util.concurrent.*;

public class ThreadUtil {
    static {
        System.out.printf("(ThreadUtil first call): Running on a machine with %d cpu core(s)%n", Runtime.getRuntime().availableProcessors() );
    }

    private static class WorkerThreadFactory implements ThreadFactory {
        private int countr = 0;
        private String prefix = "";

        public WorkerThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        public Thread newThread(Runnable r) {
            return new Thread(r, prefix + "-" + countr++);
        }
    }

    private final static ExecutorService gameThreadPool = Executors.newCachedThreadPool(new WorkerThreadFactory("Game"));
    private static ExecutorService getGameThreadPool() { return gameThreadPool; }
    private final static ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(2, new WorkerThreadFactory("Delayed"));
    private static ScheduledExecutorService getScheduledPool() { return scheduledPool; }

    // This pool is designed to parallel CPU or IO intensive tasks like parse cards or download images, assuming a load factor of 0.5
    public final static ExecutorService getComputingPool(float loadFactor) {
        return Executors.newFixedThreadPool((int)(Runtime.getRuntime().availableProcessors() / (1-loadFactor)));
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

    public static <T> T executeWithTimeout(Callable<T> task, int milliseconds) {
        ExecutorService executor = Executors.newCachedThreadPool();
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