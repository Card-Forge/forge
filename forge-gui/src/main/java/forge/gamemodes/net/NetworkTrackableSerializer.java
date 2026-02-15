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
    private int bytesWritten = 0;

    public NetworkTrackableSerializer(DataOutputStream dos) {
        this.dos = dos;
    }

    /**
     * Get the number of bytes written so far.
     * Used for debugging serialization issues.
     */
    public int getBytesWritten() {
        return bytesWritten;
    }

    public void write(String value) throws IOException {
        if (value == null) {
            dos.writeInt(-1);
            bytesWritten += 4;
        } else {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(bytes.length);
            dos.write(bytes);
            bytesWritten += 4 + bytes.length;
        }
    }

    public void write(boolean value) throws IOException {
        dos.writeBoolean(value);
        bytesWritten += 1;
    }

    public void write(int value) throws IOException {
        dos.writeInt(value);
        bytesWritten += 4;
    }

    public void write(byte value) throws IOException {
        dos.writeByte(value);
        bytesWritten += 1;
    }

    public void write(float value) throws IOException {
        dos.writeFloat(value);
        bytesWritten += 4;
    }

    /**
     * Write a collection of TrackableObjects as a size followed by their IDs.
     * This is the key optimization - we don't serialize the full objects,
     * just their IDs for lookup on the client side.
     */
    public void write(TrackableCollection<? extends TrackableObject> collection) throws IOException {
        if (collection == null) {
            dos.writeInt(-1);
            bytesWritten += 4;
        } else {
            dos.writeInt(collection.size());
            bytesWritten += 4;
            for (TrackableObject obj : collection) {
                dos.writeInt(obj == null ? -1 : obj.getId());
                bytesWritten += 4;
            }
        }
    }

}
