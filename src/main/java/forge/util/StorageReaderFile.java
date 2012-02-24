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

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import net.slightlymagic.braids.util.lambda.Lambda1;

import org.apache.commons.lang3.StringUtils;

/**
 * This class treats every line of a given file as a source for a named object.
 *
 * @param <T> the generic type
 */
public abstract class StorageReaderFile<T> implements IItemReader<T> {

    private final File file;
    private final Lambda1<String,T> keySelector;

    /**
     * Instantiates a new storage reader file.
     *
     * @param file0 the file0
     */
    public StorageReaderFile(final String pathname, Lambda1<String,T> keySelector0) {
        this(new File(pathname), keySelector0);
    }

    
    public StorageReaderFile(final File file0, Lambda1<String,T> keySelector0) {
        this.file = file0;
        keySelector = keySelector0;
    }

    @Override
    public Map<String, T> readAll() {
        final Map<String, T> result = new TreeMap<String, T>();
        final ArrayList<String> fData = FileUtil.readFile(this.file);

        for (final String s : fData) {
            if (!this.lineContainsObject(s)) {
                continue;
            }

            final T item = this.read(s);
            if (null == item) {
                final String msg = "An object stored in " + this.file.getPath()
                        + " failed to load.\nPlease submit this as a bug with the mentioned file attached.";
                JOptionPane.showMessageDialog(null, msg);
                continue;
            }

            result.put(keySelector.apply(item), item);
        }

        return result;
    }

    /**
     * TODO: Write javadoc for this method.
     *
     * @param line the line
     * @return the t
     */
    protected abstract T read(String line);

    /**
     * Line contains object.
     *
     * @param line the line
     * @return true, if successful
     */
    protected boolean lineContainsObject(final String line) {
        return !StringUtils.isBlank(line) && !line.trim().startsWith("#");
    }

}
