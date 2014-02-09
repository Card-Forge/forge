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
package forge.util.storage;

import com.google.common.base.Function;
import forge.util.FileUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class treats every line of a given file as a source for a named object.
 * 
 * @param <T>
 *            the generic type
 */
public abstract class StorageReaderFileSections<T> extends StorageReaderBase<T> {
    private final File file;

    public StorageReaderFileSections(final String pathname, final Function<? super T, String> keySelector0) {
        this(new File(pathname), keySelector0);
    }

    public StorageReaderFileSections(final File file0, final Function<? super T, String> keySelector0) {
        super(keySelector0);
        this.file = file0;
    }

    protected Map<String, T> createMap() {
        return new TreeMap<String, T>();
    }

    /* (non-Javadoc)
     * @see forge.util.IItemReader#readAll()
     */
    @Override
    public Map<String, T> readAll() {
        final Map<String, T> result = createMap();

        int idx = 0;
        Iterable<String> file = FileUtil.readFile(this.file);

        List<String> accumulator = new ArrayList<String>();
        String header = null;

        for (final String s : file) {
            if (!this.lineContainsObject(s)) {
                continue;
            }

            if(s.charAt(0) == '[') {
                if( header != null ) {
                    // read previously collected item
                    T item = readItem(header, accumulator, idx);
                    if( item != null ) {
                        result.put(this.keySelector.apply(item), item);
                        idx++;
                    }
                }

                header = StringUtils.strip(s, "[] ");
                accumulator.clear();
            } else
                accumulator.add(s);
        }

        // store the last item
        if ( !accumulator.isEmpty() ) {
            T item = readItem(header, accumulator, idx);
            if( item != null ) {
                String newKey = keySelector.apply(item);
                if( result.containsKey(newKey))
                    System.err.println("StorageReader: Overwriting an object with key " + newKey);

                result.put(newKey, item);
            }
        }
        return result;
    }

    private final T readItem(String header, Iterable<String> accumulator, int idx) {
        final T item = this.read(header, accumulator, idx);
        if (null != item) return item;

        final String msg = "An object stored in " + this.file.getPath() + " failed to load.\nPlease submit this as a bug with the mentioned file attached.";
        throw new RuntimeException(msg);
    }

    /**
     * TODO: Write javadoc for this method.
     * 
     * @param line
     *            the line
     * @return the t
     */
    protected abstract T read(String title, Iterable<String> body, int idx);

    /**
     * Line contains object.
     * 
     * @param line
     *            the line
     * @return true, if successful
     */
    protected boolean lineContainsObject(final String line) {
        return !StringUtils.isBlank(line) && !line.trim().startsWith("#");
    }

    /* (non-Javadoc)
     * @see forge.util.IItemReader#getItemKey(java.lang.Object)
     */
    @Override
    public String getItemKey(final T item) {
        return this.keySelector.apply(item);
    }
}
