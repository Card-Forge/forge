package forge.gamemodes.net;

import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Binary serializer for network delta sync.
 * Unlike TrackableSerializer which writes to a file with delimiters,
 * this class writes directly to a DataOutputStream in a compact binary format.
 *
 * Key difference from Java's ObjectOutputStream:
 * - Object references (CardView, PlayerView, etc.) are written as IDs only (4 bytes)
 * - No full object graph serialization - just primitive values and IDs
 * - Collections are written as size + list of IDs
 */
public class NetworkTrackableSerializer {
    private final DataOutputStream dos;

    public NetworkTrackableSerializer(DataOutputStream dos) {
        this.dos = dos;
    }

    public void write(String value) throws IOException {
        if (value == null) {
            dos.writeInt(-1);
        } else {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }
    }

    public void write(boolean value) throws IOException {
        dos.writeBoolean(value);
    }

    public void write(int value) throws IOException {
        dos.writeInt(value);
    }

    public void write(byte value) throws IOException {
        dos.writeByte(value);
    }

    public void write(long value) throws IOException {
        dos.writeLong(value);
    }

    public void write(float value) throws IOException {
        dos.writeFloat(value);
    }

    public void write(double value) throws IOException {
        dos.writeDouble(value);
    }

    /**
     * Write a collection of TrackableObjects as a size followed by their IDs.
     * This is the key optimization - we don't serialize the full objects,
     * just their IDs for lookup on the client side.
     */
    public void write(TrackableCollection<? extends TrackableObject> collection) throws IOException {
        if (collection == null) {
            dos.writeInt(-1);
        } else {
            dos.writeInt(collection.size());
            for (TrackableObject obj : collection) {
                dos.writeInt(obj == null ? -1 : obj.getId());
            }
        }
    }

    /**
     * Write a single TrackableObject reference as just its ID.
     * The client will look up the object by ID in its Tracker.
     */
    public void writeObjectRef(TrackableObject obj) throws IOException {
        dos.writeInt(obj == null ? -1 : obj.getId());
    }

    /**
     * Get the underlying DataOutputStream for direct access when needed.
     */
    public DataOutputStream getOutputStream() {
        return dos;
    }
}
