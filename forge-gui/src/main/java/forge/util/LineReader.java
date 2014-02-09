/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
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

/** 
 * TODO: Write javadoc for this type.
 *
 */

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Represents the lines found in an {@link InputStream}. The lines are read one
 * at a time using {@link BufferedReader#readLine()} and may be streamed through
 * an iterator or returned all at once.
 * 
 * <p>
 * This class does not handle any concurrency issues.
 * 
 * <p>
 * The stream is closed automatically when the for loop is done :)
 * 
 * <pre>
 * {@code
 * for(String line : new LineReader(stream))
 *      // ...
 * }
 * </pre>
 * 
 * <p>
 * An {@link IllegalStateException} will be thrown if any {@link IOException}s
 * occur when reading or closing the stream.
 * 
 * @author Torleif Berger
 * http://creativecommons.org/licenses/by/3.0/
 * @see http://www.geekality.net/?p=1614
 */
public class LineReader implements Iterable<String>, Closeable {
    private final BufferedReader reader;

    /**
     * Instantiates a new line reader.
     *
     * @param stream the stream
     */
    public LineReader(final InputStream stream) {
        this(stream, null);
    }

    /**
     * Instantiates a new line reader.
     *
     * @param stream the stream
     * @param charset the charset
     */
    public LineReader(final InputStream stream, final Charset charset) {
        this.reader = new BufferedReader(new InputStreamReader(stream, charset));
    }

    /**
     * Closes the underlying stream.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    /**
     * Makes sure the underlying stream is closed.
     *
     * @throws Throwable the throwable
     */
    @Override
    protected void finalize() throws Throwable {
        this.close();
    }

    /**
     * Returns an iterator over the lines remaining to be read.
     * 
     * <p>
     * The underlying stream is closed automatically once
     *
     * @return This iterator.
     * {@link Iterator#hasNext()} returns false. This means that the stream
     * should be closed after using a for loop.
     */
    @Override
    public Iterator<String> iterator() {
        return new LineIterator();
    }

    /**
     * Returns all lines remaining to be read and closes the stream.
     * 
     * @return The lines read from the stream.
     */
    public Collection<String> readLines() {
        final Collection<String> lines = new ArrayList<String>();
        for (final String line : this) {
            lines.add(line);
        }
        return lines;
    }

    private class LineIterator implements Iterator<String> {
        private String nextLine;

        public String bufferNext() {
            try {
                return this.nextLine = LineReader.this.reader.readLine();
            } catch (final IOException e) {
                throw new IllegalStateException("I/O error while reading stream.", e);
            }
        }

        @Override
        public boolean hasNext() {
            final boolean hasNext = (this.nextLine != null) || (this.bufferNext() != null);

            if (!hasNext) {
                try {
                    LineReader.this.reader.close();
                } catch (final IOException e) {
                    throw new IllegalStateException("I/O error when closing stream.", e);
                }
            }

            return hasNext;
        }

        @Override
        public String next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }

            final String result = this.nextLine;
            this.nextLine = null;
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
