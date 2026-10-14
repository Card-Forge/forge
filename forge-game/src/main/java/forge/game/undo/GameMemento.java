package forge.game.undo;

import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflective, in-place snapshot of the live game object graph. Game thread only.
 *
 *  - primitive fields are read with typed getters into long[] (no boxing),
 *  - final fields are walked but not stored (they are never restored anyway),
 *  - a {@link Recorder} keeps the latest snapshot of every object, and a new capture reuses (shares) the previous
 *    snapshot of any object that did not change, so consecutive mementos only pay for what changed,
 *  - scratch buffers are reused, and nothing is allocated for debugging unless asked to.
 */
public final class GameMemento {
    private static final String[] WALK = { "forge.game.", "forge.trackable.", "forge.card.CardType" };
    private static final String[] SKIP = { "forge.game.undo.", "forge.game.event.", "forge.game.Match",
            "forge.game.GameRules", "forge.game.GameLog", "forge.game.player.PlayerController",
            "forge.game.player.RegisteredPlayer" };

    private enum Kind { LEAF, OBJECT, ARRAY, COLLECTION, MAP, MULTIMAP, TABLE }

    private static final int P_REF = 0, P_INT = 1, P_LONG = 2, P_BOOL = 3, P_DOUBLE = 4, P_FLOAT = 5,
            P_BYTE = 6, P_SHORT = 7, P_CHAR = 8;

    private static final Object[] NO_OBJECTS = new Object[0];
    private static final long[] NO_LONGS = new long[0];
    private static final Snap[] NO_SNAPS = new Snap[0];

    private static final Map<Class<?>, Kind> KINDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, ClassInfo> INFOS = new ConcurrentHashMap<>();

    // snapshot data

    private abstract static class Snap { }

    /** Non-final fields of one object: references in refs[], primitives (as raw bits) in prims[]. */
    private static final class ObjSnap extends Snap {
        final ClassInfo ci; final Object[] refs; final long[] prims;
        ObjSnap(ClassInfo ci, Object[] refs, long[] prims) { this.ci = ci; this.refs = refs; this.prims = prims; }
    }

    /** Contents of an object array / collection / map (k,v,k,v..) / multimap (k,v,..) / table (r,c,v,..). */
    private static final class SeqSnap extends Snap {
        final Object[] items;
        SeqSnap(Object[] items) { this.items = items; }
    }

    private static final class PrimArrSnap extends Snap {
        final Object copy;
        PrimArrSnap(Object copy) { this.copy = copy; }
    }

    private static final class ClassInfo {
        final Field[] fields;      // every non-static field of the forge.* part of the hierarchy
        final int[] pk;            // P_REF or a primitive kind, per field
        final boolean[] stored;    // false for final fields: walked, but not stored or restored
        final int nRefs, nPrims;   // number of stored reference / primitive fields

