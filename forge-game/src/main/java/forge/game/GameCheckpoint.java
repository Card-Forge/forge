package forge.game;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.google.common.base.Suppliers;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import forge.card.CardType;
import forge.game.event.GameEvent;
import forge.game.player.PlayerController;
import forge.game.player.RegisteredPlayer;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;

/**
 * A point the game can be put back to, for undoing a misplay.
 * <p>
 * Rather than copying the game, this records the state of the objects the game is made of, in
 * place: every field of every game and trackable object reachable from the {@link Game}, and the
 * contents of every collection, map and table they hold. Restoring writes those values back into
 * the same objects. A card that has since changed zones was replaced by a new object; restoring
 * the zone's contents puts the original object back, so anything still pointing at it (triggers,
 * remembered lists, the stack) stays valid without any id mapping. Objects created after the
 * checkpoint simply stop being reachable.
 * <p>
 * Walking by reflection keeps this complete as the engine grows: a field added to a card or a
 * player is covered without anyone having to remember undo. The cost is that a few things must be
 * kept out on purpose - see {@link #OPAQUE_TYPES} and {@link KeptOnRestore}.
 */
public final class GameCheckpoint {

    /** Reachable, but not part of the game state to rewind: controllers (GUI, AI), the match, the log. */
    private static final List<Class<?>> OPAQUE_TYPES = List.of(
            PlayerController.class, Match.class, GameRules.class, RegisteredPlayer.class,
            GameLog.class, GameOutcome.class, GameEvent.class, GameSnapshot.class,
            GameCheckpoint.class, UndoHistory.class, DrawOffer.class);

    /** The engine's own classes are the ones under these packages. */
    private static final String GAME_PACKAGE = Game.class.getPackageName() + ".";
    private static final String TRACKABLE_PACKAGE = TrackableObject.class.getPackageName() + ".";

    /**
     * Types from outside the engine that game objects keep state in, walked like the engine's own:
     * commons-lang tuples and mutable values. CardType lives in forge-core but is edited in place by
     * the card states that hold it.
     */
    private static final List<Class<?>> HOLDER_TYPES = List.of(
            CardType.class, Pair.class, Triple.class, Mutable.class);

    private static final Map<Class<?>, Boolean> WALKED = new ConcurrentHashMap<>();
    private static final Map<Class<?>, ClassInfo> CLASS_INFO = new ConcurrentHashMap<>();

    private final Game game;
    private final List<ObjectRecord> objects = new ArrayList<>();
    private final List<ContainerRecord> containers = new ArrayList<>();
    // only collected for auditUnwalkedTypes
    private final Set<Class<?>> unwalked;

    private GameCheckpoint(final Game game, final boolean audit) {
        this.game = game;
        unwalked = audit ? new HashSet<>() : null;
        walk(game);
    }

    public static GameCheckpoint capture(final Game game) {
        return new GameCheckpoint(game, false);
    }

    public Game getGame() {
        return game;
    }

    /** Puts every recorded object back to the state it had when the checkpoint was taken. */
    public void restore() {
        // Views whose properties get rewritten have to be re-sent, so note what they hold now too.
        final IdentityHashMap<TrackableObject, EnumSet<TrackableProperty>> viewProps = new IdentityHashMap<>();
        for (final ObjectRecord r : objects) {
            if (r.object instanceof TrackableObject view) {
                viewProps.put(view, propsOf(view));
            }
        }

        // Fields first: hashed containers may hash on them.
        for (final ObjectRecord r : objects) {
            r.restore();
        }
        final List<ContainerRecord> readOnly = new ArrayList<>();
        for (final ContainerRecord r : containers) {
            if (!r.isRestored()) {
                try {
                    r.write();
                } catch (final UnsupportedOperationException e) {
                    readOnly.add(r);
                }
            }
        }

        for (final Map.Entry<TrackableObject, EnumSet<TrackableProperty>> e : viewProps.entrySet()) {
            final EnumSet<TrackableProperty> changed = e.getValue();
            changed.addAll(propsOf(e.getKey()));
            for (final TrackableProperty key : changed) {
                e.getKey().flagAsChanged(key);
            }
        }

        // A container that can't be written is a view onto one that can, and matches again once
        // that one is back. One that still differs shows state this checkpoint did not reach.
        for (final ContainerRecord r : readOnly) {
            if (!r.isRestored()) {
                throw new IllegalStateException("Cannot restore read-only " + r.container().getClass().getName());
            }
        }
    }

    /**
     * The classes of everything reachable from game objects that a checkpoint neither walks into, nor
     * treats as a plain value, nor leaves alone on purpose. Any of them holding mutable game state
     * would not be restored, so this is for auditing that list.
     */
    public static Set<Class<?>> auditUnwalkedTypes(final Game game) {
        return new GameCheckpoint(game, true).unwalked;
    }

    @Override
    public String toString() {
        return String.format("GameCheckpoint[%d objects, %d containers]", objects.size(), containers.size());
    }

