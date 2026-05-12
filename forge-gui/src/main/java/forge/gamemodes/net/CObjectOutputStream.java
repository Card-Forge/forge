package forge.gamemodes.net;

import forge.trackable.Tracker;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

/**
 * {@link ObjectOutputStream} subclass used by {@link CompatibleObjectEncoder}
 * for the payload of every network message. Two differences from the JDK
 * default:
 *
 * <ul>
 *   <li>Writes a <b>thin class descriptor</b>: a one-byte type tag plus the
 *       UTF class name, instead of the full {@code ObjectStreamClass} block.
 *       The receiver looks up its own local descriptor by name, so the
 *       sender's {@code serialVersionUID} and field layout never travel on
 *       the wire.</li>
 *   <li>When replacement is enabled (per-message, by the encoder), delegates
 *       {@code replaceObject} to {@link TrackableSerializer#replace} with the
 *       supplied Tracker, turning tracked CardView/PlayerView references into
 *       compact {@link TrackableSerializer.IdRef} markers.</li>
 * </ul>
 */
public class CObjectOutputStream extends ObjectOutputStream {
    static final int TYPE_THIN_DESCRIPTOR = 1;

    private final Tracker tracker;
    private final int consumerId;

    CObjectOutputStream(OutputStream out, boolean replaceTrackables, Tracker tracker, int consumerId) throws IOException {
        super(out);
        this.tracker = tracker;
        this.consumerId = consumerId;
        if (replaceTrackables) {
            enableReplaceObject(true);
        }
    }

    @Override
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        write(TYPE_THIN_DESCRIPTOR);
        writeUTF(desc.getName());
    }

    @Override
    protected Object replaceObject(Object obj) throws IOException {
        return TrackableSerializer.replace(obj, tracker, consumerId, false);
    }
}
