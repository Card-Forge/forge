package forge.net;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Maps;

public class ReplyPool {

    private final Map<Integer, CompletableFuture> pool = Maps.newHashMap();

    public ReplyPool() {
    }

    public void initialize(final int index) {
        synchronized (pool) {
            pool.put(Integer.valueOf(index), new CompletableFuture());
        }
    }

    public void complete(final int index, final Object value) {
        synchronized (pool) {
            pool.get(Integer.valueOf(index)).set(value);
        }
    }

    public Object get(final int index) throws TimeoutException {
        final CompletableFuture future;
        synchronized (pool) {
            future = pool.get(Integer.valueOf(index));
        }
        try {
            return future.get(1, TimeUnit.MINUTES);
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class CompletableFuture extends FutureTask<Object> {
        public CompletableFuture() {
            super(new Callable<Object>() {
                @Override public Object call() throws Exception {
                    return null;
                }
            });
        }

        @Override
        public void set(final Object v) {
            super.set(v);
        }
    }
}
