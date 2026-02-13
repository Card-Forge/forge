package forge.util.maps;

import java.util.LinkedHashMap;

public class LinkedHashMapToAmount<T> extends LinkedHashMap<T, Integer> implements MapToAmount<T> {
    private static final long serialVersionUID = 1438913784333297606L;

    /**
     * 
     */
    public LinkedHashMapToAmount() {
        super();
    }

    /**
     * @param arg0
     */
    public LinkedHashMapToAmount(final int arg0) {
        super(arg0);
    }

    @Override
    public Integer put(final T key, final Integer value) {
        if (value == null) {
            throw new NullPointerException("Trying to put a key in a HashMapToAmount to null.");
        }
        return super.put(key, value);
    }

    @Override
    public void add(final T item) {
        add(item, 1);
    }

    @Override
    public void add(final T item, final int amount) {
        if (amount <= 0) { return; } // throw an exception maybe?
        Integer cur = get(item);
        int newVal = cur == null ? amount : amount + cur;
        put(item, newVal);
    }

    @Override
    public void addAll(final Iterable<T> items) {
        for (T i : items) {
            add(i, 1);
        }
    }

    @Override
    public int countAll() {
        int c = 0;
        for (java.util.Map.Entry<T, Integer> kv : this.entrySet()) {
            c += kv.getValue();
        }
        return c;
    }

    @Override
    public int count(final T item) {
        Integer cur = get(item);
        return cur == null ? 0 : cur;
    }
    
}
