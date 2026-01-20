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

    public NetworkTrackableDeserializer(DataInputStream dis, Tracker tracker) {
        this.dis = dis;
        this.tracker = tracker;
    }

    public String readString() throws IOException {
        int length = dis.readInt();
        if (length == -1) {
            return null;
        }
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public boolean readBoolean() throws IOException {
        return dis.readBoolean();
    }

    public int readInt() throws IOException {
        return dis.readInt();
    }

    public byte readByte() throws IOException {
        return dis.readByte();
    }

    public long readLong() throws IOException {
        return dis.readLong();
    }

    public float readFloat() throws IOException {
        return dis.readFloat();
    }

    public double readDouble() throws IOException {
        return dis.readDouble();
    }

    /**
     * Read a TrackableObject reference by ID and look it up in the Tracker.
     * @param type the TrackableObjectType for lookup
     * @return the object from the Tracker, or null if not found or ID is -1
     */
    @SuppressWarnings("unchecked")
    public <T extends TrackableObject> T readObject(TrackableObjectType<T> type) throws IOException {
        int id = dis.readInt();
        if (id == -1) {
            return null;
        }
        if (tracker == null) {
            System.err.println("[NetworkDeserializer] Warning: Tracker is null, cannot resolve object ID " + id);
            return null;
        }
        T obj = tracker.getObj(type, id);
        if (obj == null) {
            // Object not yet in tracker - this can happen if it's a new object
            // that will be created from a full state sync
            System.err.println("[NetworkDeserializer] Warning: Object not found in Tracker: type=" +
                    type.getClass().getSimpleName() + ", id=" + id);
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
        if (size == -1) {
            return null;
        }
        TrackableCollection<T> collection = new TrackableCollection<>();
        int foundCount = 0;
        int notFoundCount = 0;
        for (int i = 0; i < size; i++) {
            int id = dis.readInt();
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
                        System.err.println("[NetworkDeserializer] Collection lookup failed: type=" +
                                type.getClass().getSimpleName() + ", id=" + id + " - NOT FOUND in tracker or oldValue");
                    }
                }
            }
        }
        if (notFoundCount > 0) {
            System.err.println("[NetworkDeserializer] Collection deserialization: size=" + size +
                    ", found=" + foundCount + ", notFound=" + notFoundCount);
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
     * Get the underlying DataInputStream for direct access when needed.
     */
    public DataInputStream getInputStream() {
        return dis;
    }
}