    private static EnumSet<TrackableProperty> propsOf(final TrackableObject view) {
        final Map<TrackableProperty, Object> props = view.getProps();
        return props.isEmpty() ? EnumSet.noneOf(TrackableProperty.class) : EnumSet.copyOf(props.keySet());
    }

    // ---- capture ----

    private void walk(final Object root) {
        final IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        final Deque<Object> work = new ArrayDeque<>();
        seen.put(root, Boolean.TRUE);
        work.push(root);
        while (!work.isEmpty()) {
            final Object o = work.pop();
            for (final Object child : record(o)) {
                if (child == null) {
                    continue;
                }
                if (isLeaf(child)) {
                    if (unwalked != null && !isPlainValue(child) && !isOpaque(child.getClass())
                            && hasFields(child.getClass())) {
                        unwalked.add(child.getClass());
                    }
                } else if (seen.put(child, Boolean.TRUE) == null) {
                    work.push(child);
                }
            }
        }
    }

    /** Records one object and returns what it refers to. */
    private Iterable<?> record(final Object o) {
        final Class<?> c = o.getClass();
        if (c.isArray()) {
            if (c.getComponentType().isPrimitive()) {
                containers.add(new ArrayRecord(o, copyArray(o)));
                return List.of();
            }
            final Object[] copy = ((Object[]) o).clone();
            containers.add(new ArrayRecord(o, copy));
            return Arrays.asList(copy);
        }
        if (o instanceof Optional<?> optional) {
            return optional.isPresent() ? List.of(optional.get()) : List.of();
        }
        if (o instanceof Collection<?> coll) {
            final List<Object> copy = new ArrayList<>(coll);
            containers.add(new CollectionRecord(coll, copy));
            return copy;
        }
        if (o instanceof Map<?, ?> map) {
            final List<Map.Entry<Object, Object>> copy = copyEntries(map.entrySet());
            containers.add(new MapRecord(map, copy));
            return keysAndValues(copy);
        }
        if (o instanceof Table<?, ?, ?> table) {
            final List<Table.Cell<?, ?, ?>> copy = new ArrayList<>(table.cellSet());
            final List<Object> refs = new ArrayList<>(copy.size() * 3);
            for (final Table.Cell<?, ?, ?> cell : copy) {
                refs.add(cell.getRowKey());
                refs.add(cell.getColumnKey());
                refs.add(cell.getValue());
            }
            containers.add(new TableRecord(table, copy));
            return refs;
        }
        if (o instanceof Multimap<?, ?> multimap) {
            final List<Map.Entry<Object, Object>> copy = copyEntries(multimap.entries());
            containers.add(new MultimapRecord(multimap, copy));
            return keysAndValues(copy);
        }

        final ClassInfo info = classInfo(c);
        final Object[] values = new Object[info.fields.length];
        for (int i = 0; i < values.length; i++) {
            try {
                values[i] = info.fields[i].get(o);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Cannot read " + info.fields[i], e);
            }
        }
        if (info.hasRestorable) {
            objects.add(new ObjectRecord(o, info, values));
        }
        return Arrays.asList(values);
    }

    private static Object copyArray(final Object array) {
        final int length = Array.getLength(array);
        final Object copy = Array.newInstance(array.getClass().getComponentType(), length);
        System.arraycopy(array, 0, copy, 0, length);
        return copy;
    }

