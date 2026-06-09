package forge.gamemodes.net;

import forge.trackable.Tracker;
import io.netty.handler.codec.serialization.ClassResolver;

import java.io.*;

/**
 * {@link ObjectInputStream} subclass used by {@link CompatibleObjectDecoder}
 * for the payload of every network message. Mirrors {@link CObjectOutputStream}:
 *
 * <ul>
 *   <li>Reads the <b>thin class descriptor</b> (one-byte type tag + UTF class
 *       name) the sender wrote, and looks up the local {@code ObjectStreamClass}
 *       via the {@link ClassResolver}. Sender-side {@code serialVersionUID}
 *       and field metadata are not on the wire — both ends must hold matching
 *       class definitions.</li>
 *   <li>When a Tracker is set, delegates {@code resolveObject} to
 *       {@link TrackableSerializer#resolve}, turning incoming
 *       {@link TrackableSerializer.IdRef} markers back into live CardView/
 *       PlayerView instances from the Tracker.</li>
 * </ul>
 */
public class CObjectInputStream extends ObjectInputStream {
    private final ClassResolver classResolver;
    private final Tracker tracker;

    /**
     * Resolution is enabled whenever a tracker is present. The encoder's
     * replacement, by contrast, is gated per-message via the
     * {@code replaceTrackables} flag — the encoder knows which messages
     * carry compressible references; the decoder does not, so it stays
     * ready for any frame.
     */
    CObjectInputStream(InputStream in, ClassResolver classResolver, Tracker tracker) throws IOException {
        super(in);
        this.classResolver = classResolver;
        this.tracker = tracker;
        if (tracker != null) {
            enableResolveObject(true);
        }
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        int type = read();
        if (type < 0)
            throw new EOFException();
        else if (type == CObjectOutputStream.TYPE_THIN_DESCRIPTOR)
            return ObjectStreamClass.lookupAny(classResolver.resolve(readUTF()));
        else
            return super.readClassDescriptor();
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        Class<?> clazz;
        try {
            clazz = classResolver.resolve(desc.getName());
        } catch (ClassNotFoundException ignored) {
            clazz = super.resolveClass(desc);
        }
        return clazz;
    }

    @Override
    protected Object resolveObject(Object obj) throws IOException {
        return TrackableSerializer.resolve(obj, tracker);
    }
}
