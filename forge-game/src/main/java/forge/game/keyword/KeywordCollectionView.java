package forge.game.keyword;

import java.util.Collection;
import java.util.Iterator;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

public class KeywordCollectionView implements Iterable<KeywordView> {
    // don't use enumKeys it causes a slow down
    private final Multimap<Keyword, KeywordView> map = MultimapBuilder.hashKeys()
            .linkedHashSetValues().build();

    public KeywordCollectionView(Iterable<KeywordView> list) {
        for (KeywordView k : list) {
            map.put(k.keyword(), k);
        }
    }

    public boolean contains(Keyword keyword) {
        return map.containsKey(keyword);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public int size() {
        return map.values().size();
    }

    public Collection<KeywordView> getValues() {
        return map.values();
    }

    public Collection<KeywordView> getValues(final Keyword keyword) {
        return map.get(keyword);
    }

    @Override
    public Iterator<KeywordView> iterator() {
        return this.map.values().iterator();
    }
}