    private static List<Map.Entry<Object, Object>> copyEntries(final Collection<? extends Map.Entry<?, ?>> entries) {
        final List<Map.Entry<Object, Object>> copy = new ArrayList<>(entries.size());
        for (final Map.Entry<?, ?> e : entries) {
            copy.add(new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
        }
        return copy;
    }

    private static List<Object> keysAndValues(final List<Map.Entry<Object, Object>> entries) {
        final List<Object> refs = new ArrayList<>(entries.size() * 2);
        for (final Map.Entry<Object, Object> e : entries) {
            refs.add(e.getKey());
            refs.add(e.getValue());
        }
        return refs;
    }

    /** Values that are what they are: nothing behind them to walk into or restore. */
    private static boolean isPlainValue(final Object o) {
        return o instanceof String || o instanceof Boolean || o instanceof Character || o instanceof Enum<?>
                || o instanceof Class<?> || (o instanceof Number && !(o instanceof Mutable<?>));
    }

    /** True for what is not walked into: plain values, and anything outside the engine's own classes. */
    private static boolean isLeaf(final Object o) {
        if (isPlainValue(o)) {
            return true;
        }
        final Class<?> c = o.getClass();
        if (c.isArray() || o instanceof Collection || o instanceof Map || o instanceof Table || o instanceof Multimap
                || o instanceof Optional) {
            return false;
        }
        return !isWalked(c);
    }

    private static boolean isOpaque(final Class<?> c) {
        for (final Class<?> opaque : OPAQUE_TYPES) {
            if (opaque.isAssignableFrom(c)) {
                return true;
            }
        }
        return false;
    }

    /** An object without fields (a lock, a lambda that captures nothing) has no state to restore. */
    private static boolean hasFields(final Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (final Field f : c.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether objects of this class have their fields recorded and what they refer to followed. */
    private static boolean isWalked(final Class<?> c) {
        return WALKED.computeIfAbsent(c, GameCheckpoint::isEngineOrHolderClass);
    }

    private static boolean isEngineOrHolderClass(final Class<?> c) {
        if (isOpaque(c)) {
            return false;
        }
        final String name = c.getName();
        if (name.startsWith(GAME_PACKAGE) || name.startsWith(TRACKABLE_PACKAGE)) {
            return true;
        }
        for (final Class<?> holder : HOLDER_TYPES) {
            if (holder.isAssignableFrom(c)) {
                return true;
            }
        }
        // what Suppliers.memoize returns, which game objects create their collections lazily in
        return c.getEnclosingClass() == Suppliers.class;
    }

    private static ClassInfo classInfo(final Class<?> c) {
        return CLASS_INFO.computeIfAbsent(c, ClassInfo::new);
    }

    private static final class ClassInfo {
        final Field[] fields;
        /** Parallel to {@link #fields}: whether restore writes it back. Final fields only get walked. */
        final boolean[] restorable;
        final boolean hasRestorable;

        ClassInfo(final Class<?> type) {
            final List<Field> found = new ArrayList<>();
            for (Class<?> c = type; c != null && isWalked(c); c = c.getSuperclass()) {
                for (final Field f : c.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || f.isAnnotationPresent(KeptOnRestore.class)) {
                        continue;
                    }
                    try {
                        f.setAccessible(true);
                    } catch (final RuntimeException e) {
                        continue;
                    }
                    found.add(f);
                }
            }
            fields = found.toArray(new Field[0]);
            restorable = new boolean[fields.length];
            boolean any = false;
            for (int i = 0; i < fields.length; i++) {
                restorable[i] = !Modifier.isFinal(fields[i].getModifiers());
                any |= restorable[i];
            }
            hasRestorable = any;
        }
    }

    // ---- restore ----

    private record ObjectRecord(Object object, ClassInfo info, Object[] values) {
        void restore() {
            for (int i = 0; i < values.length; i++) {
                if (!info.restorable[i]) {
                    continue;
                }
                try {
                    info.fields[i].set(object, values[i]);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException("Cannot restore " + info.fields[i], e);
                }
            }
        }
    }

    private interface ContainerRecord {
        Object container();

        /** Whether the container holds what it held when the checkpoint was taken. */
        boolean isRestored();

        /** Puts back what the container held. Immutable ones never get here: they can't have changed. */
        void write();
    }

    private record ArrayRecord(Object container, Object saved) implements ContainerRecord {
        @Override
        public boolean isRestored() {
            return false;
        }

        @Override
        public void write() {
            System.arraycopy(saved, 0, container, 0, Array.getLength(saved));
        }
    }

    private record CollectionRecord(Collection<?> container, List<Object> saved) implements ContainerRecord {
        @Override
        public boolean isRestored() {
            if (container.size() != saved.size()) {
                return false;
            }
            final Iterator<?> it = container.iterator();
            for (final Object o : saved) {
                if (!it.hasNext() || it.next() != o) {
                    return false;
                }
            }
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void write() {
            container.clear();
            ((Collection<Object>) container).addAll(saved);
        }
    }

    private record MapRecord(Map<?, ?> container, List<Map.Entry<Object, Object>> saved) implements ContainerRecord {
        @Override
        public boolean isRestored() {
            if (container.size() != saved.size()) {
                return false;
            }
            for (final Map.Entry<Object, Object> e : saved) {
                if (container.get(e.getKey()) != e.getValue() || !container.containsKey(e.getKey())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void write() {
            container.clear();
            for (final Map.Entry<Object, Object> e : saved) {
                ((Map<Object, Object>) container).put(e.getKey(), e.getValue());
            }
        }
    }

    private record TableRecord(Table<?, ?, ?> container, List<Table.Cell<?, ?, ?>> saved) implements ContainerRecord {
        @Override
        public boolean isRestored() {
            if (container.size() != saved.size()) {
                return false;
            }
            for (final Table.Cell<?, ?, ?> cell : saved) {
                if (container.get(cell.getRowKey(), cell.getColumnKey()) != cell.getValue()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void write() {
            container.clear();
            for (final Table.Cell<?, ?, ?> cell : saved) {
                ((Table<Object, Object, Object>) container).put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
            }
        }
    }

    private record MultimapRecord(Multimap<?, ?> container, List<Map.Entry<Object, Object>> saved) implements ContainerRecord {
        @Override
        public boolean isRestored() {
            if (container.size() != saved.size()) {
                return false;
            }
            final Iterator<? extends Map.Entry<?, ?>> it = container.entries().iterator();
            for (final Map.Entry<Object, Object> e : saved) {
                final Map.Entry<?, ?> current = it.next();
                if (current.getKey() != e.getKey() || current.getValue() != e.getValue()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void write() {
            container.clear();
            for (final Map.Entry<Object, Object> e : saved) {
                ((Multimap<Object, Object>) container).put(e.getKey(), e.getValue());
            }
        }
    }
}
