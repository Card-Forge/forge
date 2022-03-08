package forge.gamemodes.net;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import io.netty.handler.codec.serialization.ClassResolver;

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
            ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();
            Class localClass;
            try {
                localClass = Class.forName(resultClassDescriptor.getName());
            } catch (ClassNotFoundException e) {
                System.err.println("[Class Not Found Exception]\nNo local class for " + resultClassDescriptor.getName());
                return resultClassDescriptor;
            }
            ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookupAny(localClass);
            if (localClassDescriptor != null && type == 1) {
                resultClassDescriptor = localClassDescriptor; // Use local class descriptor for deserialization by default
            }
            return resultClassDescriptor;
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
