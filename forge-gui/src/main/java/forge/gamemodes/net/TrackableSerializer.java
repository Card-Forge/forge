package forge.gamemodes.net;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;
import forge.trackable.Tracker;

import org.tinylog.Logger;

import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.*;

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
     * Simple replacement — no tracker, no stale detection.
     * Used by the client encoder (which has no game-state awareness).
     */
    static Object replace(Object obj) {
        if (obj instanceof TrackableObject trackable) {
            byte tag = typeTagFor(trackable);
            if (tag >= 0) {
                return new IdRef(tag, trackable.getId());
            }
        }
        return obj;
    }

    /**
     * Verified replacement with stale detection.
     * Used by the server encoder (which has the game's tracker).
     * Returns {@link StaleCardRef} for stale CardViews, {@link IdRef} for
     * current objects, or the original object for non-trackable types.
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
        // Resolve raw TrackableObjects that were deserialized without IdRef
        // replacement (e.g. events embedded in DeltaPacket, which excludes
        // replacement to protect state data). Return the canonical tracker
        // instance so downstream code can call getTracker() safely.
        if (obj instanceof TrackableObject trackable && trackable.getTracker() == null) {
            byte tag = typeTagFor(trackable);
            if (tag >= 0) {
                TrackableType<?> type = trackableTypeFor(tag);
                if (type != null) {
                    Object canonical = tracker.getObj(type, trackable.getId());
                    if (canonical != null) {
                        return canonical;
                    }
                }
            }
        }
        return obj;
    }

    /**
     * Measures LZ4-compressed serialized size with IdRef replacement,
     * matching the encoder wire format for applyDelta messages.
     */
    public static int measureReplacedSize(Object obj, Tracker tracker) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            LZ4BlockOutputStream lz4Out = new LZ4BlockOutputStream(baos);
            ObjectOutputStream oos = new ReplacingOutputStream(lz4Out, tracker);
            oos.writeObject(obj);
            oos.close();
            return baos.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Measures LZ4-compressed serialized size without replacement,
     * matching the encoder wire format for setGameView messages.
     */
    public static int measurePlainSize(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            LZ4BlockOutputStream lz4Out = new LZ4BlockOutputStream(baos);
            ObjectOutputStream oos = new ObjectOutputStream(lz4Out);
            oos.writeObject(obj);
            oos.close();
            return baos.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private static class ReplacingOutputStream extends ObjectOutputStream {
        private final Tracker tracker;

        ReplacingOutputStream(OutputStream out, Tracker tracker) throws IOException {
            super(out);
            this.tracker = tracker;
            enableReplaceObject(true);
        }

        @Override
        protected Object replaceObject(Object obj) {
            return tracker != null ? replace(obj, tracker) : replace(obj);
        }
    }

    private TrackableSerializer() {}
}
