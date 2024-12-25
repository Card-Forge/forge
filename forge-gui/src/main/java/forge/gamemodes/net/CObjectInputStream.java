package forge.gamemodes.net;

import io.netty.handler.codec.serialization.ClassResolver;

import java.io.*;

public class CObjectInputStream extends ObjectInputStream {
    private final ClassResolver classResolver;

    CObjectInputStream(InputStream in, ClassResolver classResolver) throws IOException {
        super(in);
        this.classResolver = classResolver;
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
}
