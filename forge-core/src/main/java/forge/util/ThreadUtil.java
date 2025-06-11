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

    /* Image Retrieval */
    /**
     * @implNote Operations on these threads will be network IO-bound, so
     * adjusting the count based on available processors should not be necessary.
     */
    private static final int IMAGE_FETCH_THREAD_COUNT = 10;
    private static final int IMAGE_FETCH_DELAY_IN_MILLIS = 1000;
    private static ScheduledExecutorService scheduledImageFetchService = Executors.newScheduledThreadPool(IMAGE_FETCH_THREAD_COUNT);
    private static ScheduledExecutorService getImageFetcherService() {
        return scheduledImageFetchService;
    }

    /**
     * Fetch an image using a dedicated thread pool that enforces adherence to
     * rate-limiting requirements.
     *
     * @implSpec Scryfall <a href="https://scryfall.com/docs/api#rate-limits-and-good-citizenship">specifies</a>
     * a ~10 request/sec rate limit for their API.
     * @implNote Limiting the thread pool to no more than 10 threads at a
     * 1s schedule interval, we ensure that no more than 10 requests
     * are made each second while simultaneously maximizing the retrieval rate.
     * Due to the latency of the HTTP request-response cycle, using fewer threads would
     * result in a lower rate of retrieval than the rate-limit limit requires.
     */
    public static void scheduleImageFetch(Runnable runnable) {
        getImageFetcherService().schedule(runnable, IMAGE_FETCH_DELAY_IN_MILLIS, TimeUnit.MILLISECONDS);
    }

}