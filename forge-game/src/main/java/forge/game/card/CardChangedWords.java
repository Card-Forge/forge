package forge.game.card;

import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public final class CardChangedWords {

    private final SortedMap<Long, CardChangedWord> map = Maps.newTreeMap();

    private boolean isDirty = false;
    private Map<String, String> resultCache = Maps.newHashMap();

    public CardChangedWords() {
    }

    public Long add(final long timestamp, final String originalWord, final String newWord) {
        final Long stamp = Long.valueOf(timestamp);
        map.put(stamp, new CardChangedWord(originalWord, newWord));
        isDirty = true;
        return stamp;
    }

    public boolean remove(final Long timestamp) {
        isDirty = true;
        return map.remove(timestamp) != null;
    }

    public void removeAll() {
        map.clear();
        isDirty = true;
    }

    void copyFrom(final CardChangedWords other) {
        map.clear();
        map.putAll(other.map);
        isDirty = true;
    }

    /**
     * Converts this object to a {@link Map}.
     *
     * @return a map of strings to strings, where each changed word in this
     * object is mapped to its corresponding replacement word.
     */
    public Map<String, String> toMap() {
        refreshCache();
        return resultCache;
    }

    private void refreshCache() {
        if (isDirty) {
            resultCache = Maps.newHashMap();
            for (final CardChangedWord ccw : this.map.values()) {
                // changes because a->b and b->c (resulting in a->c)
                final Map<String, String> toBeChanged = Maps.newHashMap();
                for (final Entry<String, String> e : resultCache.entrySet()) {
                    if (e.getValue().equals(ccw.getOriginalWord())) {
                        toBeChanged.put(e.getKey(), ccw.getNewWord());
                    }
                }
                resultCache.putAll(toBeChanged);

                // the actual change (b->c)
                resultCache.put(ccw.getOriginalWord(), ccw.getNewWord());
            }

            // TODO should that be removed?
            for (final String key : ImmutableList.copyOf(resultCache.keySet())) {
                if (!key.equals("Any")) {
                    resultCache.put(key.toLowerCase(), resultCache.get(key).toLowerCase());
                }
            }
            isDirty = false;
        }
    }
}
