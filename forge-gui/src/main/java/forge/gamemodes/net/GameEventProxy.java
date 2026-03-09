package forge.gamemodes.net;

import forge.game.card.CardView;
import forge.game.event.GameEvent;
import forge.game.player.PlayerView;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableTypes;
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
     * replaced by IdRef markers.
     */
    public static GameEventProxy wrap(GameEvent event) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        try (IdReplacingOutputStream out = new IdReplacingOutputStream(baos)) {
            out.writeObject(event);
        }
        return new GameEventProxy(baos.toByteArray());
    }

    /**
     * Unwraps the proxy by deserializing the event with IdRef markers
     * resolved to TrackableObjects from the given Tracker.
     */
    public GameEvent unwrap(Tracker tracker) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(eventData);
        try (IdResolvingInputStream in = new IdResolvingInputStream(bais, tracker)) {
            return (GameEvent) in.readObject();
        }
    }

    /**
     * Wraps a list of events. On failure for any individual event,
     * keeps the original event as a fallback.
     */
    public static List<Object> wrapAll(List<GameEvent> events) {
        List<Object> result = new ArrayList<>(events.size());
        for (GameEvent event : events) {
            try {
                result.add(wrap(event));
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
                    result.add(proxy.unwrap(tracker));
                } catch (IOException | ClassNotFoundException e) {
                    netLog.warn("Failed to unwrap GameEventProxy: {}", e.getMessage());
                }
            } else if (item instanceof GameEvent event) {
                result.add(event);
            }
        }
        return result;
    }

    // Only CardView and PlayerView are replaced with IdRef markers. These two
    // types carry the largest object graphs and are always present in the GameView
    // (registered via updateObjLookup on the IO thread before proxy unwrapping).
    // Other TrackableObject types (StackItemView, SpellAbilityView, CombatView)
    // are either ephemeral or not reachable from GameView's property graph, so
    // they serialize normally.
    private static final byte TYPE_CARD_VIEW = 0;
    private static final byte TYPE_PLAYER_VIEW = 1;

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
     * Returns the type tag for the given TrackableObject, or -1 if unsupported.
     */
    private static byte typeTagFor(TrackableObject obj) {
        if (obj instanceof CardView) return TYPE_CARD_VIEW;
        if (obj instanceof PlayerView) return TYPE_PLAYER_VIEW;
        return -1;
    }

    /**
     * Returns the TrackableType for the given type tag, for Tracker lookup.
     */
    private static TrackableType<?> trackableTypeFor(byte typeTag) {
        switch (typeTag) {
            case TYPE_CARD_VIEW: return TrackableTypes.CardViewType;
            case TYPE_PLAYER_VIEW: return TrackableTypes.PlayerViewType;
            default: return null;
        }
    }

    /**
     * ObjectOutputStream that replaces every TrackableObject with an IdRef.
     */
    private static class IdReplacingOutputStream extends ObjectOutputStream {
        IdReplacingOutputStream(OutputStream out) throws IOException {
            super(out);
            enableReplaceObject(true);
        }

        @Override
        protected Object replaceObject(Object obj) {
            if (obj instanceof TrackableObject trackable) {
                byte tag = typeTagFor(trackable);
                if (tag >= 0) {
                    return new IdRef(tag, trackable.getId());
                }
            }
            return obj;
        }
    }

    /**
     * ObjectInputStream that resolves IdRef markers back to TrackableObjects
     * from the Tracker.
     */
    private static class IdResolvingInputStream extends ObjectInputStream {
        private final Tracker tracker;

        IdResolvingInputStream(InputStream in, Tracker tracker) throws IOException {
            super(in);
            this.tracker = tracker;
            enableResolveObject(true);
        }

        @Override
        protected Object resolveObject(Object obj) {
            if (obj instanceof IdRef ref) {
                TrackableType<?> type = trackableTypeFor(ref.typeTag);
                if (type != null) {
                    Object resolved = tracker.getObj(type, ref.id);
                    if (resolved == null) {
                        netLog.debug("Could not resolve {} id={} from Tracker",
                                type.getClass().getSimpleName(), ref.id);
                    }
                    return resolved;
                }
            }
            return obj;
        }
    }
}
