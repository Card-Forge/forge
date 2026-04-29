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

/** Replaces CardView/PlayerView refs with compact wire markers; resolves on the receiving side. */
public final class TrackableSerializer {
    private static final TaggedLogger netLog = Logger.tag("NETWORK");

    static final byte TYPE_CARD_VIEW = 0;
    static final byte TYPE_PLAYER_VIEW = 1;

    /** Marker for tracker-stable refs (top-level protocol method args). */
    static final class IdRef implements Serializable {
        private static final long serialVersionUID = 1L;
        final byte typeTag;
        final int id;

        IdRef(byte typeTag, int id) {
            this.typeTag = typeTag;
            this.id = id;
        }
    }

    /** CardView ref inside a wrapped event. {@code preserveSnapshot=true} forces fallback even if the tracker has the id. */
    static final class EventCardRef implements Serializable {
        private static final long serialVersionUID = 1L;
        final int id;
        final String name;
        final String imageKey;
        final boolean preserveSnapshot;

        EventCardRef(int id, String name, String imageKey, boolean preserveSnapshot) {
            this.id = id;
            this.name = name;
            this.imageKey = imageKey;
            this.preserveSnapshot = preserveSnapshot;
        }
    }

    /** PlayerView ref inside a wrapped event. {@code preserveSnapshot=true} forces fallback even if the tracker has the id. */
    static final class EventPlayerRef implements Serializable {
        private static final long serialVersionUID = 1L;
        final int id;
        final String name;
        final boolean preserveSnapshot;

        EventPlayerRef(int id, String name, boolean preserveSnapshot) {
            this.id = id;
            this.name = name;
            this.preserveSnapshot = preserveSnapshot;
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

    static Object replace(Object obj, Tracker tracker, boolean eventMode) {
        if (obj instanceof TrackableObject trackable) {
            byte tag = typeTagFor(trackable);
            if (tag < 0) return obj;

            if (!eventMode) {
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
            if (tag == TYPE_CARD_VIEW) {
                CardView cv = (CardView) trackable;
                String imgKey = cv.getCurrentState() != null
                        ? cv.getCurrentState().getImageKey(null) : null;
                return new EventCardRef(trackable.getId(), cv.getName(), imgKey, preserveSnapshot);
            }
            if (tag == TYPE_PLAYER_VIEW) {
                PlayerView pv = (PlayerView) trackable;
                return new EventPlayerRef(trackable.getId(), pv.getName(), preserveSnapshot);
            }
        }
        return obj;
    }

    static Object resolve(Object obj, Tracker tracker) {
        if (obj instanceof EventCardRef ref) {
            if (!ref.preserveSnapshot) {
                CardView fromTracker = tracker.getObj(TrackableTypes.CardViewType, ref.id);
                if (fromTracker != null) return fromTracker;
            }
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
        if (obj instanceof EventPlayerRef ref) {
            if (!ref.preserveSnapshot) {
                PlayerView fromTracker = tracker.getObj(TrackableTypes.PlayerViewType, ref.id);
                if (fromTracker != null) return fromTracker;
            }
            PlayerView detached = new PlayerView(ref.id, tracker);
            if (ref.name != null) {
                detached.set(TrackableProperty.Name, ref.name);
            }
            return detached;
        }
        if (obj instanceof IdRef ref) {
            TrackableType<?> type = trackableTypeFor(ref.typeTag);
            if (type != null) {
                Object resolved = tracker.getObj(type, ref.id);
                if (resolved == null) {
                    netLog.warn("Could not resolve IdRef(tag={}, id={}) from Tracker", ref.typeTag, ref.id);
                }
                return resolved;
            }
        }
        return obj;
    }

    /** Approximate serialized size for telemetry. */
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

    static final class WrappedEvent implements Serializable {
        private static final long serialVersionUID = 1L;
        final byte[] data;
        WrappedEvent(byte[] data) { this.data = data; }
    }

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
