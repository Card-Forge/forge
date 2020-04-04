package forge.net;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;

public class CObjectOutputStream extends ObjectOutputStream {
    static final int TYPE_FAT_DESCRIPTOR = 0;
    static final int TYPE_THIN_DESCRIPTOR = 1;

    CObjectOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    protected void writeStreamHeader() throws IOException {
        this.writeByte(5);
    }

    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        Class<?> clazz = desc.forClass();
        if (!clazz.isPrimitive() && !clazz.isArray() && !clazz.isInterface() && desc.getSerialVersionUID() != 0L) {
            this.write(1);
            this.writeUTF(desc.getName());
        } else {
            this.write(0);
            super.writeClassDescriptor(desc);
        }

    }
}
