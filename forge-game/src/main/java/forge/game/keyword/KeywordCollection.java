package forge.game.keyword;

import java.io.Serializable;

import java.util.Collection;
import java.util.Iterator;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

public class KeywordCollection implements Iterable<String>, Serializable {
    private static final long serialVersionUID = -2882986558147844702L;

    private transient KeywordCollectionView view;
    private final Multimap<Keyword, KeywordInterface> map = MultimapBuilder.enumKeys(Keyword.class)
            .arrayListValues().build();

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

    public boolean add(String k) {
        KeywordInterface inst = Keyword.getInstance(k);
        Keyword keyword = inst.getKeyword();
        Collection<KeywordInterface> list = map.get(keyword);
        if (list.isEmpty() || !keyword.isMultipleRedundant) {
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

    public boolean remove(String keyword) {
        Iterator<KeywordInterface> it = map.values().iterator();
        
        boolean result = false;
        while (it.hasNext()) {
            KeywordInterface k = it.next();
            if (keyword.equals(k.getOriginal())) {
                it.remove();
                result = true;
            }
        }
        
        return result;
    }

    public void removeAll(Iterable<String> keywords) {
        for (String k : keywords) {
            remove(k);
        }
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