        ClassInfo(Class<?> c) {
            final List<Field> out = new ArrayList<>();
            for (Class<?> t = c; t != null && t.getName().startsWith("forge."); t = t.getSuperclass()) {
                for (Field f : t.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) { continue; }
                    f.setAccessible(true);
                    out.add(f);
                }
            }
            fields = out.toArray(new Field[0]);
            pk = new int[fields.length];
            stored = new boolean[fields.length];
            int r = 0, p = 0;
            for (int i = 0; i < fields.length; i++) {
                pk[i] = primKind(fields[i].getType());
                stored[i] = !Modifier.isFinal(fields[i].getModifiers());
                if (stored[i]) { if (pk[i] == P_REF) { r++; } else { p++; } }
            }
            nRefs = r;
            nPrims = p;
        }
    }

    // the memento itself

    Object[] targets = NO_OBJECTS;   // package-private for tests
    Snap[] snaps = NO_SNAPS;
    public final List<String> problems = new ArrayList<>();

    private GameMemento() { }

    /** One-off capture with no sharing. Prefer {@link Recorder#capture} when capturing repeatedly. */
    public static GameMemento capture(Object root) {
        return new Recorder().capture(root);
    }

    /** Number of objects held by this memento (for logging / tuning). */
    public int objectCount() { return targets.length; }

    /** How many of this memento's snapshots are the very same instances as in another one (tests / tuning). */
    int sharedWith(GameMemento other) {
        final int n = Math.min(snaps.length, other.snaps.length);
        int shared = 0;
        for (int i = 0; i < n; i++) { if (snaps[i] == other.snaps[i]) { shared++; } }
        return shared;
    }

    /** Fields first (so hash codes are right again), then container contents. */
    public void restore() {
        for (int i = 0; i < targets.length; i++) {
            if (snaps[i] instanceof ObjSnap s) { writeFields(targets[i], s); }
        }
        for (int i = 0; i < targets.length; i++) {
            if (!(snaps[i] instanceof ObjSnap)) { restoreContainer(targets[i], snaps[i]); }
        }
    }

    private void writeFields(Object o, ObjSnap s) {
        final ClassInfo ci = s.ci;
        final Field[] fs = ci.fields;
        int ri = 0, pi = 0;
        for (int i = 0; i < fs.length; i++) {
            if (!ci.stored[i]) { continue; }
            try {
                if (ci.pk[i] == P_REF) { fs[i].set(o, s.refs[ri++]); }
                else { writePrim(fs[i], ci.pk[i], o, s.prims[pi++]); }
            } catch (Exception ex) {
                problems.add("write " + fs[i]);   // indexes were already advanced by the argument expressions
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreContainer(Object c, Snap snap) {
        try {
            switch (kindOf(c.getClass())) {
                case ARRAY:
                    if (snap instanceof PrimArrSnap p) {
                        System.arraycopy(p.copy, 0, c, 0, Array.getLength(p.copy));
                    } else {
                        final Object[] items = ((SeqSnap) snap).items;
                        System.arraycopy(items, 0, c, 0, items.length);
                    }
                    break;
                case MAP: {
                    final Map<Object, Object> m = (Map<Object, Object>) c;
                    final Object[] kv = ((SeqSnap) snap).items;
                    if (sameMap(m, kv)) { break; }
                    m.clear();
                    for (int i = 0; i < kv.length; i += 2) { m.put(kv[i], kv[i + 1]); }
                    break;
                }
                case MULTIMAP: {
                    final Multimap<Object, Object> m = (Multimap<Object, Object>) c;
                    final Object[] kv = ((SeqSnap) snap).items;
                    m.clear();
                    for (int i = 0; i < kv.length; i += 2) { m.put(kv[i], kv[i + 1]); }
                    break;
                }
                case TABLE: {
                    final Table<Object, Object, Object> t = (Table<Object, Object, Object>) c;
                    final Object[] rcv = ((SeqSnap) snap).items;
                    t.clear();
                    for (int i = 0; i < rcv.length; i += 3) { t.put(rcv[i], rcv[i + 1], rcv[i + 2]); }
                    break;
                }
                case COLLECTION: {
                    final Collection<Object> col = (Collection<Object>) c;
                    final Object[] items = ((SeqSnap) snap).items;
                    if (sameElements(col, items)) { break; }
                    col.clear();
                    Collections.addAll(col, items);
                    break;
                }
                default:
                    break;
            }
        } catch (RuntimeException ex) {
            problems.add("container " + c.getClass().getName() + ": " + ex);
        }
    }

    private static boolean sameElements(Collection<?> col, Object[] arr) {
        if (col.size() != arr.length) { return false; }
        int i = 0;
        for (Object x : col) { if (x != arr[i++]) { return false; } }
        return true;
    }

    private static boolean sameMap(Map<?, ?> m, Object[] kv) {
        if (m.size() * 2 != kv.length) { return false; }
        int i = 0;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (e.getKey() != kv[i] || e.getValue() != kv[i + 1]) { return false; }
            i += 2;
        }
        return true;
    }

    /** Content equality of two primitive arrays of the same type (no allocation). */
    private static boolean samePrimArray(Object a, Object b) {
        if (a == b) { return true; }
        if (a instanceof int[] x && b instanceof int[] y) { return Arrays.equals(x, y); }
        if (a instanceof long[] x && b instanceof long[] y) { return Arrays.equals(x, y); }
        if (a instanceof boolean[] x && b instanceof boolean[] y) { return Arrays.equals(x, y); }
        if (a instanceof double[] x && b instanceof double[] y) { return Arrays.equals(x, y); }
        if (a instanceof float[] x && b instanceof float[] y) { return Arrays.equals(x, y); }
        if (a instanceof byte[] x && b instanceof byte[] y) { return Arrays.equals(x, y); }
        if (a instanceof short[] x && b instanceof short[] y) { return Arrays.equals(x, y); }
        if (a instanceof char[] x && b instanceof char[] y) { return Arrays.equals(x, y); }
        return false;
    }

    private static boolean sameItems(Object[] stored, Object[] scratch, int n) {
        if (stored.length != n) { return false; }
        for (int i = 0; i < n; i++) { if (stored[i] != scratch[i]) { return false; } }
        return true;
    }

    // test oracle

    /** Field-level differences between two mementos (debugging / tests). Empty = same state. */
    public List<String> diff(GameMemento other) {
        final IdentityHashMap<Object, Snap> theirs = new IdentityHashMap<>(other.targets.length * 2 + 1);
        for (int i = 0; i < other.targets.length; i++) { theirs.put(other.targets[i], other.snaps[i]); }
        final List<String> out = new ArrayList<>();
        for (int i = 0; i < targets.length; i++) {
            final Snap a = snaps[i], b = theirs.get(targets[i]);
            final String n = targets[i].getClass().getSimpleName();
            if (b == null) { out.add("unreachable: " + n); continue; }
            if (a == b) { continue; }
            if (a instanceof ObjSnap x && b instanceof ObjSnap y && x.ci == y.ci) {
                int ri = 0, pi = 0;
                for (int f = 0; f < x.ci.fields.length; f++) {
                    if (!x.ci.stored[f]) { continue; }
                    final boolean same = x.ci.pk[f] == P_REF ? x.refs[ri++] == y.refs[ri - 1]
                            : x.prims[pi++] == y.prims[pi - 1];
                    if (!same) { out.add(n + "." + x.ci.fields[f].getName()); }
                }
            } else if (a instanceof SeqSnap x && b instanceof SeqSnap y) {
                if (!sameItems(x.items, y.items, y.items.length)) { out.add("container " + n); }
            } else if (a instanceof PrimArrSnap x && b instanceof PrimArrSnap y) {
                if (!samePrimArray(x.copy, y.copy)) { out.add("primitive array " + n); }
            } else {
                out.add("changed kind: " + n);
            }
        }
        return out;
    }

    // classification (cached per class)

    private static Kind kindOf(Class<?> c) {
        Kind k = KINDS.get(c);
        if (k == null) { k = computeKind(c); KINDS.put(c, k); }
        return k;
    }

    private static Kind computeKind(Class<?> c) {
        if (Enum.class.isAssignableFrom(c) || CharSequence.class.isAssignableFrom(c)
                || Number.class.isAssignableFrom(c) || c == Boolean.class || c == Character.class || c == Class.class) {
            return Kind.LEAF;
        }
        if (c.isArray()) { return Kind.ARRAY; }
        final String n = c.getName();
        final boolean isMap = Map.class.isAssignableFrom(c), isMulti = Multimap.class.isAssignableFrom(c),
                isTable = Table.class.isAssignableFrom(c), isCol = Collection.class.isAssignableFrom(c);
        if (isMap || isMulti || isTable || isCol) {
            if (n.contains("Immutable") || n.contains("Unmodifiable")) { return Kind.LEAF; }
            // JDK/Guava view classes (keySet(), subList(), ...) would write through to their backing store
            if ((n.startsWith("java.util.") || n.startsWith("com.google.common.")) && n.contains("$")
                    && !n.contains("Synchronized")) {
                return Kind.LEAF;
            }
            if (isMap) { return Kind.MAP; }
            if (isMulti) { return Kind.MULTIMAP; }
            if (isTable) { return Kind.TABLE; }
            return Kind.COLLECTION;
        }
        if (c.isAnonymousClass() || c.isSynthetic() || n.contains("$$")) { return Kind.LEAF; } // closures
        for (String s : SKIP) { if (n.startsWith(s)) { return Kind.LEAF; } }
        for (String p : WALK) { if (n.startsWith(p)) { return Kind.OBJECT; } }
        return Kind.LEAF;
    }

    private static ClassInfo infoOf(Class<?> c) {
        ClassInfo ci = INFOS.get(c);
        if (ci == null) { ci = new ClassInfo(c); INFOS.put(c, ci); }
        return ci;
    }

    private static int primKind(Class<?> t) {
        if (!t.isPrimitive()) { return P_REF; }
        if (t == int.class) { return P_INT; }
        if (t == long.class) { return P_LONG; }
        if (t == boolean.class) { return P_BOOL; }
        if (t == double.class) { return P_DOUBLE; }
        if (t == float.class) { return P_FLOAT; }
        if (t == byte.class) { return P_BYTE; }
        if (t == short.class) { return P_SHORT; }
        return P_CHAR;
    }

    private static long readPrim(Field f, int kind, Object o) throws IllegalAccessException {
        switch (kind) {
            case P_INT: return f.getInt(o);
            case P_LONG: return f.getLong(o);
            case P_BOOL: return f.getBoolean(o) ? 1L : 0L;
            case P_DOUBLE: return Double.doubleToRawLongBits(f.getDouble(o));
            case P_FLOAT: return Float.floatToRawIntBits(f.getFloat(o));
            case P_BYTE: return f.getByte(o);
            case P_SHORT: return f.getShort(o);
            default: return f.getChar(o);
        }
    }

    private static void writePrim(Field f, int kind, Object o, long v) throws IllegalAccessException {
        switch (kind) {
            case P_INT: f.setInt(o, (int) v); break;
            case P_LONG: f.setLong(o, v); break;
            case P_BOOL: f.setBoolean(o, v != 0L); break;
            case P_DOUBLE: f.setDouble(o, Double.longBitsToDouble(v)); break;
            case P_FLOAT: f.setFloat(o, Float.intBitsToFloat((int) v)); break;
            case P_BYTE: f.setByte(o, (byte) v); break;
            case P_SHORT: f.setShort(o, (short) v); break;
            default: f.setChar(o, (char) v); break;
        }
    }

    // capturing

    private static final class Node {
        final Object obj; Snap snap; int gen;
        Node(Object obj) { this.obj = obj; }
    }

    /**
     * Owns the "latest snapshot per object" registry that makes consecutive captures cheap.
     * Keep one per game (UndoManager does). Game thread only.
     */
    public static final class Recorder {
        private final IdentityHashMap<Object, Node> registry = new IdentityHashMap<>();
        private final ArrayDeque<Node> queue = new ArrayDeque<>();
        private final ArrayList<Node> order = new ArrayList<>();
        private Object[] sRefs = new Object[256];
        private long[] sPrims = new long[64];
        private int sn;          // used part of sRefs for sequences
        private int highWater;   // for clearing sRefs after a capture
        private int gen;
        private GameMemento current;

        /** Debug only (off by default): list fields holding values the walker treats as opaque. */
        public boolean debugBlindSpots;
        public final Set<String> blindSpots = new TreeSet<>();
        private final Set<Field> blindSeen = Collections.newSetFromMap(new IdentityHashMap<>());

        /** Stats of the last capture, handy for logging how well sharing works. */
        public int lastAllocated, lastReused;

        int registrySize() { return registry.size(); }   // tests

        public GameMemento capture(Object root) {
            final int g = ++gen;
            final GameMemento m = new GameMemento();
            current = m;
            lastAllocated = 0;
            lastReused = 0;
            order.clear();
            queue.clear();
            try {
                enqueue(root);
                Node n;
                while ((n = queue.poll()) != null) {
                    order.add(n);
                    n.snap = snapshot(n);
                }
                registry.values().removeIf(x -> x.gen != g);   // objects no longer reachable
                final int size = order.size();
                m.targets = new Object[size];
                m.snaps = new Snap[size];
                for (int i = 0; i < size; i++) {
                    final Node x = order.get(i);
                    m.targets[i] = x.obj;
                    m.snaps[i] = x.snap;
                }
            } finally {
                order.clear();
                queue.clear();
                Arrays.fill(sRefs, 0, Math.min(highWater, sRefs.length), null);   // don't pin objects via scratch
                highWater = 0;
                current = null;
            }
            return m;
        }

        private void enqueue(Object v) {
            if (v == null || kindOf(v.getClass()) == Kind.LEAF) { return; }
            Node n = registry.get(v);
            if (n == null) {
                n = new Node(v);
                registry.put(v, n);
            } else if (n.gen == gen) {
                return;
            }
            n.gen = gen;
            queue.add(n);
        }

        private Snap snapshot(Node n) {
            final Object o = n.obj;
            final Kind k = kindOf(o.getClass());
            if (k == Kind.OBJECT) { return snapObject(n); }
            if (k == Kind.ARRAY && o.getClass().getComponentType().isPrimitive()) { return snapPrimArray(n); }
            return snapSequence(n, k);
        }

        private void putSeq(Object x) {
            if (sn == sRefs.length) { sRefs = Arrays.copyOf(sRefs, sn * 2); }
            sRefs[sn++] = x;
            enqueue(x);
        }

        @SuppressWarnings("unchecked")
        private Snap snapSequence(Node n, Kind k) {
            final Object o = n.obj;
            sn = 0;
            switch (k) {
                case ARRAY:
                    for (Object x : (Object[]) o) { putSeq(x); }
                    break;
                case MAP:
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) { putSeq(e.getKey()); putSeq(e.getValue()); }
                    break;
                case MULTIMAP:
                    for (Map.Entry<?, ?> e : ((Multimap<?, ?>) o).entries()) { putSeq(e.getKey()); putSeq(e.getValue()); }
                    break;
                case TABLE:
                    for (Table.Cell<?, ?, ?> c : ((Table<?, ?, ?>) o).cellSet()) {
                        putSeq(c.getRowKey()); putSeq(c.getColumnKey()); putSeq(c.getValue());
                    }
                    break;
                default: // COLLECTION
                    for (Object x : (Collection<?>) o) { putSeq(x); }
                    break;
            }
            highWater = Math.max(highWater, sn);
            if (n.snap instanceof SeqSnap prev && sameItems(prev.items, sRefs, sn)) {
                lastReused++;
                return prev;
            }
            lastAllocated++;
            return new SeqSnap(sn == 0 ? NO_OBJECTS : Arrays.copyOf(sRefs, sn));
        }

        private Snap snapPrimArray(Node n) {
            final Object arr = n.obj;
            if (n.snap instanceof PrimArrSnap prev && samePrimArray(arr, prev.copy)) {
                lastReused++;
                return prev;
            }
            final int len = Array.getLength(arr);
            final Object copy = Array.newInstance(arr.getClass().getComponentType(), len);
            System.arraycopy(arr, 0, copy, 0, len);
            lastAllocated++;
            return new PrimArrSnap(copy);
        }

        private Snap snapObject(Node n) {
            final Object o = n.obj;
            final ClassInfo ci = infoOf(o.getClass());
            if (sRefs.length < ci.nRefs) { sRefs = new Object[Math.max(ci.nRefs, sRefs.length * 2)]; }
            if (sPrims.length < ci.nPrims) { sPrims = new long[Math.max(ci.nPrims, sPrims.length * 2)]; }

            final Field[] fs = ci.fields;
            int ri = 0, pi = 0;
            for (int i = 0; i < fs.length; i++) {
                final boolean stored = ci.stored[i];
                if (ci.pk[i] == P_REF) {
                    Object v = null;
                    try { v = fs[i].get(o); } catch (IllegalAccessException e) { current.problems.add("read " + fs[i]); }
                    if (stored) { sRefs[ri++] = v; }
                    if (v != null) {
                        if (debugBlindSpots && kindOf(v.getClass()) == Kind.LEAF) { noteBlind(fs[i], v); }
                        enqueue(v);
                    }
                } else if (stored) {
                    long v = 0L;
                    try { v = readPrim(fs[i], ci.pk[i], o); } catch (IllegalAccessException e) { current.problems.add("read " + fs[i]); }
                    sPrims[pi++] = v;
                }
            }
            highWater = Math.max(highWater, ri);

            if (n.snap instanceof ObjSnap prev && prev.ci == ci
                    && sameItems(prev.refs, sRefs, ri) && samePrims(prev.prims, sPrims, pi)) {
                lastReused++;
                return prev;
            }
            lastAllocated++;
            return new ObjSnap(ci, ri == 0 ? NO_OBJECTS : Arrays.copyOf(sRefs, ri),
                    pi == 0 ? NO_LONGS : Arrays.copyOf(sPrims, pi));
        }

        private static boolean samePrims(long[] stored, long[] scratch, int n) {
            if (stored.length != n) { return false; }
            for (int i = 0; i < n; i++) { if (stored[i] != scratch[i]) { return false; } }
            return true;
        }

        private void noteBlind(Field f, Object v) {
            if (v instanceof Enum || v instanceof Number || v instanceof CharSequence
                    || v instanceof Boolean || v instanceof Character || v instanceof Class) { return; }
            if (blindSeen.add(f)) { blindSpots.add(f.getDeclaringClass().getSimpleName() + "." + f.getName() + " : " + v.getClass().getName()); }
        }
    }
}