package forge.gamemodes.net;

import forge.game.card.CardView;
import forge.game.event.GameEvent;
import forge.game.player.PlayerView;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;
import forge.trackable.Tracker;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles serialization of {@link TrackableObject} references across the network.
 * Replaces CardView/PlayerView with lightweight {@link IdRef} markers during
 * encoding and resolves them back from the Tracker during decoding.
 *
 * <p>Used by the Netty encoder/decoder pipeline ({@link CompatibleObjectEncoder},
 * {@link CompatibleObjectDecoder}) and the mobile codec path
 * ({@link CObjectOutputStream}, {@link CObjectInputStream}).
 */
public final class TrackableSerializer {
    private static final TaggedLogger netLog = Logger.tag("NETWORK");

    static final byte TYPE_CARD_VIEW = 0;
    static final byte TYPE_PLAYER_VIEW = 1;

    /** Marker for tracker-stable refs (top-level protocol method args, and PlayerView in events). */
    record IdRef(byte typeTag, int id) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    /** CardView ref inside a wrapped event. {@code preserveSnapshot=true} forces fallback even if the tracker has the id. */
    record EventCardRef(int id, String name, String imageKey, boolean preserveSnapshot) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    static byte typeTagFor(TrackableObject obj) {
        if (obj instanceof CardView) return TYPE_CARD_VIEW;
        if (obj instanceof PlayerView) return TYPE_PLAYER_VIEW;
        return -1;
    }

    static TrackableType<?> trackableTypeFor(byte typeTag) {
        switch (typeTag) {
            case TYPE_CARD_VIEW: return TrackableTypes.CardViewType;
            case TYPE_PLAYER_VIEW: return TrackableTypes.PlayerViewType;
            default: return null;
        }
    }

    /**
     * Replaces TrackableObject references with {@link IdRef} markers, or
     * {@link EventCardRef} markers for CardViews inside wrapped events
     * ({@code eventMode = true}). When the tracker holds a different object
     * for the CardView's id (zone-change copy), {@code preserveSnapshot} is
     * set so the receiver decodes a detached CardView from the carried name
     * and image key. When {@code tracker} is null, the snapshot check is
     * skipped (used by the client encoder, which has no game-state awareness).
     */
    static Object replace(Object obj, Tracker tracker, boolean eventMode) {
        if (obj instanceof TrackableObject trackable) {
            byte tag = typeTagFor(trackable);
            if (tag < 0) return obj;

            if (!eventMode || tag == TYPE_PLAYER_VIEW) {
                return new IdRef(tag, trackable.getId());
            }

            boolean preserveSnapshot = false;
            if (tracker != null) {
                TrackableType<?> type = trackableTypeFor(tag);
                if (type != null) {
                    Object tracked = tracker.getObj(type, trackable.getId());
                    if (tracked != null && tracked != trackable) {
                        preserveSnapshot = true;
                    }
                }
            }
            CardView cv = (CardView) trackable;
            String imgKey = cv.getCurrentState() != null
                    ? cv.getCurrentState().getImageKey(null) : null;
            return new EventCardRef(trackable.getId(), cv.getName(), imgKey, preserveSnapshot);
        }
        return obj;
    }

    /**
     * Resolves {@link IdRef} and {@link EventCardRef} markers back to
     * TrackableObjects from the given Tracker.
     */
    static Object resolve(Object obj, Tracker tracker) {
        if (obj instanceof EventCardRef ref) {
            if (!ref.preserveSnapshot()) {
                CardView fromTracker = tracker.getObj(TrackableTypes.CardViewType, ref.id());
                if (fromTracker != null) return fromTracker;
            }
            CardView detached = new CardView(ref.id(), tracker);
            if (ref.name() != null) {
                detached.set(TrackableProperty.Name, ref.name());
                detached.getCurrentState().set(TrackableProperty.Name, ref.name());
            }
            if (ref.imageKey() != null) {
                detached.getCurrentState().set(TrackableProperty.ImageKey, ref.imageKey());
            }
            return detached;
        }
        if (obj instanceof IdRef ref) {
            TrackableType<?> type = trackableTypeFor(ref.typeTag());
            if (type != null) {
                Object resolved = tracker.getObj(type, ref.id());
                if (resolved == null) {
                    netLog.warn("Could not resolve IdRef(tag={}, id={}) from Tracker", ref.typeTag(), ref.id());
                }
                return resolved;
            }
        }
        return obj;
    }

