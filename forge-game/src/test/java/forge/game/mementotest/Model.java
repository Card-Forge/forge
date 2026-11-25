package forge.game.mementotest;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A small object graph for the memento tests. It lives in forge.game.mementotest (not forge.game.undo, which the
 * walker skips) so the memento treats these classes like game classes. It covers every kind of field the memento
 * handles: primitives of several types, final and non-final references, arrays, lists, maps, sets, multimaps, tables.
 */
public final class Model {
    private Model() { }

    public static final class Item {
        public final int id;                       // final: walked, never restored
        public int value;
        public long big;
        public boolean flag;
        public double ratio;
        public char letter;
        public String name;
        public Item partner;
        public int[] slots = new int[3];
        public final List<Item> kids = new ArrayList<>();

        public Item(int id) { this.id = id; }
    }

    /** An item whose hash code depends on a mutable field, to prove fields are restored before containers. */
    public static final class KeyedItem {
        public int key;
        public KeyedItem(int key) { this.key = key; }
        @Override public int hashCode() { return key * 31 + 7; }
        @Override public boolean equals(Object o) { return o instanceof KeyedItem k && k.key == key; }
    }

    /** A list that can be told to fail while it is being read, to simulate a capture that dies half way. */
    public static final class ExplodingList extends ArrayList<Item> {
        public boolean armed;
        @Override public java.util.Iterator<Item> iterator() {
            if (armed) { throw new IllegalStateException("boom"); }
            return super.iterator();
        }
    }

    public static final class Root {
        public final List<Item> items = new ArrayList<>();
        public final Map<String, Item> byName = new HashMap<>();
        public final Set<Item> marked = new HashSet<>();
        public final Multimap<String, Item> multi = ArrayListMultimap.create();
        public final Table<String, String, Item> grid = HashBasedTable.create();
        public final Map<KeyedItem, String> keyed = new HashMap<>();
        public final ExplodingList fragile = new ExplodingList();
        public Item current;
        public int counter;
        public Item[] ring = new Item[4];
    }

    // deterministic dump of everything reachable from a root, insensitive to hash order

    private static String ref(Item i) { return i == null ? "null" : "#" + i.id; }

    public static String fingerprint(Root r) {
        final java.util.TreeMap<Integer, Item> all = new java.util.TreeMap<>();
        final java.util.ArrayDeque<Item> todo = new java.util.ArrayDeque<>();
        final Collection<Item> seeds = new ArrayList<>();
        seeds.addAll(r.items);
        seeds.addAll(r.byName.values());
        seeds.addAll(r.marked);
        seeds.addAll(r.multi.values());
        seeds.addAll(r.grid.values());
        seeds.addAll(r.fragile);
        seeds.add(r.current);
        for (Item i : r.ring) { seeds.add(i); }
        for (Item i : seeds) { if (i != null && all.put(i.id, i) == null) { todo.add(i); } }
        while (!todo.isEmpty()) {
            final Item i = todo.poll();
            final List<Item> next = new ArrayList<>(i.kids);
            next.add(i.partner);
            for (Item n : next) { if (n != null && all.put(n.id, n) == null) { todo.add(n); } }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("counter=").append(r.counter).append(" current=").append(ref(r.current)).append('\n');
        sb.append("items=");
        for (Item i : r.items) { sb.append(ref(i)).append(','); }
        sb.append("\nring=");
        for (Item i : r.ring) { sb.append(ref(i)).append(','); }
        sb.append("\nfragile=");
        for (Item i : r.fragile) { sb.append(ref(i)).append(','); }
        sb.append('\n');
        final List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Item> e : r.byName.entrySet()) { lines.add("name " + e.getKey() + "->" + ref(e.getValue())); }
        for (Item i : r.marked) { lines.add("marked " + ref(i)); }
        for (Map.Entry<String, Item> e : r.multi.entries()) { lines.add("multi " + e.getKey() + "->" + ref(e.getValue())); }
        for (Table.Cell<String, String, Item> c : r.grid.cellSet()) {
            lines.add("grid " + c.getRowKey() + "," + c.getColumnKey() + "->" + ref(c.getValue()));
        }
        for (Map.Entry<KeyedItem, String> e : r.keyed.entrySet()) { lines.add("keyed " + e.getKey().key + "->" + e.getValue()); }
        java.util.Collections.sort(lines);
        for (String l : lines) { sb.append(l).append('\n'); }
        for (Item i : all.values()) {
            sb.append(ref(i)).append(" v=").append(i.value).append(" big=").append(i.big).append(" flag=").append(i.flag)
                    .append(" ratio=").append(i.ratio).append(" letter=").append((int) i.letter).append(" name=").append(i.name)
                    .append(" partner=").append(ref(i.partner)).append(" slots=")
                    .append(java.util.Arrays.toString(i.slots)).append(" kids=");
            for (Item k : i.kids) { sb.append(ref(k)).append(','); }
            sb.append('\n');
        }
        return sb.toString();
    }
}
