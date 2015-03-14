package forge.net;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Maps;

public class ReplyPool {

    private final Map<Integer, CompletableFuture<Object>> pool = Maps.newHashMap();

    public ReplyPool() {
    }

    public void initialize(final int index) {
        synchronized (pool) {
            pool.put(Integer.valueOf(index), new CompletableFuture<Object>());
        }
    }

    public void complete(final int index, final Object value) {
        synchronized (pool) {
            pool.get(Integer.valueOf(index)).complete(value);
        }
    }

    public Object get(final int index) throws TimeoutException {
        final CompletableFuture<Object> future;
        synchronized (pool) {
            future = pool.get(Integer.valueOf(index));
        }
        try {
            return future.get(1, TimeUnit.MINUTES);
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
