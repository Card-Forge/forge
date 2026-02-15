package forge.gamemodes.net;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.trackable.Tracker;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableObjectType;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Binary deserializer for network delta sync.
 * Reads the compact binary format written by NetworkTrackableSerializer
 * and resolves object IDs via the Tracker.
 */
public class NetworkTrackableDeserializer {
    private final DataInputStream dis;
    private final Tracker tracker;
    private int bytesRead = 0;

    public NetworkTrackableDeserializer(DataInputStream dis, Tracker tracker) {
        this.dis = dis;
        this.tracker = tracker;
    }

    /**
     * Get the number of bytes read so far.
     * Used for debugging deserialization issues.
     */
    public int getBytesRead() {
        return bytesRead;
    }

    /**
     * Reset the byte counter.
     */
    public void resetBytesRead() {
        this.bytesRead = 0;
    }

    public String readString() throws IOException {
        int length = dis.readInt();
        bytesRead += 4;
        if (length == -1) {
            return null;
        }
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        bytesRead += length;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public boolean readBoolean() throws IOException {
        bytesRead += 1;
        return dis.readBoolean();
    }

    public int readInt() throws IOException {
        bytesRead += 4;
        return dis.readInt();
    }

    public byte readByte() throws IOException {
        bytesRead += 1;
        return dis.readByte();
    }

    public float readFloat() throws IOException {
        bytesRead += 4;
        return dis.readFloat();
    }

    /**
     * Read a TrackableObject reference by ID and look it up in the Tracker.
     * @param type the TrackableObjectType for lookup
     * @return the object from the Tracker, or null if not found or ID is -1
     */
    @SuppressWarnings("unchecked")
    public <T extends TrackableObject> T readObject(TrackableObjectType<T> type) throws IOException {
        int id = dis.readInt();
        bytesRead += 4;
        if (id == -1) {
            return null;
        }
        if (tracker == null) {
            NetworkDebugLogger.warn("[NetworkDeserializer] Tracker is null, cannot resolve object ID %d", id);
            return null;
        }
        T obj = tracker.getObj(type, id);
        if (obj == null) {
            // Object not yet in tracker - this can happen if it's a new object
            // that will be created from a full state sync
            NetworkDebugLogger.warn("[NetworkDeserializer] Object not found in Tracker: type=%s, id=%d",
                    getTypeName(type), id);
        }
        return obj;
    }

    /**
     * Read a CardView reference by ID.
     */
    public CardView readCardView() throws IOException {
        return readObject(TrackableTypes.CardViewType);
    }

    /**
     * Read a PlayerView reference by ID.
     */
    public PlayerView readPlayerView() throws IOException {
        return readObject(TrackableTypes.PlayerViewType);
    }

    /**
     * Read a collection of TrackableObjects by reading size and IDs,
     * then looking up each object in the Tracker.
     */
    @SuppressWarnings("unchecked")
    public <T extends TrackableObject> TrackableCollection<T> readCollection(
            TrackableObjectType<T> type, TrackableCollection<T> oldValue) throws IOException {
        int size = dis.readInt();
        bytesRead += 4;
        if (size == -1) {
            return null;
        }
        TrackableCollection<T> collection = new TrackableCollection<>();
        int foundCount = 0;
        int notFoundCount = 0;
        for (int i = 0; i < size; i++) {
            int id = dis.readInt();
            bytesRead += 4;
            if (id == -1) {
                collection.add(null);
            } else if (tracker != null) {
                T obj = tracker.getObj(type, id);
                if (obj != null) {
                    collection.add(obj);
                    foundCount++;
                } else {
                    // Object not found - might be new, check oldValue
                    boolean foundInOld = false;
                    if (oldValue != null) {
                        for (T old : oldValue) {
                            if (old != null && old.getId() == id) {
                                collection.add(old);
                                foundInOld = true;
                                foundCount++;
                                break;
                            }
                        }
                    }
                    if (!foundInOld) {
                        notFoundCount++;
                        NetworkDebugLogger.warn("[NetworkDeserializer] Collection lookup failed: type=%s, id=%d - NOT FOUND in tracker or oldValue",
                                getTypeName(type), id);
                    }
                }
            }
        }
        // Always log collection stats for debugging
        NetworkDebugLogger.trace("[NetworkDeserializer] Collection read: type=%s, size=%d, found=%d, notFound=%d",
                getTypeName(type), size, foundCount, notFoundCount);
        if (notFoundCount > 0) {
            NetworkDebugLogger.warn("[NetworkDeserializer] Collection has %d missing objects!", notFoundCount);
        }
        return collection;
    }

    /**
     * Read a CardView collection.
     */
    public TrackableCollection<CardView> readCardViewCollection(TrackableCollection<CardView> oldValue) throws IOException {
        return readCollection(TrackableTypes.CardViewType, oldValue);
    }

    /**
     * Read a PlayerView collection.
     */
    public TrackableCollection<PlayerView> readPlayerViewCollection(TrackableCollection<PlayerView> oldValue) throws IOException {
        return readCollection(TrackableTypes.PlayerViewType, oldValue);
    }

    /**
     * Get the Tracker for object lookups.
     */
    public Tracker getTracker() {
        return tracker;
    }

    /**
     * Get a human-readable name for a TrackableObjectType.
     * Anonymous classes return empty string for getSimpleName(), so we map known types.
     */
    private static <T extends TrackableObject> String getTypeName(TrackableObjectType<T> type) {
        if (type == TrackableTypes.CardViewType) {
            return "CardView";
        } else if (type == TrackableTypes.PlayerViewType) {
            return "PlayerView";
        } else if (type == TrackableTypes.StackItemViewType) {
            return "StackItemView";
        } else if (type == TrackableTypes.CombatViewType) {
            return "CombatView";
        } else if (type == TrackableTypes.GameEntityViewType) {
            return "GameEntityView";
        } else if (type == TrackableTypes.CardStateViewType) {
            return "CardStateView";
        }
        // Fallback to class name (will be empty for anonymous classes, but at least we tried)
        String simpleName = type.getClass().getSimpleName();
        return simpleName.isEmpty() ? "UnknownType" : simpleName;
    }
}
