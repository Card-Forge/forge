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
import java.util.Map;
import java.util.TreeMap;

/**
 * This class treats every line of a given file as a source for a named object.
 * 
 * @param <T>
 *            the generic type
 */
public abstract class StorageReaderFile<T> extends StorageReaderBase<T> {
    private final File file;

    /**
     * Instantiates a new storage reader file.
     *
     * @param pathname the pathname
     * @param keySelector0 the key selector0
     */
    public StorageReaderFile(final String pathname, final Function<? super T, String> keySelector0) {
        this(new File(pathname), keySelector0);
    }

    /**
     * Instantiates a new storage reader file.
     *
     * @param file0 the file0
     * @param keySelector0 the key selector0
     */
    public StorageReaderFile(final File file0, final Function<? super T, String> keySelector0) {
        super(keySelector0);
        this.file = file0;
    }

    /* (non-Javadoc)
     * @see forge.util.IItemReader#readAll()
     */
    @Override
    public Map<String, T> readAll() {
        final Map<String, T> result = new TreeMap<String, T>();

        int idx = 0;
        for (final String s : FileUtil.readFile(this.file)) {
            if (!this.lineContainsObject(s)) {
                continue;
            }

            final T item = this.read(s, idx);
            if (null == item) {
                final String msg = "An object stored in " + this.file.getPath() + " failed to load.\nPlease submit this as a bug with the mentioned file attached.";
                throw new RuntimeException(msg);
            }

            idx++;
            String newKey = keySelector.apply(item);
            if (result.containsKey(newKey)) {
                System.err.println("StorageReader: Overwriting an object with key " + newKey);
            }
            result.put(newKey, item);
        }

        return result;
    }

    /**
     * TODO: Write javadoc for this method.
     * 
     * @param line
     *            the line
     * @return the t
     */
    protected abstract T read(String line, int idx);

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
