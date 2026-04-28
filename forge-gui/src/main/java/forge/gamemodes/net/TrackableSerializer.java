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
    static final byte TYPE_CARD_VIEW = 0;
    static final byte TYPE_PLAYER_VIEW = 1;

    /**
     * Lightweight serializable marker that replaces a TrackableObject reference.
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
     * Marker for a stale CardView reference — the object holds a previous
     * incarnation of a card (same ID, different Java object) that has since
     * been replaced in the tracker by a zone-change copy. Carries the
     * image key and name so the decoder can construct a detached CardView
     * with the correct display data.
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
     * {@link StaleCardRef} markers for CardViews whose tracker entry has
     * been replaced by a zone-change copy. When {@code tracker} is null,
     * stale detection is skipped (used by the client encoder, which has
     * no game-state awareness).
     */
    static Object replace(Object obj, Tracker tracker) {
        if (obj instanceof TrackableObject trackable) {
            byte tag = typeTagFor(trackable);
            if (tag >= 0) {
                if (tracker != null) {
                    TrackableType<?> type = trackableTypeFor(tag);
                    if (type != null) {
                        Object tracked = tracker.getObj(type, trackable.getId());
                        if (tracked != trackable && tag == TYPE_CARD_VIEW && tracked != null) {
                            // Stale reference: previous incarnation of this card
                            CardView cv = (CardView) trackable;
                            String imgKey = cv.getCurrentState() != null
                                    ? cv.getCurrentState().getImageKey(null) : null;
                            return new StaleCardRef(cv.getId(), imgKey, cv.getName());
                        }
                    }
                }
                return new IdRef(tag, trackable.getId());
            }
        }
        return obj;
    }

    /**
     * Resolves {@link IdRef} and {@link StaleCardRef} markers back to
     * TrackableObjects from the given Tracker.
     */
    static Object resolve(Object obj, Tracker tracker) {
        if (obj instanceof StaleCardRef ref) {
            // Create a detached CardView with the correct image key.
            // Not registered in the tracker — used only for display
            // (game log thumbnail) so it won't affect live game state.
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
            TrackableType<?> type = trackableTypeFor(ref.typeTag);
            if (type != null) {
                Object resolved = tracker.getObj(type, ref.id);
                if (resolved == null) {
                    Logger.warn("Could not resolve IdRef(tag={}, id={}) from Tracker", ref.typeTag, ref.id);
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
            ObjectOutputStream oos = tracker == null ? new ObjectOutputStream(lz4Out) : new ReplacingOutputStream(lz4Out, tracker);
            oos.writeObject(obj);
            oos.close();
            return baos.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Serializable wrapper for a GameEvent whose TrackableObject references
     * have been replaced with IdRef/StaleCardRef markers. Stored in
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
                try (ReplacingOutputStream out = new ReplacingOutputStream(baos, tracker)) {
                    out.writeObject(event);
                }
                wrapped.add(new WrappedEvent(baos.toByteArray()));
            } catch (IOException e) {
                Logger.warn("Failed to wrap event {}: {}", event.getClass().getSimpleName(), e.getMessage());
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
                    Logger.warn("Failed to unwrap event: {}", e.getMessage());
                }
            }
        }
        return events;
    }

    static class ReplacingOutputStream extends ObjectOutputStream {
        private final Tracker tracker;

        ReplacingOutputStream(OutputStream out, Tracker tracker) throws IOException {
            super(out);
            this.tracker = tracker;
            enableReplaceObject(true);
        }

        @Override
        protected Object replaceObject(Object obj) {
            return replace(obj, tracker);
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
