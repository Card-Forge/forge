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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
 * place: every field of every {@code forge.game} and {@code forge.trackable} object reachable from
 * the {@link Game}, and the contents of every collection, map and table they hold. Restoring
 * writes those values back into the same objects. A card that has since changed zones was
 * replaced by a new object; restoring the zone's contents puts the original object back, so
 * anything still pointing at it (triggers, remembered lists, the stack) stays valid without any
 * id mapping. Objects created after the checkpoint simply stop being reachable.
 * <p>
 * Walking by reflection keeps this complete as the engine grows: a field added to a card or a
 * player is covered without anyone having to remember undo. The cost is that a few things must be
 * kept out on purpose - see {@link #OPAQUE_TYPES} and {@link #SKIPPED_FIELDS}.
 */
public final class GameCheckpoint {

    /** Reachable, but not part of the game state to rewind: controllers (GUI, AI), the match, the log. */
    private static final List<Class<?>> OPAQUE_TYPES = List.of(
            PlayerController.class, Match.class, GameRules.class, RegisteredPlayer.class,
            GameLog.class, GameOutcome.class, GameEvent.class, GameSnapshot.class,
            GameCheckpoint.class, UndoHistory.class, DrawOffer.class);

    /**
     * Fields left as they are on restore.
     * Card ids keep counting up, so a card made after the undo can't reuse the id of one the GUI or a
     * network client last saw as something else. Dirty tracking for network sync is bookkeeping about
     * what clients have been sent, not game state; a restore marks what changed instead.
     */
    private static final Set<String> SKIPPED_FIELDS = Set.of(
            "forge.game.Game#cardIdCounter",
            "forge.game.Game#hiddenCardIdCounter",
            "forge.game.Game#previousGameState",
            "forge.game.Game#drawOffer",
            "forge.game.Game#undoHistory",
            "forge.game.phase.PhaseHandler#resumeAtStepStart",
            "forge.game.phase.PhaseHandler#inLoopStep",
            "forge.trackable.TrackableObject#version",
            "forge.trackable.TrackableObject#consumers",
            "forge.trackable.TrackableObject#copyingProps");

    private static final Map<Class<?>, ClassInfo> CLASS_INFO = new ConcurrentHashMap<>();

    private final Game game;
    private final List<ObjectRecord> objects = new ArrayList<>();
    private final List<ContainerRecord> containers = new ArrayList<>();
    private final long captureNanos;
    private final Map<String, Integer> unwalked = new java.util.TreeMap<>();

    private GameCheckpoint(final Game game) {
        this.game = game;
        long start = System.nanoTime();
        walk(game);
        captureNanos = System.nanoTime() - start;
    }

    public static GameCheckpoint capture(final Game game) {
        return new GameCheckpoint(game);
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
        for (final ContainerRecord r : containers) {
            r.restore();
        }

        for (final Map.Entry<TrackableObject, EnumSet<TrackableProperty>> e : viewProps.entrySet()) {
            final EnumSet<TrackableProperty> changed = e.getValue();
            changed.addAll(propsOf(e.getKey()));
            e.getKey().flagAllAsChanged(changed);
        }
    }

    /**
     * The classes of everything reachable from game objects that a checkpoint neither walks into nor
     * treats as a plain value, with how often each occurs. Any of them holding mutable game state would
     * not be restored, so this is for auditing that list.
     */
    public static Map<String, Integer> auditUnwalkedTypes(final Game game) {
        final GameCheckpoint checkpoint = new GameCheckpoint(game);
        return checkpoint.unwalked;
    }

    @Override
    public String toString() {
        return String.format("GameCheckpoint[%d objects, %d containers, %.1f ms]",
                objects.size(), containers.size(), captureNanos / 1_000_000.0);
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
                    if (!isPlainValue(child)) {
                        unwalked.merge(child.getClass().getName(), 1, Integer::sum);
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
            if (isMutable(c)) {
                containers.add(new CollectionRecord(coll, copy));
            }
            return copy;
        }
        if (o instanceof Map<?, ?> map) {
            final List<Map.Entry<Object, Object>> copy = new ArrayList<>(map.size());
            final List<Object> refs = new ArrayList<>(map.size() * 2);
            for (final Map.Entry<?, ?> e : map.entrySet()) {
                copy.add(new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
                refs.add(e.getKey());
                refs.add(e.getValue());
            }
            if (isMutable(c)) {
                containers.add(new MapRecord(map, copy));
            }
            return refs;
        }
        if (o instanceof Table<?, ?, ?> table) {
            final List<Table.Cell<?, ?, ?>> copy = new ArrayList<>(table.cellSet());
            final List<Object> refs = new ArrayList<>(copy.size() * 3);
            for (final Table.Cell<?, ?, ?> cell : copy) {
                refs.add(cell.getRowKey());
                refs.add(cell.getColumnKey());
                refs.add(cell.getValue());
            }
            if (isMutable(c)) {
                containers.add(new TableRecord(table, copy));
            }
            return refs;
        }
        if (o instanceof Multimap<?, ?> multimap) {
            final List<Map.Entry<Object, Object>> copy = new ArrayList<>(multimap.size());
            final List<Object> refs = new ArrayList<>(multimap.size() * 2);
            for (final Map.Entry<?, ?> e : multimap.entries()) {
                copy.add(new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
                refs.add(e.getKey());
                refs.add(e.getValue());
            }
            if (isMutable(c)) {
                containers.add(new MultimapRecord(multimap, copy));
            }
            return refs;
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

    /** True for values that are not walked into: plain values, and anything outside the game's own classes. */
    private static boolean isPlainValue(final Object o) {
        return o instanceof String || o instanceof Number || o instanceof Boolean || o instanceof Character
                || o instanceof Enum<?> || o instanceof Class<?>;
    }

    private static boolean isLeaf(final Object o) {
        if (isPlainValue(o)) {
            return true;
        }
        final Class<?> c = o.getClass();
        if (c.isArray() || o instanceof Collection || o instanceof Map || o instanceof Table || o instanceof Multimap
                || o instanceof Optional) {
            return false;
        }
        return !isGameClass(c);
    }

    private static boolean isGameClass(final Class<?> c) {
        for (final Class<?> opaque : OPAQUE_TYPES) {
            if (opaque.isAssignableFrom(c)) {
                return false;
            }
        }
        final String name = c.getName();
        // CardType lives in forge-core but is edited in place by the card states that hold it.
        return name.startsWith("forge.game.") || name.startsWith("forge.trackable.") || c == CardType.class
                || isForeignHolder(name);
    }

    /**
     * Library types game objects keep state in, walked like game classes: lazily created collections in
     * Guava's Suppliers.memoize, and commons-lang tuples and mutable values.
     */
    private static boolean isForeignHolder(final String className) {
        return className.startsWith("com.google.common.base.Suppliers$")
                || className.startsWith("org.apache.commons.lang3.tuple.")
                || className.startsWith("org.apache.commons.lang3.mutable.");
    }

    /** Immutable and read-only containers can't have changed, and would throw if restored. */
    private static boolean isMutable(final Class<?> c) {
        final String name = c.getName();
        return !(name.startsWith("com.google.common.collect.Immutable")
                || name.startsWith("com.google.common.collect.Regular")
                || name.startsWith("com.google.common.collect.Singleton")
                || name.startsWith("java.util.ImmutableCollections")
                || name.startsWith("java.util.Collections$Unmodifiable")
                || name.startsWith("java.util.Collections$Empty")
                || name.startsWith("java.util.Collections$Singleton")
                || name.startsWith("java.util.Arrays$ArrayList"));
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
            // Captured variables of lambdas and records can't be written; they're only walked.
            final boolean readOnlyType = type.isHidden() || type.isRecord();
            for (Class<?> c = type; c != null && c != Object.class
                    && (c.getName().startsWith("forge.") || isForeignHolder(c.getName())); c = c.getSuperclass()) {
                for (final Field f : c.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || SKIPPED_FIELDS.contains(c.getName() + "#" + f.getName())) {
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
                restorable[i] = !readOnlyType && !Modifier.isFinal(fields[i].getModifiers());
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
        void restore();
    }

    private record ArrayRecord(Object array, Object saved) implements ContainerRecord {
        @Override
        public void restore() {
            System.arraycopy(saved, 0, array, 0, Array.getLength(saved));
        }
    }

    private record CollectionRecord(Collection<?> collection, List<Object> saved) implements ContainerRecord {
        @Override
        @SuppressWarnings("unchecked")
        public void restore() {
            if (sameElements(collection, saved)) {
                return;
            }
            try {
                collection.clear();
                ((Collection<Object>) collection).addAll(saved);
            } catch (final UnsupportedOperationException e) {
                // a read-only view onto something restored elsewhere
            }
        }
    }

    private record MapRecord(Map<?, ?> map, List<Map.Entry<Object, Object>> saved) implements ContainerRecord {
        @Override
        @SuppressWarnings("unchecked")
        public void restore() {
            if (map.size() == saved.size()) {
                boolean same = true;
                for (final Map.Entry<Object, Object> e : saved) {
                    if (map.get(e.getKey()) != e.getValue() || !map.containsKey(e.getKey())) {
                        same = false;
                        break;
                    }
                }
                if (same) {
                    return;
                }
            }
            try {
                map.clear();
                for (final Map.Entry<Object, Object> e : saved) {
                    ((Map<Object, Object>) map).put(e.getKey(), e.getValue());
                }
            } catch (final UnsupportedOperationException e) {
                // a read-only view onto something restored elsewhere
            }
        }
    }

    private record TableRecord(Table<?, ?, ?> table, List<Table.Cell<?, ?, ?>> saved) implements ContainerRecord {
        @Override
        @SuppressWarnings("unchecked")
        public void restore() {
            try {
                table.clear();
                for (final Table.Cell<?, ?, ?> cell : saved) {
                    ((Table<Object, Object, Object>) table).put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
                }
            } catch (final UnsupportedOperationException e) {
                // a read-only view onto something restored elsewhere
            }
        }
    }

    private record MultimapRecord(Multimap<?, ?> multimap, List<Map.Entry<Object, Object>> saved) implements ContainerRecord {
        @Override
        @SuppressWarnings("unchecked")
        public void restore() {
            try {
                multimap.clear();
                for (final Map.Entry<Object, Object> e : saved) {
                    ((Multimap<Object, Object>) multimap).put(e.getKey(), e.getValue());
                }
            } catch (final UnsupportedOperationException e) {
                // a read-only view onto something restored elsewhere
            }
        }
    }

    private static boolean sameElements(final Collection<?> current, final List<Object> saved) {
        if (current.size() != saved.size()) {
            return false;
        }
        final Iterator<?> it = current.iterator();
        for (final Object o : saved) {
            if (!it.hasNext() || it.next() != o) {
                return false;
            }
        }
        return true;
    }
}
