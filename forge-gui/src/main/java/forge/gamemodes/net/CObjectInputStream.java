package forge.gamemodes.net;

import forge.trackable.Tracker;
import forge.util.IHasForgeLog;
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
public class CObjectInputStream extends ObjectInputStream implements IHasForgeLog {
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
        WireStreamLimits.applyTo(this);
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        int type = read();
        if (type < 0)
            throw new EOFException();
        else if (type == CObjectOutputStream.TYPE_THIN_DESCRIPTOR) {
            // Checked here because the thin path resolves the class itself, so
            // the JDK's own resolveClass call would come too late. The wide
            // path needs no check — resolveClass below covers it.
            final String name = readUTF();
            WireClassFilter.checkAllowed(name);
            return ObjectStreamClass.lookupAny(classResolver.resolve(name));
        } else {
            return super.readClassDescriptor();
        }
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        WireClassFilter.checkAllowed(desc.getName());
        Class<?> clazz;
        try {
            clazz = classResolver.resolve(desc.getName());
        } catch (ClassNotFoundException ignored) {
            clazz = super.resolveClass(desc);
        }
        return clazz;
    }

    /**
     * A proxy descriptor carries no class name, so it reaches neither method
     * above — the JDK reads the interface list and calls this instead, which is
     * where the best-known chains start. The protocol carries no proxies.
     */
    @Override
    protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
        netLog.error("Rejected dynamic proxy on the wire implementing {} — "
                + "the multiplayer protocol does not carry proxies", String.join(", ", interfaces));
        if (WireClassFilter.isEnforcing()) {
            throw new InvalidClassException("dynamic proxy",
                    "proxy classes are not permitted by the multiplayer class filter");
        }
        return super.resolveProxyClass(interfaces);
    }

    @Override
    protected Object resolveObject(Object obj) throws IOException {
        return TrackableSerializer.resolve(obj, tracker);
    }
}
