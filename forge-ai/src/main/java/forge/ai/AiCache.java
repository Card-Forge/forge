package forge.ai;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class AiCache {

    /** How long a cached answer stays good, and which threads may share it. */
    public enum Scope {
        /** One priority pass, shared between threads. Cleared when the AI takes priority. */
        PRIORITY,
        /**
         * One loop over a list of candidates, on the thread running it. An answer that reads the
         * combat cannot outlive its loop, because combat changes as blockers are assigned. The
         * loop clears this on entry.
         */
        CALL
    }

    /** Answers matched against an argument tuple, and answers looked up by a key the caller builds. */
    private static final class Store {
        private final boolean shared;
        private final Multimap<String, List<Object>> tuples;
        private final Map<String, Map<Object, Object>> keyed;

        private Store(boolean shared) {
            this.shared = shared;
            this.tuples = shared
                    ? Multimaps.synchronizedMultimap(ArrayListMultimap.create())
                    : ArrayListMultimap.create();
            this.keyed = shared ? new ConcurrentHashMap<>() : new HashMap<>();
        }

        private Map<Object, Object> keyed(String name) {
            return keyed.computeIfAbsent(name, k -> shared ? new ConcurrentHashMap<>() : new HashMap<>());
        }

        private void clear() {
            tuples.clear();
            keyed.clear();
        }
    }

    private final static Store priorityStore = new Store(true);
    private final static ThreadLocal<Store> callStore = ThreadLocal.withInitial(() -> new Store(false));

    private static Store store(Scope scope) {
        return scope == Scope.PRIORITY ? priorityStore : callStore.get();
    }

    public static boolean identity(Object a, Object b) {
        return a == b;
    }

    // the cache is global for calculations that can be shared between games
    // but that also means unwanted collisions need to be considered:
    // for that you can pass Functions that compare the args
    public static <T> T getCached(Scope scope, String key, Supplier<T> func, List<BiFunction<Object, Object, Boolean>> argsCheck, Object... args) {
        // TODO would like a good strategy to derive default key, but there's no clean way to obtain the method name
        Multimap<String, List<Object>> tuples = store(scope).tuples;
        for (List<Object> cached : Lists.newArrayList(tuples.get(key))) {
            boolean hit = true;
            for (int i = 0; i < args.length; i++) {
                BiFunction<Object, Object, Boolean> checker = argsCheck == null ? Object::equals : argsCheck.get(i);
                if (!checker.apply(args[i], cached.get(i + 1))) {
                    hit = false;
                    break;
                }
            }
            if (hit) {
                return (T) cached.get(0);
            }
        }
        T result = func.get();
        List<Object> cached = Lists.newArrayList(result);
        cached.addAll(Arrays.asList(args));
        tuples.put(key, cached);
        return result;
    }

    /**
     * For answers the caller can name with a key. Where getCached compares argument tuples one at a
     * time, this looks the key up, so it stays cheap when a scope holds thousands of answers.
     */
    public static <T> T memo(Scope scope, String name, Object key, Supplier<T> func) {
        Map<Object, Object> byKey = store(scope).keyed(name);
        Object cached = byKey.get(key);
        if (cached != null) {
            return (T) cached;
        }
        T result = func.get();
        byKey.put(key, result);
        return result;
    }

    // TODO add a staleness indicator
    public static void clear(Scope scope) {
        store(scope).clear();
    }

}
