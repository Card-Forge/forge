package arcane.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An OutputStream that writes to multiple other OutputStreams.
 * 
 * @author Forge
 * @version $Id$
 */
public class MultiplexOutputStream extends OutputStream {
    private final OutputStream[] streams;

    /**
     * <p>
     * Constructor for MultiplexOutputStream.
     * </p>
     * 
     * @param streams
     *            a {@link java.io.OutputStream} object.
     */
    public MultiplexOutputStream(final OutputStream... streams) {
        super();
        if (streams == null) {
            throw new IllegalArgumentException("streams cannot be null.");
        }
        this.streams = streams;
    }

    /** {@inheritDoc} */
    public final void write(final int b) throws IOException {
        for (int i = 0; i < streams.length; i++) {
            streams[i].write(b);
        }
    }

    /** {@inheritDoc} */
    public final void write(final byte[] b, final int off, final int len) throws IOException {
        for (int i = 0; i < streams.length; i++) {
            streams[i].write(b, off, len);
        }
    }
}
