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
 *  - a {@link Recorder} keeps the latest snapshot of every object and a new capture reuses (shares) the previous
 *    snapshot of any object that did not change,
 *  - a history of mementos is kept as <b>reverse deltas</b>: the recorder holds the full current state once, and
 *    older mementos retain compact old-field patches / sequence splices when those are smaller than full snapshots;
 *    objects that disappear still retain a full snapshot so they can be brought back. History therefore costs memory
 *    in proportion to what changed, not to the size of the game,
 *  - every byte the history pins is counted ({@link Recorder#liveBytes()}, {@link #retainedBytes()}) so the caller
 *    can keep it under a heap budget,
 *  - scratch buffers are reused but never kept when they grew large, and a failed capture resets the recorder
 *    instead of leaving it half updated.
 *
 * Restoring is last-in-first-out: {@link Recorder#restoreTo} can go back to the newest memento or to any older one that
 * is still in the chain, and refuses (returns false, touches nothing) for anything else.
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
    private static final int[] NO_INTS = new int[0];
    private static final Snap[] NO_SNAPS = new Snap[0];
    private static final HistoryData[] NO_DELTAS = new HistoryData[0];
    private static final Node[] NO_NODES = new Node[0];

    /** Pessimistic heap cost of a registry entry (Node + map slots), of one delta entry, and of a memento shell. */
    static final long NODE_BYTES = 64, ENTRY_BYTES = 16, MEMENTO_BYTES = 96;
    /** Scratch buffers that grew beyond this many slots are dropped after a capture instead of kept (phones). */
    private static final int MAX_KEPT_SCRATCH = 1 << 16;
    private static final int INITIAL_SCRATCH = 1024;

    private static final Map<Class<?>, Kind> KINDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, ClassInfo> INFOS = new ConcurrentHashMap<>();

    // snapshot data

    private abstract static class HistoryData {
        abstract long bytes();
    }

    private abstract static class Snap extends HistoryData {
        /** Estimated heap held by this snapshot (pessimistic: 8 bytes per slot). */
    }

    private static long arr(int n) { return n == 0 ? 0L : 16L + 8L * n; }   // empty arrays are shared constants

    /** Non-final fields of one object: references in refs[], primitives (as raw bits) in prims[]. */
    private static final class ObjSnap extends Snap {
        final ClassInfo ci; final Object[] refs; final long[] prims;
        ObjSnap(ClassInfo ci, Object[] refs, long[] prims) { this.ci = ci; this.refs = refs; this.prims = prims; }
        @Override long bytes() { return 24L + arr(refs.length) + arr(prims.length); }
    }

    /** Contents of an object array / collection / map (k,v,k,v..) / multimap (k,v,..) / table (r,c,v,..). */
    private static final class SeqSnap extends Snap {
        final Object[] items;
        SeqSnap(Object[] items) { this.items = items; }
        @Override long bytes() { return 16L + arr(items.length); }
    }

    private static final class PrimArrSnap extends Snap {
        final Object copy;
        PrimArrSnap(Object copy) { this.copy = copy; }
        @Override long bytes() {
            final int w = copy instanceof long[] || copy instanceof double[] ? 8
                    : copy instanceof int[] || copy instanceof float[] ? 4
                    : copy instanceof short[] || copy instanceof char[] ? 2 : 1;
            return 32L + (long) Array.getLength(copy) * w;
        }
    }

    /** Compact reverse patch; the recorder keeps one complete snapshot for the current state. */
    private abstract static class UndoDelta extends HistoryData {
        abstract Snap restore(Snap current);

        static HistoryData between(final Snap old, final Snap current) {
            if (old instanceof ObjSnap before && current instanceof ObjSnap after && before.ci == after.ci) {
                int nr = 0, np = 0;
                for (int i = 0; i < before.refs.length; i++) {
                    if (before.refs[i] != after.refs[i]) { nr++; }
                }
                for (int i = 0; i < before.prims.length; i++) {
                    if (before.prims[i] != after.prims[i]) { np++; }
                }
                if (ObjDelta.estimatedBytes(nr, np) >= old.bytes()) {
                    return old;
                }
                final int[] refIndexes = nr == 0 ? NO_INTS : new int[nr];
                final Object[] oldRefs = nr == 0 ? NO_OBJECTS : new Object[nr];
                final int[] primIndexes = np == 0 ? NO_INTS : new int[np];
                final long[] oldPrims = np == 0 ? NO_LONGS : new long[np];
                for (int i = 0, r = 0; i < before.refs.length; i++) {
                    if (before.refs[i] != after.refs[i]) { refIndexes[r] = i; oldRefs[r++] = before.refs[i]; }
                }
                for (int i = 0, p = 0; i < before.prims.length; i++) {
                    if (before.prims[i] != after.prims[i]) { primIndexes[p] = i; oldPrims[p++] = before.prims[i]; }
                }
                return new ObjDelta(before.ci, refIndexes, oldRefs, primIndexes, oldPrims);
            }
            if (old instanceof SeqSnap before && current instanceof SeqSnap after) {
                int prefix = 0;
                final int common = Math.min(before.items.length, after.items.length);
                while (prefix < common && before.items[prefix] == after.items[prefix]) prefix++;
                int suffix = 0;
                while (suffix < before.items.length - prefix && suffix < after.items.length - prefix
                        && before.items[before.items.length - 1 - suffix]
                        == after.items[after.items.length - 1 - suffix]) suffix++;
                final int middleLength = before.items.length - prefix - suffix;
                if (32L + arr(middleLength) >= old.bytes()) {
                    return old;
                }
                final Object[] oldMiddle = middleLength == 0 ? NO_OBJECTS
                        : Arrays.copyOfRange(before.items, prefix, prefix + middleLength);
                return new SeqDelta(prefix, suffix, before.items.length, oldMiddle);
            }
            return old;
        }
    }

    private static final class ObjDelta extends UndoDelta {
        final ClassInfo ci;
        final int[] refIndexes;
        final Object[] oldRefs;
        final int[] primIndexes;
        final long[] oldPrims;
        ObjDelta(ClassInfo ci, int[] refIndexes, Object[] oldRefs, int[] primIndexes, long[] oldPrims) {
            this.ci = ci; this.refIndexes = refIndexes; this.oldRefs = oldRefs;
            this.primIndexes = primIndexes; this.oldPrims = oldPrims;
        }
        @Override Snap restore(Snap current) {
            if (!(current instanceof ObjSnap now) || now.ci != ci) {
                throw new IllegalStateException("object snapshot kind changed");
            }
            final Object[] refs = now.refs.length == 0 ? NO_OBJECTS : now.refs.clone();
            final long[] prims = now.prims.length == 0 ? NO_LONGS : now.prims.clone();
            for (int i = 0; i < refIndexes.length; i++) { refs[refIndexes[i]] = oldRefs[i]; }
            for (int i = 0; i < primIndexes.length; i++) { prims[primIndexes[i]] = oldPrims[i]; }
            return new ObjSnap(ci, refs, prims);
        }
        private static long intArrayBytes(int n) { return n == 0 ? 0L : 16L + 4L * n; }
        private static long longArrayBytes(int n) { return n == 0 ? 0L : 16L + 8L * n; }
        static long estimatedBytes(int refs, int prims) {
            return 32L + intArrayBytes(refs) + arr(refs) + intArrayBytes(prims) + longArrayBytes(prims);
        }
        @Override long bytes() { return estimatedBytes(oldRefs.length, oldPrims.length); }
    }

    private static final class SeqDelta extends UndoDelta {
        final int prefix, suffix, oldLength;
        final Object[] oldMiddle;
        SeqDelta(int prefix, int suffix, int oldLength, Object[] oldMiddle) {
            this.prefix = prefix; this.suffix = suffix; this.oldLength = oldLength; this.oldMiddle = oldMiddle;
        }
        @Override Snap restore(Snap current) {
            if (!(current instanceof SeqSnap now)) {
                throw new IllegalStateException("sequence snapshot kind changed");
            }
            final Object[] items = oldLength == 0 ? NO_OBJECTS : new Object[oldLength];
            if (prefix > 0) System.arraycopy(now.items, 0, items, 0, prefix);
            if (oldMiddle.length > 0) System.arraycopy(oldMiddle, 0, items, prefix, oldMiddle.length);
            if (suffix > 0) System.arraycopy(now.items, now.items.length - suffix, items, oldLength - suffix, suffix);
            return new SeqSnap(items);
        }
        @Override long bytes() { return 32L + arr(oldMiddle.length); }
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

    /** One tracked object and its latest snapshot. */
    private static final class Node {
        final Object obj; final boolean isObj;   // isObj: plain game object (fields); otherwise a container / array
        Snap snap; int gen;
        int idx;                                 // position in the recorder's dense objs[] / conts[] array
        Node(Object obj, boolean isObj) { this.obj = obj; this.isObj = isObj; }
    }

    // the memento itself

    // Full form (standalone mementos from capture(Object) / captureStandalone): every object and its snapshot.
    Object[] targets = NO_OBJECTS;   // package-private for tests
    Snap[] snaps = NO_SNAPS;

    // Delta form (mementos from Recorder.capture): the state lives in the recorder; this holds the way back.
    private Recorder owner;
    private GameMemento next;                    // the memento captured right after this one, null for the newest
    private Node[] undoNodes = NO_NODES;         // objects that differ between this state and the next one ...
    private HistoryData[] undoDeltas = NO_DELTAS; // ... old snapshot or compact reverse patch (null = new object)
    private long undoBytes;
    private int size;                            // objects in the graph at capture time

    public final List<String> problems = new ArrayList<>();

    private GameMemento() { }

    /** One-off capture, independent of any recorder (full form). Prefer {@link Recorder#capture} for history. */
    public static GameMemento capture(Object root) {
        return new Recorder().captureStandalone(root);
    }

    /** Number of objects in the graph this memento describes (for logging / tuning). */
    public int objectCount() { return owner != null ? size : targets.length; }

    /** Entries in this memento's reverse delta (0 for the full form and for the newest memento). */
    public int deltaSize() { return undoNodes.length; }

    /** Estimated heap this memento pins on top of what the recorder holds for the current state. */
    public long retainedBytes() {
        if (owner != null) { return MEMENTO_BYTES + undoBytes; }
        return MEMENTO_BYTES + ((long) targets.length + snaps.length) * 8L;
    }

    /** Object count per class, biggest first. Works for full mementos and for the recorder's newest one. */
    public List<Map.Entry<String, Integer>> histogram() {
        final Map<String, Integer> h = new HashMap<>();
        if (owner == null) {
            for (Object t : targets) { h.merge(t.getClass().getName(), 1, Integer::sum); }
        } else if (owner.last == this) {
            for (Node x : owner.registry.values()) { h.merge(x.obj.getClass().getName(), 1, Integer::sum); }
        }
        final List<Map.Entry<String, Integer>> out = new ArrayList<>(h.entrySet());
        out.sort((a, b) -> b.getValue() - a.getValue());
        return out;
    }

    /** How many of this memento's snapshots are the very same instances as in another one (full form, tests). */
    int sharedWith(GameMemento other) {
        final int n = Math.min(snaps.length, other.snaps.length);
        int shared = 0;
        for (int i = 0; i < n; i++) { if (snaps[i] == other.snaps[i]) { shared++; } }
        return shared;
    }

    /**
     * Puts the game back as it was when this memento was captured. Returns false, having touched nothing, when this
     * memento is no longer restorable (it was trimmed, rewound past, or its recorder was reset); true otherwise
     * (check {@link #problems} for fields or containers that could not be written).
     */
    public boolean restore() {
        if (owner != null) {
            if (owner.restoreTo(this)) { return true; }
            problems.add("memento is no longer part of the recorder's history");
            return false;
        }
        // Fields first (so hash codes are right again), then container contents.
        for (int i = 0; i < targets.length; i++) {
            if (snaps[i] instanceof ObjSnap s) { writeFields(targets[i], s); }
        }
        for (int i = 0; i < targets.length; i++) {
            if (!(snaps[i] instanceof ObjSnap)) { restoreContainer(targets[i], snaps[i]); }
        }
        return true;
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

    /** Field-level differences between two full mementos (debugging / tests). Empty = same state. */
    public List<String> diff(GameMemento other) {
        if (owner != null || other.owner != null) {
            return Collections.singletonList("diff needs full mementos (GameMemento.capture)");
        }
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

    /**
     * Owns the "latest snapshot per object" registry and the chain of reverse deltas behind it.
     * Keep one per game (UndoManager does). Game thread only.
     *
     * Invariant: the registry always describes exactly the state of {@link #last}. Capturing a new memento moves the
     * registry forward and stores, in the previous memento, how to move it back; restoring moves it back.
     */
    public static final class Recorder {
        private IdentityHashMap<Object, Node> registry = new IdentityHashMap<>();
        // The same nodes as in the registry, in dense arrays: restoring and sweeping walk these sequentially instead
        // of iterating the hash table. Order is not meaningful (removal swaps the last node into the hole).
        private Node[] objs = new Node[INITIAL_SCRATCH];
        private Node[] conts = new Node[INITIAL_SCRATCH];
        private int no, nc;
        private Node[] work = new Node[INITIAL_SCRATCH];   // breadth-first work list of the capture in progress
        private int wn;
        private Node[] dn = new Node[INITIAL_SCRATCH];     // reverse delta being built
        private HistoryData[] ds = new HistoryData[INITIAL_SCRATCH];
        private int dc;
        private long deltaBytes;
        private Object[] sRefs = new Object[256];
        private long[] sPrims = new long[64];
        private int sn;          // used part of sRefs for sequences
        private int highWater;   // for clearing sRefs after a capture
        private int gen;
        private GameMemento current;   // the memento being built (receives read problems)
        private GameMemento last;      // newest memento of the chain; the registry describes its state
        private boolean recording;     // true while a reverse delta is being collected
        private long liveBytes;        // estimated heap of the registry: nodes plus current snapshots
        /** Conservative, decaying estimate of the latest incremental capture allocation. */
        private long lastCaptureBytes;

        /** Debug only (off by default): list fields holding values the walker treats as opaque. */
        public boolean debugBlindSpots;
        public final Set<String> blindSpots = new TreeSet<>();
        private final Set<Field> blindSeen = Collections.newSetFromMap(new IdentityHashMap<>());

        /** Stats of the last capture, handy for logging how well sharing works. */
        public int lastAllocated, lastReused;
        /** Size of the reverse delta the last capture attached to the memento before it. */
        private int lastDeltaEntries;
        private long lastDeltaBytes;
        int registrySize() { return registry.size(); }   // tests

        /** Estimated heap held by the recorder itself: one node and one snapshot per tracked object. */
        public long liveBytes() { return liveBytes; }

        /** Estimated allocation of a recent incremental capture, excluding the initial full graph. */
        public long lastCaptureBytes() { return lastCaptureBytes; }

        /** Entries in / estimated heap of the delta the last capture produced (predicts the next one). */
        public int lastDeltaEntries() { return lastDeltaEntries; }
        public long lastDeltaBytes() { return lastDeltaBytes; }
        /** True if {@code m} is the newest memento, i.e. the state the recorder currently describes. */
        public boolean isLatest(GameMemento m) { return m != null && m == last && m.owner == this; }

        /** Tests: the dense arrays must hold exactly the registry's nodes, each at the index it records. */
        boolean checkDense() {
            if (no + nc != registry.size()) { return false; }
            for (int i = 0; i < no; i++) {
                final Node n = objs[i];
                if (n == null || !n.isObj || n.idx != i || registry.get(n.obj) != n) { return false; }
            }
            for (int i = 0; i < nc; i++) {
                final Node n = conts[i];
                if (n == null || n.isObj || n.idx != i || registry.get(n.obj) != n) { return false; }
            }
            for (int i = no; i < objs.length; i++) { if (objs[i] != null) { return false; } }
            for (int i = nc; i < conts.length; i++) { if (conts[i] != null) { return false; } }
            return true;
        }

        private void link(final Node n) {
            if (n.isObj) {
                if (no == objs.length) { objs = Arrays.copyOf(objs, no * 2); }
                n.idx = no;
                objs[no++] = n;
            } else {
                if (nc == conts.length) { conts = Arrays.copyOf(conts, nc * 2); }
                n.idx = nc;
                conts[nc++] = n;
            }
        }

        private void unlink(final Node n) {
            final Node[] a = n.isObj ? objs : conts;
            final int last = n.isObj ? --no : --nc;
            final Node l = a[last];
            a[last] = null;
            if (l != n) { a[n.idx] = l; l.idx = n.idx; }
        }

        /** Recomputes {@link #liveBytes()} from scratch (tests: the incremental figure must always equal this). */
        long recomputeLiveBytes() {
            long n = 0;
            for (Node x : registry.values()) { n += NODE_BYTES + x.snap.bytes(); }
            return n;
        }

        /**
         * Forgets the chain but keeps the registry, so the next capture still shares unchanged snapshots. Use when
         * the history was dropped (the next memento starts a new chain and no delta is wasted on the old one).
         */
        public void detach() { last = null; }

        /** Forgets everything and gives the memory back; mementos taken so far can no longer be restored. */
        public void reset() {
            registry = new IdentityHashMap<>();   // clear() would keep the big table
            objs = new Node[INITIAL_SCRATCH];
            conts = new Node[INITIAL_SCRATCH];
            no = 0;
            nc = 0;
            work = new Node[INITIAL_SCRATCH];
            wn = 0;
            dn = new Node[INITIAL_SCRATCH];
            ds = new HistoryData[INITIAL_SCRATCH];
            dc = 0;
            deltaBytes = 0;
            sRefs = new Object[256];
            sPrims = new long[64];
            sn = 0;
            highWater = 0;
            liveBytes = 0;
            lastDeltaEntries = 0;
            lastDeltaBytes = 0;
            last = null;
            current = null;
            recording = false;
        }

        /** Captures the graph below {@code root} as the next memento of this recorder's history. */
        public GameMemento capture(Object root) { return run(root, true); }

        /** Captures a self-contained memento (full form) that does not depend on this recorder afterwards. */
        public GameMemento captureStandalone(Object root) { return run(root, false); }

        private GameMemento run(final Object root, final boolean chained) {
            final boolean hadRegistry = !registry.isEmpty();
            final long scratchBefore = scratchBytes();
            final int g = ++gen;
            final GameMemento m = new GameMemento();
            current = m;
            lastAllocated = 0;
            lastReused = 0;
            lastDeltaEntries = 0;
            lastDeltaBytes = 0;
            final GameMemento prev = chained ? last : null;
            recording = prev != null;
            dc = 0;
            deltaBytes = 0;
            wn = 0;
            long allocatedBytes = 0L;
            long deltaAllocatedBytes = 0L;
            try {
                enqueue(root);
                for (int wi = 0; wi < wn; wi++) {
                    final Node n = work[wi];
                    final Snap old = n.snap;
                    final Snap now = snapshot(n);
                    if (now != old) {
                        n.snap = now;
                        liveBytes += old == null ? NODE_BYTES + now.bytes() : now.bytes() - old.bytes();
                        allocatedBytes += now.bytes() + (old == null ? NODE_BYTES : 0L);
                        if (recording) { deltaAllocatedBytes += note(n, old, now); }
                    }
                }
                // Objects no longer reachable: remember them so a rewind can bring them back. Walking downwards means
                // the node swapped into a hole by unlink() has already been checked.
                for (int i = no - 1; i >= 0; i--) { if (objs[i].gen != g) { forget(objs[i]); } }
                for (int i = nc - 1; i >= 0; i--) { if (conts[i].gen != g) { forget(conts[i]); } }
                if (chained) {
                    final Node[] undoN = dc == 0 ? NO_NODES : Arrays.copyOf(dn, dc);
                    final HistoryData[] undoD = dc == 0 ? NO_DELTAS : Arrays.copyOf(ds, dc);
                    m.owner = this;
                    m.size = wn;
                    if (prev != null) {
                        prev.undoNodes = undoN;
                        prev.undoDeltas = undoD;
                        prev.undoBytes = deltaBytes;
                        prev.next = m;
                    }
                    last = m;
                    lastDeltaEntries = dc;
                    lastDeltaBytes = deltaBytes;
                    final long arrays = dc == 0 ? 0L : 32L + 16L * dc;
                    final long captureBytes = allocatedBytes + deltaAllocatedBytes
                            + Math.max(0L, scratchBytes() - scratchBefore)
                            + MEMENTO_BYTES + arrays;
                    // Retain some history of larger captures, but let the estimate fall after quieter actions.
                    final long decayed = lastCaptureBytes - lastCaptureBytes / 4L;
                    lastCaptureBytes = Math.max(captureBytes, decayed);
                } else {
                    m.targets = new Object[wn];
                    m.snaps = new Snap[wn];
                    for (int i = 0; i < wn; i++) {
                        m.targets[i] = work[i].obj;
                        m.snaps[i] = work[i].snap;
                    }
                }
            } catch (final RuntimeException | Error e) {
                reset();   // never leave a half-updated registry or a dangling chain behind
                throw e;
            } finally {
                releaseScratch();
            }
            return m;
        }

        private void forget(final Node x) {
            liveBytes -= NODE_BYTES + x.snap.bytes();
            if (recording) { note(x, x.snap, null); }
            registry.remove(x.obj);
            unlink(x);
        }

        /** Don't pin game objects through scratch buffers, and don't keep oversized ones. */
        /** Approximate retained capacities of recorder scratch arrays, for the next-capture safety estimate. */
        private long scratchBytes() {
            return arr(work.length) + arr(objs.length) + arr(conts.length) + arr(dn.length)
                    + arr(ds.length) + arr(sRefs.length) + arr(sPrims.length);
        }

        private void releaseScratch() {
            if (work.length > MAX_KEPT_SCRATCH) { work = new Node[INITIAL_SCRATCH]; }
            else { Arrays.fill(work, 0, Math.min(wn, work.length), null); }
            wn = 0;
            if (dn.length > MAX_KEPT_SCRATCH) { dn = new Node[INITIAL_SCRATCH]; ds = new HistoryData[INITIAL_SCRATCH]; }
            else {
                Arrays.fill(dn, 0, Math.min(dc, dn.length), null);
                Arrays.fill(ds, 0, Math.min(dc, ds.length), null);
            }
            dc = 0;
            if (sRefs.length > MAX_KEPT_SCRATCH) { sRefs = new Object[256]; }
            else { Arrays.fill(sRefs, 0, Math.min(highWater, sRefs.length), null); }
            highWater = 0;
            current = null;
            recording = false;
        }

        private long note(final Node n, final Snap old, final Snap now) {
            if (dc == dn.length) {
                dn = Arrays.copyOf(dn, dc * 2);
                ds = Arrays.copyOf(ds, dc * 2);
            }
            // Keep a full snapshot directly when a compact patch is not worthwhile. This avoids
            // allocating a wrapper object around every fallback snapshot.
            final HistoryData delta = old == null ? null
                    : now == null ? old : UndoDelta.between(old, now);
            dn[dc] = n;
            ds[dc] = delta;
            dc++;
            deltaBytes += ENTRY_BYTES + (delta == null ? 0L : delta.bytes());
            return delta instanceof UndoDelta ? delta.bytes() : 0L;
        }

        // restoring

        /**
         * Puts the live objects back to the state of {@code m} and makes {@code m} the newest memento (everything
         * captured after it is dropped). Returns false, touching nothing, if {@code m} is not the newest memento or
         * an older one still linked to it.
         */
        public boolean restoreTo(final GameMemento m) {
            if (m == null || m.owner != this || last == null) { return false; }
            int steps = 0;
            for (GameMemento x = m; x != last; x = x.next) {
                if (x == null) { return false; }
                steps++;
            }
            if (steps > 0) {
                final GameMemento[] chain = new GameMemento[steps];
                GameMemento x = m;
                for (int i = 0; i < steps; i++) { chain[i] = x; x = x.next; }
                try {
                    for (int i = steps - 1; i >= 0; i--) { rewind(chain[i]); }   // newest step first
                } catch (final RuntimeException | OutOfMemoryError e) {
                    reset();
                    m.problems.add("rewind failed: " + e);
                    return false;
                }
                for (final GameMemento c : chain) {
                    c.next = null;
                    c.undoNodes = NO_NODES;
                    c.undoDeltas = NO_DELTAS;
                    c.undoBytes = 0;
                }
            }
            last = m;
            // Fields first (so hash codes are right again), then container contents.
            for (int i = 0; i < no; i++) {
                final Node nd = objs[i];
                if (nd.snap instanceof ObjSnap s) { m.writeFields(nd.obj, s); }
            }
            for (int i = 0; i < nc; i++) {
                final Node nd = conts[i];
                m.restoreContainer(nd.obj, nd.snap);
            }
            return true;
        }

        /** Moves the registry from the state after {@code c} back to the state of {@code c}. */
        private void rewind(final GameMemento c) {
            final Node[] ns = c.undoNodes;
            final HistoryData[] deltas = c.undoDeltas;
            for (int i = 0; i < ns.length; i++) {
                final Node nd = ns[i];
                final HistoryData delta = deltas[i];
                final boolean present = registry.get(nd.obj) == nd;
                if (delta == null) {               // appeared after c: forget it
                    if (present) {
                        registry.remove(nd.obj);
                        unlink(nd);
                        liveBytes -= NODE_BYTES + nd.snap.bytes();
                    }
                } else if (present) {              // changed after c: restore a patch or use the old full snapshot
                    final Snap old = delta instanceof UndoDelta patch ? patch.restore(nd.snap) : (Snap) delta;
                    liveBytes += old.bytes() - nd.snap.bytes();
                    nd.snap = old;
                } else {                           // disappeared after c: bring back its full old snapshot
                    if (!(delta instanceof Snap old)) {
                        throw new IllegalStateException("missing object has no full reverse snapshot");
                    }
                    nd.snap = old;
                    registry.put(nd.obj, nd);
                    link(nd);
                    liveBytes += NODE_BYTES + old.bytes();
                }
            }
        }

        // snapshotting

        private void enqueue(Object v) {
            if (v == null || kindOf(v.getClass()) == Kind.LEAF) { return; }
            Node n = registry.get(v);
            if (n == null) {
                n = new Node(v, kindOf(v.getClass()) == Kind.OBJECT);
                registry.put(v, n);
                link(n);
            } else if (n.gen == gen) {
                return;
            }
            n.gen = gen;
            if (wn == work.length) { work = Arrays.copyOf(work, wn * 2); }
            work[wn++] = n;
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
