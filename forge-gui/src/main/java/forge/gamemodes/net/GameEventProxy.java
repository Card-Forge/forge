package forge.gamemodes.net;

import forge.game.event.GameEvent;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableTypes.TrackableType;
import forge.trackable.Tracker;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a {@link GameEvent} by serializing it into a byte array with all
 * {@link TrackableObject} references replaced by lightweight {@link IdRef}
 * markers. On the client side, the proxy is unwrapped by resolving each
 * IdRef from the client's {@link Tracker}.
 *
 * <p>This avoids Java serialization expanding TrackableObject references into
 * the full game state object graph when events are sent over the network.
 */
public class GameEventProxy implements Serializable, IHasNetLog {
    private static final long serialVersionUID = 1L;

    private final byte[] eventData;

    private GameEventProxy(byte[] eventData) {
        this.eventData = eventData;
    }

    /**
     * Wraps a GameEvent by serializing it with TrackableObject references
     * replaced by IdRef markers. If a tracker is provided, verifies that
     * each replaced object is present in the tracker (server-side sanity
     * check). Returns null if verification fails.
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
     * Wraps a list of events. Uses the tracker for server-side sanity
     * checking: events with unresolvable references are dropped rather
     * than sent (the client would fail to resolve them too).
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
                byte tag = TrackableRef.typeTagFor(trackable);
                if (tag >= 0) {
                    if (tracker != null) {
                        TrackableType<?> type = TrackableRef.trackableTypeFor(tag);
                        if (type != null && tracker.getObj(type, trackable.getId()) == null) {
                            netLog.debug("Server-side check: {} id={} not in tracker",
                                    trackable.getClass().getSimpleName(), trackable.getId());
                            unresolvableRefs = true;
                        }
                    }
                    return new TrackableRef.IdRef(tag, trackable.getId());
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

        @Override
        protected Object resolveObject(Object obj) {
            if (obj instanceof TrackableRef.IdRef ref) {
                TrackableType<?> type = TrackableRef.trackableTypeFor(ref.typeTag);
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
