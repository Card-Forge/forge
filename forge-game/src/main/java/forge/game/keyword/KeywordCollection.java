package forge.game.keyword;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class KeywordCollection implements Iterable<String>, Serializable {
    private static final long serialVersionUID = -2882986558147844702L;

    private transient KeywordCollectionView view;
    private final EnumMap<Keyword, List<KeywordInstance<?>>> map = new EnumMap<Keyword, List<KeywordInstance<?>>>(Keyword.class);

    public boolean contains(Keyword keyword) {
        return map.containsKey(keyword);
    }

    public boolean isEmpty() {
        return stringMap.isEmpty(); //TODO: Replace with map when stringMap goes away
    }

    public int size() {
        return stringMap.size(); //TODO: Replace with map when stringMap goes away
    }

    public int getAmount(Keyword keyword) {
        int amount = 0;
        List<KeywordInstance<?>> instances = map.get(keyword);
        if (instances != null) {
            for (KeywordInstance<?> inst : instances) {
                amount += inst.getAmount();
            }
        }
        return amount;
    }

    public void add(String k) {
        KeywordInstance<?> inst = Keyword.getInstance(k);
        Keyword keyword = inst.getKeyword();
        List<KeywordInstance<?>> list = map.get(keyword);
        if (list == null) {
            list = new ArrayList<KeywordInstance<?>>();
            list.add(inst);
            map.put(keyword, list);
            stringMap.put(k, inst.getAmount());
        }
        else if (!keyword.isMultipleRedundant) {
            list.add(inst);
            int amount = 0;
            for (KeywordInstance<?> i : list) {
                amount += i.getAmount();
            }
            stringMap.put(k, amount);
        }
    }

    public void addAll(Iterable<String> keywords) {
        for (String k : keywords) {
            add(k);
        }
    }

    public void remove(String keyword) {
        int amount = getAmount(keyword);
        switch (amount) {
        case 0:
            break;
        case 1:
            stringMap.remove(keyword);
            break;
        default:
            stringMap.put(keyword, amount - 1);
        }
    }

    public void removeAll(Iterable<String> keywords) {
        for (String k : keywords) {
            remove(k);
        }
    }

    public void clear() {
        map.clear();
        stringMap.clear();
    }

    //Below is temporary code to mimic the current List<String>
    //TODO: Remove when keywords no longer implemented that way
    private HashMap<String, Integer> stringMap = new HashMap<String, Integer>();
    public boolean contains(String keyword) {
        return stringMap.containsKey(keyword);
    }

    public int getAmount(String keyword) {
        Integer amount = stringMap.get(keyword);
        return amount == null ? 0 : amount.intValue();
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private final Iterator<Entry<String, Integer>> iterator = stringMap.entrySet().iterator();
            private String entryKey;
            private int entryRemainder = 0;

            @Override
            public boolean hasNext() {
                return entryRemainder > 0 || iterator.hasNext();
            }

            @Override
            public String next() {
                if (entryRemainder > 0) {
                    entryRemainder--;
                    return entryKey;
                }
                Entry<String, Integer> entry = iterator.next();
                entryKey = entry.getKey();
                entryRemainder = entry.getValue() - 1;
                return entryKey;
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
            Integer amount = stringMap.get(keyword);
            return amount == null ? 0 : amount.intValue();
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
