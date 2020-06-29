package forge.net;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

public class CObjectOutputStream extends ObjectOutputStream {
    static final int TYPE_THIN_DESCRIPTOR = 1;

    CObjectOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    @Override
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        //we only pass this and the decoder will lookup in the stream (faster method both mobile and desktop)
        write(TYPE_THIN_DESCRIPTOR);
        writeUTF(desc.getName());
    }
}
