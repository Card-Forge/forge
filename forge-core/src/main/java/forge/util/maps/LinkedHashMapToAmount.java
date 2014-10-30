package forge.util.maps;

import java.util.LinkedHashMap;

public class LinkedHashMapToAmount<T> extends LinkedHashMap<T, Integer> implements MapToAmount<T> {
    private static final long serialVersionUID = 1438913784333297606L;

    public static <T> LinkedHashMapToAmount<T> emptyMap() {
        return new LinkedHashMapToAmount<T>(0);
    }

    /**
     * 
     */
    public LinkedHashMapToAmount() {
        super();
    }

    /**
     * @param arg0
     * @param arg1
     */
    public LinkedHashMapToAmount(final int arg0, final float arg1) {
        super(arg0, arg1);
    }

    /**
     * @param arg0
     */
    public LinkedHashMapToAmount(final int arg0) {
        super(arg0);
    }

    /**
     * @param arg0
     */
    public LinkedHashMapToAmount(final MapToAmount<T> arg0) {
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
        int newVal = cur == null ? amount : amount + cur.intValue();
        put(item, Integer.valueOf(newVal));
    }

    @Override
    public void addAll(final Iterable<T> items) {
        for (T i : items) {
            add(i, 1);
        }
    }

    @Override
    public boolean substract(final T item) {
        return substract(item, 1);
    }

    @Override
    public boolean substract(final T item, final int amount) {
        Integer cur = get(item);
        if (cur == null) { return false; }
        int newVal = cur.intValue() - amount;
        if (newVal > 0) {
            put(item, Integer.valueOf(newVal));
        } else {
            remove(item);
        }
        return true;
    }

    @Override
    public void substractAll(final Iterable<T> items) {
        for (T i : items) {
            substract(i);
        }
    }

    @Override
    public int countAll() {
        int c = 0;
        for (java.util.Map.Entry<T, Integer> kv : this.entrySet()) {
            c += kv.getValue().intValue();
        }
        return c;
    }

    @Override
    public int count(final T item) {
        Integer cur = get(item);
        return cur == null ? 0 : cur.intValue();
    }
    
}
