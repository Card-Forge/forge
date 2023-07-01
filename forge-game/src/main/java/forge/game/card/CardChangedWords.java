package forge.game.card;

import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

public final class CardChangedWords extends ForwardingMap<String, String> {

    class WordHolder {
        public String oldWord;
        public String newWord;

        public boolean clear = false;

        WordHolder() {
            this.clear = true;
        }
        WordHolder(String oldWord, String newWord) {
            this.oldWord = oldWord;
            this.newWord = newWord;
        }
    }

    private final Table<Long, Long, WordHolder> map = TreeBasedTable.create();

    private boolean isDirty = false;
    private Map<String, String> resultCache = Maps.newHashMap();

    public CardChangedWords() {
    }

    public Long addEmpty(final long timestamp, final long staticId) {
        final Long stamp = Long.valueOf(timestamp);
        map.put(stamp, staticId, new WordHolder()); // Table doesn't allow null value
        isDirty = true;
        return stamp;
    }

    public Long add(final long timestamp, final long staticId, final String originalWord, final String newWord) {
        final Long stamp = Long.valueOf(timestamp);
        map.put(stamp, staticId, new WordHolder(originalWord, newWord));
        isDirty = true;
        return stamp;
    }

    public boolean remove(final Long timestamp, final long staticId) {
        isDirty = true;
        return map.remove(timestamp, staticId) != null;
    }

    @Override
    public void clear() {
        super.clear();
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
    @Override
    protected Map<String, String> delegate() {
        refreshCache();
        return resultCache;
    }

    private void refreshCache() {
        if (isDirty) {
            resultCache.clear();
            for (final WordHolder ccw : this.map.values()) {
                // is empty pair is for resetting the data, it is done for Volrath’s Shapeshifter
                if (ccw.clear) {
                    resultCache.clear();
                    continue;
                }

                // changes because a->b and b->c (resulting in a->c)
                final Map<String, String> toBeChanged = Maps.newHashMap();
                for (final Entry<String, String> e : resultCache.entrySet()) {
                    if (e.getValue().equals(ccw.oldWord)) {
                        toBeChanged.put(e.getKey(), ccw.newWord);
                    }
                }
                resultCache.putAll(toBeChanged);

                // the actual change (b->c)
                resultCache.put(ccw.oldWord, ccw.newWord);
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
