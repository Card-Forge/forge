package forge.gamemodes.net;

import forge.trackable.Tracker;
import io.netty.handler.codec.serialization.ClassResolver;

import java.io.*;

public class CObjectInputStream extends ObjectInputStream {
    private final ClassResolver classResolver;
    private final Tracker tracker;

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
        if (type < 0) {
            throw new EOFException();
        } else {
            if (type == 1)
                return ObjectStreamClass.lookupAny(classResolver.resolve(readUTF()));
            else
                return super.readClassDescriptor();
        }
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
