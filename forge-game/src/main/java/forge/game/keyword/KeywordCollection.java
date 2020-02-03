package forge.game.keyword;

import java.io.Serializable;

import java.util.Collection;
import java.util.Iterator;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import forge.game.card.Card;

public class KeywordCollection implements Iterable<String>, Serializable {
    private static final long serialVersionUID = -2882986558147844702L;

    private boolean hidden = false;

    private transient KeywordCollectionView view;
    private final Multimap<Keyword, KeywordInterface> map = MultimapBuilder.enumKeys(Keyword.class)
            .arrayListValues().build();

    public KeywordCollection() {
        super();
        this.hidden = false;
    }
    public KeywordCollection(boolean hidden) {
        super();
        this.hidden = hidden;
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

    public int getAmount(Keyword keyword) {
        int amount = 0;
        for (KeywordInterface inst : map.get(keyword)) {
            amount += inst.getAmount();
        }
        return amount;
    }

    public KeywordInterface add(String k) {
        KeywordInterface inst = Keyword.getInstance(k);
        inst.setHidden(hidden);
        if (insert(inst)) {
            return inst;
        }
        return null;
    }
    public boolean insert(KeywordInterface inst) {
        Keyword keyword = inst.getKeyword();
        Collection<KeywordInterface> list = map.get(keyword);
        if (list.isEmpty() || !inst.redundant(list)) {
            list.add(inst);
            return true;
        }
        return false;

    }

    public void addAll(Iterable<String> keywords) {
        for (String k : keywords) {
            add(k);
        }
    }

    public boolean insertAll(Iterable<KeywordInterface> inst) {
        boolean result = false;
        for (KeywordInterface k : inst) {
            if (insert(k)) {
                result = true;
            }
        }
        return result;
    }

    public boolean remove(String keyword) {
        Iterator<KeywordInterface> it = map.values().iterator();

        boolean result = false;
        while (it.hasNext()) {
            KeywordInterface k = it.next();
            if (k.getOriginal().startsWith(keyword)) {
                it.remove();
                result = true;
            }
        }

        return result;
    }

    public boolean remove(KeywordInterface keyword) {
        return map.remove(keyword.getKeyword(), keyword);
    }

    public boolean removeAll(Keyword kenum) {
        return !map.removeAll(kenum).isEmpty();
    }

    public boolean removeAll(Iterable<String> keywords) {
        boolean result = false;
        for (String k : keywords) {
            if (remove(k)) {
                result = true;
            }
        }
        return result;
    }

    public boolean removeInstances(Iterable<KeywordInterface> keywords) {
        boolean result = false;
        for (KeywordInterface k : keywords) {
            if (map.remove(k.getKeyword(), k)) {
                result = true;
            }
        }
        return result;
    }

    public void clear() {
        map.clear();
    }

    public boolean contains(String keyword) {
        for (KeywordInterface inst : map.values()) {
            if (keyword.equals(inst.getOriginal())) {
                return true;
            }
        }
        return false;
    }

    public int getAmount(String k) {
        int amount = 0;
        for (KeywordInterface inst : map.values()) {
            if (k.equals(inst.getOriginal())) {
                amount++;
            }
        }
        return amount;
    }

    public Collection<KeywordInterface> getValues() {
        return map.values();
    }

    public Collection<KeywordInterface> getValues(final Keyword keyword) {
        return map.get(keyword);
    }

    public void setHostCard(final Card host) {
        for (KeywordInterface k : map.values()) {
            k.setHostCard(host);
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private final Iterator<KeywordInterface> iterator = map.values().iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public String next() {
                KeywordInterface entry = iterator.next();
                return entry.getOriginal();
            }

            @Override
            public void remove() {
                //Don't support this
            }
        };
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb  = new StringBuilder();

        sb.append(map.values());
        return sb.toString();
    }

    public KeywordCollectionView getView() {
        if (view == null) {
            view = new KeywordCollectionView();
        }
        return view;
    }

    public class KeywordCollectionView implements Iterable<String>, Serializable {
        private static final long serialVersionUID = 7536969077044188264L;

        protected KeywordCollectionView() {
        }

        public boolean isEmpty() {
            return KeywordCollection.this.isEmpty();
        }

        public int size() {
            return KeywordCollection.this.size();
        }

        public int getAmount(String keyword) {
            return KeywordCollection.this.getAmount(keyword);
        }

        public boolean contains(Keyword keyword) {
            return KeywordCollection.this.contains(keyword);
        }
        public boolean contains(String keyword) {
            return KeywordCollection.this.contains(keyword);
        }

        @Override
        public Iterator<String> iterator() {
            return KeywordCollection.this.iterator();
        }
    }
}
