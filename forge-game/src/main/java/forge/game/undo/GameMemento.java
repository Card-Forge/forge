package forge.game.undo;

import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Reflective, in-place snapshot of the live game object graph. Game thread only. */
public final class GameMemento {
    private static final String[] WALK = { "forge.game.", "forge.trackable.", "forge.card.CardType" };
    private static final String[] SKIP = { "forge.game.undo.", "forge.game.event.", "forge.game.Match",
            "forge.game.GameRules", "forge.game.GameLog", "forge.game.player.PlayerController",
            "forge.game.player.RegisteredPlayer" };
    private static final Map<Class<?>, Field[]> FIELDS = new ConcurrentHashMap<>();

    private final IdentityHashMap<Object, Object[]> objects = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Object> containers = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
    private final ArrayDeque<Object> pending = new ArrayDeque<>();
    public final List<String> problems = new ArrayList<>();
    public final Set<String> blindSpots = new TreeSet<>();   // debug: opaque non-trivial values

    public static GameMemento capture(Object root) {
        GameMemento m = new GameMemento();
        m.enqueue(root);
        Object o;
        while ((o = m.pending.poll()) != null) m.visit(o);
        return m;
    }

    private static Field[] fieldsOf(Class<?> c) {
        return FIELDS.computeIfAbsent(c, k -> {
            List<Field> out = new ArrayList<>();
            for (Class<?> t = k; t != null && t.getName().startsWith("forge."); t = t.getSuperclass()) {
                for (Field f : t.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    out.add(f);
                }
            }
            return out.toArray(new Field[0]);
        });
    }

    private static boolean isContainer(Object v) {
        return v instanceof Collection || v instanceof Map || v instanceof Multimap || v instanceof Table;
    }

    private static boolean walkable(Object v) {
        Class<?> c = v.getClass();
        if (v instanceof Enum || v instanceof CharSequence || v instanceof Number
                || v instanceof Boolean || v instanceof Character || v instanceof Class) return false;
        if (c.isArray()) return true;
        String n = c.getName();
        if (isContainer(v)) {
            if (n.contains("Immutable") || n.contains("Unmodifiable")) return false;
            // JDK/Guava view classes (keySet(), subList(), ...) would write through to their backing store
            return !((n.startsWith("java.util.") || n.startsWith("com.google.common.")) && n.contains("$")
                    && !n.contains("Synchronized"));
        }
        if (c.isAnonymousClass() || c.isSynthetic() || n.contains("$$")) return false;   // closures
        for (String s : SKIP) if (n.startsWith(s)) return false;
        for (String p : WALK) if (n.startsWith(p)) return true;
        return false;
    }

    private void enqueue(Object v) {
        if (v == null || seen.put(v, Boolean.TRUE) != null) return;
        if (walkable(v)) pending.add(v);
    }

