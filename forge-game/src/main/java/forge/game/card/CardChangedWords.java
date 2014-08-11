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

    public void remove(final Long timestamp) {
        map.remove(timestamp);
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
                for (final Entry<String, String> e : resultCache.entrySet()) {
                    if (e.getValue().equals(ccw.getOriginalWord())) {
                        e.setValue(ccw.getNewWord());
                    }
                }

                resultCache.put(ccw.getOriginalWord(), ccw.getNewWord());
            }
            for (final String key : ImmutableList.copyOf(resultCache.keySet())) {
                if (!key.equals("Any")) {
                    resultCache.put(key.toLowerCase(), resultCache.get(key).toLowerCase());
                }
            }
            isDirty = false;
        }
    }
}
