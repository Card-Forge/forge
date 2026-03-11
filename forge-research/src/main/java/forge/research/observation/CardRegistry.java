package forge.research.observation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maps card names to stable integer IDs for observation encoding.
 * Thread-safe. IDs are assigned incrementally on first encounter.
 */
public class CardRegistry {

    private static final CardRegistry INSTANCE = new CardRegistry();

    private final ConcurrentHashMap<String, Integer> nameToId = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public static CardRegistry getInstance() {
        return INSTANCE;
    }

    public int getNameId(String cardName) {
        return nameToId.computeIfAbsent(cardName, k -> nextId.getAndIncrement());
    }

    public int size() {
        return nameToId.size();
    }
}
