package forge.gamemodes.net;

import forge.game.card.CardView;
import forge.game.event.GameEvent;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes.TrackableType;
import forge.trackable.Tracker;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GameEventProxy implements Serializable, IHasNetLog {
    private static final long serialVersionUID = 1L;

    private final byte[] eventData;

    private GameEventProxy(byte[] eventData) {
        this.eventData = eventData;
    }

    /**
     * Wraps a {@link GameEvent} by serializing it into a byte array with
     * {@link TrackableObject} references replaced by lightweight {@link IdRef}
     * markers.
     * Returns null if verification fails.
     *
     * <p>This avoids Java serialization expanding TrackableObject references into
     * the full game state object graph when events are sent over the network.
     */
    public static GameEventProxy wrap(GameEvent event, Tracker tracker) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        try (IdReplacingOutputStream out = new IdReplacingOutputStream(baos, tracker)) {
            out.writeObject(event);
            if (out.hasUnresolvableRefs()) {
                return null;
            }
        }
        return new GameEventProxy(baos.toByteArray());
    }

    /**
     * Unwraps the proxy by deserializing the event with IdRef markers
     * resolved to TrackableObjects from the given Tracker. Returns null
     * if any IdRef could not be resolved (the event would have null
     * fields where TrackableObjects were expected).
     */
    public GameEvent unwrap(Tracker tracker) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(eventData);
        try (IdResolvingInputStream in = new IdResolvingInputStream(bais, tracker)) {
            GameEvent event = (GameEvent) in.readObject();
            if (in.hasUnresolvedRefs()) {
                return null;
            }
            return event;
        }
    }

    /**
     * Wraps a list of events. Events with unresolvable references are dropped
     * rather than sent (the client would fail to resolve them too).
     */
    public static List<Object> wrapAll(List<GameEvent> events, Tracker tracker) {
        List<Object> result = new ArrayList<>(events.size());
        for (GameEvent event : events) {
            try {
                GameEventProxy proxy = wrap(event, tracker);
                if (proxy != null) {
                    result.add(proxy);
                } else {
                    netLog.debug("Dropped {} with unresolvable references",
                            event.getClass().getSimpleName());
                }
            } catch (IOException e) {
                netLog.warn("Failed to wrap {}, sending as-is: {}",
                        event.getClass().getSimpleName(), e.getMessage());
                result.add(event);
            }
        }
        return result;
    }

    /**
     * Unwraps a list that may contain both GameEventProxy and plain GameEvent
     * objects. Proxies are unwrapped; plain events pass through unchanged.
     */
    public static List<GameEvent> unwrapAll(List<?> items, Tracker tracker) {
        List<GameEvent> result = new ArrayList<>(items.size());
        for (Object item : items) {
            if (item instanceof GameEventProxy proxy) {
                try {
                    GameEvent event = proxy.unwrap(tracker);
                    if (event != null) {
                        result.add(event);
                    } else {
                        netLog.debug("Dropped event with unresolved references");
                    }
                } catch (IOException | ClassNotFoundException e) {
                    netLog.warn("Failed to unwrap GameEventProxy: {}", e.getMessage());
                }
            } else if (item instanceof GameEvent event) {
                result.add(event);
            }
        }
        return result;
    }

    /**
     * Lightweight serializable marker that replaces a TrackableObject reference
     * during proxy serialization.
     */
    static final class IdRef implements Serializable {
        private static final long serialVersionUID = 1L;
        final byte typeTag;
        final int id;

        IdRef(byte typeTag, int id) {
            this.typeTag = typeTag;
            this.id = id;
        }
    }

    /**
     * Marker for a stale CardView reference — the event holds a previous
     * incarnation of a card (same ID, different Java object) that has since
     * been replaced in the tracker by a zone-change copy. Carries the
     * image key and name from the original so the client can construct a
     * detached CardView with the correct display data.
     */
    static final class StaleCardRef implements Serializable {
        private static final long serialVersionUID = 1L;
        final int id;
        final String imageKey;
        final String name;

        StaleCardRef(int id, String imageKey, String name) {
            this.id = id;
            this.imageKey = imageKey;
            this.name = name;
        }
    }

    /**
     * ObjectOutputStream that replaces TrackableObject with an IdRef.
     * If a tracker is provided, verifies each ID is resolvable as a
     * server-side sanity check.
     */
    private static class IdReplacingOutputStream extends ObjectOutputStream {
        private final Tracker tracker;
        private boolean unresolvableRefs;

        IdReplacingOutputStream(OutputStream out, Tracker tracker) throws IOException {
            super(out);
            this.tracker = tracker;
            enableReplaceObject(true);
        }

        boolean hasUnresolvableRefs() {
            return unresolvableRefs;
        }

        @Override
        protected Object replaceObject(Object obj) {
            if (obj instanceof TrackableObject trackable) {
                int tag = DeltaPacket.typeTagFor(trackable);
                // Only CardView and PlayerView are replaced with IdRef markers. These two
                // types carry the largest object graphs and are always present in the GameView
                // (registered via updateObjLookup on the IO thread before proxy unwrapping).
                // Other TrackableObject types (StackItemView, SpellAbilityView, CombatView)
                // are either ephemeral or not reachable from GameView's property graph, so
                // they serialize normally.
                if (tag == DeltaPacket.TYPE_CARD_VIEW || tag == DeltaPacket.TYPE_PLAYER_VIEW) {
                    if (tracker != null) {
                        TrackableType<?> type = DeltaPacket.trackableTypeFor(tag);
                        if (type != null) {
                            Object tracked = tracker.getObj(type, trackable.getId());
                            if (tracked == null) {
                                netLog.debug("Server-side check: {} id={} not in tracker",
                                        trackable.getClass().getSimpleName(), trackable.getId());
                                unresolvableRefs = true;
                            } else if (tracked != trackable && tag == DeltaPacket.TYPE_CARD_VIEW) {
                                // Stale reference: the event holds a previous incarnation
                                // of this card (e.g. ability source that changed zones).
                                // Preserve the image key so the client displays correctly
                                CardView cv = (CardView) trackable;
                                String imgKey = cv.getCurrentState() != null
                                        ? cv.getCurrentState().getImageKey(null) : null;
                                return new StaleCardRef(cv.getId(), imgKey, cv.getName());
                            }
                        }
                    }
                    return new IdRef((byte) tag, trackable.getId());
                }
            }
            return obj;
        }
    }

    private static class IdResolvingInputStream extends ObjectInputStream {
        private final Tracker tracker;
        private boolean unresolvedRefs;

        IdResolvingInputStream(InputStream in, Tracker tracker) throws IOException {
            super(in);
            this.tracker = tracker;
            enableResolveObject(true);
        }

        boolean hasUnresolvedRefs() {
            return unresolvedRefs;
        }

        // needed for cross-platform play because Android implements Records via desugaring to regular classes,
        // causing the serialVersionUID to auto-compute instead of default 0L
        // (this approach avoids having to hardcode it on each individual GameEvent instead)
        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass streamDesc = super.readClassDescriptor();
            try {
                Class<?> localClass = Class.forName(streamDesc.getName());
                ObjectStreamClass localDesc = ObjectStreamClass.lookup(localClass);
                if (localDesc != null && streamDesc.getSerialVersionUID() != localDesc.getSerialVersionUID()) {
                    return localDesc;
                }
            } catch (ClassNotFoundException ignored) {
                // Class not found locally — fall through to stream descriptor
            }
            return streamDesc;
        }

        @Override
        protected Object resolveObject(Object obj) {
            if (obj instanceof StaleCardRef ref) {
                // Create a detached CardView with the correct image key.
                // Not registered in the tracker — used only for display
                // (game log thumbnail) so it won't affect live game state
                CardView detached = new CardView(ref.id, tracker);
                if (ref.name != null) {
                    detached.set(TrackableProperty.Name, ref.name);
                    detached.getCurrentState().set(TrackableProperty.Name, ref.name);
                }
                if (ref.imageKey != null) {
                    detached.getCurrentState().set(TrackableProperty.ImageKey, ref.imageKey);
                }
                return detached;
            }
            if (obj instanceof IdRef ref) {
                TrackableType<?> type = DeltaPacket.trackableTypeFor(ref.typeTag);
                if (type != null) {
                    Object resolved = tracker.getObj(type, ref.id);
                    if (resolved == null) {
                        netLog.debug("Could not resolve {} id={} from Tracker",
                                type.getClass().getSimpleName(), ref.id);
                        unresolvedRefs = true;
                    }
                    return resolved;
                }
            }
            return obj;
        }
    }
}
