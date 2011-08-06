
package arcane.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An OutputStream that writes to multiple other OutputStreams.
 */
public class MultiplexOutputStream extends OutputStream {
	private final OutputStream[] streams;

	public MultiplexOutputStream (OutputStream... streams) {
		super();
		if (streams == null) throw new IllegalArgumentException("streams cannot be null.");
		this.streams = streams;
	}

	public void write (int b) throws IOException {
		for (int i = 0; i < streams.length; i++)
			streams[i].write(b);
	}

	public void write (byte[] b, int off, int len) throws IOException {
		for (int i = 0; i < streams.length; i++)
			streams[i].write(b, off, len);
	}
}
