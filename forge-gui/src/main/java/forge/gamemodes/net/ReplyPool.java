package forge.gamemodes.net;

import java.util.Map;
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
            pool.put(index, new CompletableFuture());
        }
    }

    public void complete(final int index, final Object value) {
        synchronized (pool) {
            pool.get(index).set(value);
        }
    }

    public Object get(final int index) throws TimeoutException {
        final CompletableFuture future;
        synchronized (pool) {
            future = pool.get(index);
        }
        try {
            return future.get(5, TimeUnit.MINUTES);
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class CompletableFuture extends FutureTask<Object> {
        public CompletableFuture() {
            super(() -> null);
        }

        @Override
        public void set(final Object v) {
            super.set(v);
        }
    }
}
