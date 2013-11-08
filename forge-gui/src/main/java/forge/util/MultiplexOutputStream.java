/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.util;

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
    @Override
    public final void write(final int b) throws IOException {
        for (int i = 0; i < streams.length; i++) {
            streams[i].write(b);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void write(final byte[] b, final int off, final int len) throws IOException {
        for (int i = 0; i < streams.length; i++) {
            streams[i].write(b, off, len);
        }
    }
}