    /**
     * Measures serialized size matching the encoder wire format
     * for applyDelta messages with IdRef replacement (when tracker not null).
     * otherwise for setGameView messages.
     */
    public static int measureSize(Object obj, Tracker tracker) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            LZ4BlockOutputStream lz4Out = new LZ4BlockOutputStream(baos);
            ObjectOutputStream oos = tracker == null ? new ObjectOutputStream(lz4Out) : new ReplacingOutputStream(lz4Out, tracker, false);
            oos.writeObject(obj);
            oos.close();
            return baos.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Serializable wrapper for a GameEvent whose TrackableObject references
     * have been replaced with IdRef/EventCardRef markers. Stored in
     * DeltaPacket.events so events travel as compact byte arrays rather than
     * full object graphs. Unwrapped after delta state is applied, when the
     * client tracker is populated.
     */
    static final class WrappedEvent implements Serializable {
        private static final long serialVersionUID = 1L;
        final byte[] data;
        WrappedEvent(byte[] data) { this.data = data; }
    }

    /**
     * Wraps GameEvents by serializing each with IdRef replacement.
     * Events that fail to serialize are dropped (logged).
     */
    public static List<Object> wrapEvents(List<GameEvent> events, Tracker tracker) {
        List<Object> wrapped = new ArrayList<>(events.size());
        for (GameEvent event : events) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
                try (ReplacingOutputStream out = new ReplacingOutputStream(baos, tracker, true)) {
                    out.writeObject(event);
                }
                wrapped.add(new WrappedEvent(baos.toByteArray()));
            } catch (IOException e) {
                netLog.warn("Failed to wrap event {}: {}", event.getClass().getSimpleName(), e.getMessage());
            }
        }
        return wrapped;
    }

    /**
     * Unwraps events by deserializing with IdRef resolution from the tracker.
     * Called after delta state is applied so new objects are resolvable.
     * Events that fail to unwrap are dropped (logged).
     */
    public static List<GameEvent> unwrapEvents(List<Object> items, Tracker tracker) {
        List<GameEvent> events = new ArrayList<>(items.size());
        for (Object item : items) {
            if (item instanceof WrappedEvent we) {
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(we.data);
                    try (ResolvingInputStream in = new ResolvingInputStream(bais, tracker)) {
                        Object obj = in.readObject();
                        if (obj instanceof GameEvent e) events.add(e);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    netLog.warn("Failed to unwrap event: {}", e.getMessage());
                }
            }
        }
        return events;
    }

    static class ReplacingOutputStream extends ObjectOutputStream {
        private final Tracker tracker;
        private final boolean eventMode;

        ReplacingOutputStream(OutputStream out, Tracker tracker, boolean eventMode) throws IOException {
            super(out);
            this.tracker = tracker;
            this.eventMode = eventMode;
            enableReplaceObject(true);
        }

        @Override
        protected Object replaceObject(Object obj) {
            return replace(obj, tracker, eventMode);
        }
    }

    static class ResolvingInputStream extends ObjectInputStream {
        private final Tracker tracker;

        ResolvingInputStream(InputStream in, Tracker tracker) throws IOException {
            super(in);
            this.tracker = tracker;
            enableResolveObject(true);
        }

        @Override
        protected Object resolveObject(Object obj) {
            return resolve(obj, tracker);
        }
    }

    private TrackableSerializer() {}
}