    @SuppressWarnings("unchecked")
    private void visit(Object o) {
        if (o.getClass().isArray()) {
            int n = Array.getLength(o);
            Object copy = Array.newInstance(o.getClass().getComponentType(), n);
            System.arraycopy(o, 0, copy, 0, n);
            if (!o.getClass().getComponentType().isPrimitive()) for (Object x : (Object[]) copy) enqueue(x);
            containers.put(o, copy);
        } else if (o instanceof Map) {
            List<Object[]> es = new ArrayList<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                es.add(new Object[] { e.getKey(), e.getValue() });
                enqueue(e.getKey()); enqueue(e.getValue());
            }
            containers.put(o, es);
        } else if (o instanceof Multimap) {
            List<Object[]> es = new ArrayList<>();
            for (Map.Entry<?, ?> e : ((Multimap<?, ?>) o).entries()) {
                es.add(new Object[] { e.getKey(), e.getValue() });
                enqueue(e.getKey()); enqueue(e.getValue());
            }
            containers.put(o, es);
        } else if (o instanceof Table) {
            List<Object[]> es = new ArrayList<>();
            for (Table.Cell<?, ?, ?> c : ((Table<?, ?, ?>) o).cellSet()) {
                es.add(new Object[] { c.getRowKey(), c.getColumnKey(), c.getValue() });
                enqueue(c.getRowKey()); enqueue(c.getColumnKey()); enqueue(c.getValue());
            }
            containers.put(o, es);
        } else if (o instanceof Collection) {
            Object[] arr = ((Collection<?>) o).toArray();
            for (Object x : arr) enqueue(x);
            containers.put(o, arr);
        } else {
            Field[] fs = fieldsOf(o.getClass());
            Object[] vals = new Object[fs.length];
            for (int i = 0; i < fs.length; i++) {
                try { vals[i] = fs[i].get(o); } catch (IllegalAccessException e) { problems.add("read " + fs[i]); }
                Object v = vals[i];
                if (v != null && !walkable(v) && !(v instanceof Enum) && !(v instanceof Number)
                        && !(v instanceof CharSequence) && !(v instanceof Boolean) && !(v instanceof Character)) {
                    blindSpots.add(o.getClass().getSimpleName() + "." + fs[i].getName() + " : " + v.getClass().getName());
                }
                enqueue(v);
            }
            objects.put(o, vals);
        }
    }

    /** Fields first (so hashCodes are right again), then container contents. */
    @SuppressWarnings("unchecked")
    public void restore() {
        for (Map.Entry<Object, Object[]> e : objects.entrySet()) {
            Field[] fs = fieldsOf(e.getKey().getClass());
            Object[] vals = e.getValue();
            for (int i = 0; i < fs.length; i++) {
                if (Modifier.isFinal(fs[i].getModifiers())) continue;
                try { fs[i].set(e.getKey(), vals[i]); } catch (Exception ex) { problems.add("write " + fs[i]); }
            }
        }
        for (Map.Entry<Object, Object> e : containers.entrySet()) {
            Object c = e.getKey(), saved = e.getValue();
            try {
                if (c.getClass().isArray()) {
                    System.arraycopy(saved, 0, c, 0, Array.getLength(saved));
                } else if (c instanceof Map) {
                    Map<Object, Object> m = (Map<Object, Object>) c;
                    List<Object[]> es = (List<Object[]>) saved;
                    if (sameEntries(m, es)) continue;
                    m.clear();
                    for (Object[] x : es) m.put(x[0], x[1]);
                } else if (c instanceof Multimap) {
                    Multimap<Object, Object> m = (Multimap<Object, Object>) c;
                    m.clear();
                    for (Object[] x : (List<Object[]>) saved) m.put(x[0], x[1]);
                } else if (c instanceof Table) {
                    Table<Object, Object, Object> t = (Table<Object, Object, Object>) c;
                    t.clear();
                    for (Object[] x : (List<Object[]>) saved) t.put(x[0], x[1], x[2]);
                } else {
                    Collection<Object> col = (Collection<Object>) c;
                    Object[] arr = (Object[]) saved;
                    if (sameElements(col, arr)) continue;
                    col.clear();
                    Collections.addAll(col, arr);
                }
            } catch (RuntimeException ex) {
                problems.add("container " + c.getClass().getName() + ": " + ex);
            }
        }
    }

    private static boolean sameElements(Collection<?> col, Object[] arr) {
        if (col.size() != arr.length) return false;
        int i = 0;
        for (Object x : col) if (x != arr[i++]) return false;
        return true;
    }
    private static boolean sameEntries(Map<?, ?> m, List<Object[]> es) {
        if (m.size() != es.size()) return false;
        int i = 0;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            Object[] x = es.get(i++);
            if (e.getKey() != x[0] || e.getValue() != x[1]) return false;
        }
        return true;
    }

    /** Test oracle: field-level differences between two captures. */
    public List<String> diff(GameMemento other) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<Object, Object[]> e : objects.entrySet()) {
            Object[] theirs = other.objects.get(e.getKey());
            Field[] fs = fieldsOf(e.getKey().getClass());
            if (theirs == null) { out.add("unreachable: " + e.getKey().getClass().getSimpleName()); continue; }
            for (int i = 0; i < fs.length; i++) {
                Object a = e.getValue()[i], b = theirs[i];
                boolean same = a == b || (a != null && (a instanceof Number || a instanceof Boolean
                        || a instanceof Character || a instanceof CharSequence) && a.equals(b));
                if (!same) out.add(e.getKey().getClass().getSimpleName() + "." + fs[i].getName());
            }
        }
        for (Map.Entry<Object, Object> e : containers.entrySet()) {
            Object theirs = other.containers.get(e.getKey());
            if (theirs == null || !Objects.equals(String.valueOf(flat(e.getValue())), String.valueOf(flat(theirs))))
                out.add("container " + e.getKey().getClass().getSimpleName());
        }
        return out;
    }
    private static List<Object> flat(Object saved) {
        List<Object> r = new ArrayList<>();
        if (saved instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof Object[])
            for (Object x : l) r.addAll(Arrays.asList((Object[]) x));
        else if (saved instanceof Object[] a) r.addAll(Arrays.asList(a));
        else if (saved.getClass().isArray()) for (int i = 0; i < Array.getLength(saved); i++) r.add(Array.get(saved, i));
        else r.add(saved);
        return r;
    }
}