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

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;

import forge.util.FileUtil;

/**
 * This class treats every line of a given file as a source for a named object.
 * 
 * @param <T>
 *            the generic type
 */
public abstract class StorageReaderFile<T> extends StorageReaderBase<T> {
    protected final File file;

    public StorageReaderFile(final String pathname, final Function<? super T, String> keySelector0) {
        this(new File(pathname), keySelector0);
    }

    public StorageReaderFile(final File file0, final Function<? super T, String> keySelector0) {
        super(keySelector0);
        file = file0;
    }

    @Override
    public String getFullPath() {
        return file.getPath();
    }

    @Override
    public Map<String, T> readAll() {
        final Map<String, T> result = new TreeMap<>();

        int idx = 0;
        for (String line : FileUtil.readFile(file)) {
            line = line.trim();
            if (line.isEmpty()) {
                continue; //ignore blank or whitespace lines
            }  

            if (!lineContainsObject(line)) {
                continue;
            }

            T item = read(line, idx);
            if (item == null) {
                continue;
            }

            idx++;
            String newKey = keySelector.apply(item);
            if (result.containsKey(newKey)) {
                System.err.println("StorageReaderFile: Overwriting an object with key " + newKey);
            }
            result.put(newKey, item);
        }

        return result;
    }

    protected abstract T read(String line, int idx);

    protected boolean lineContainsObject(final String line) {
        return !StringUtils.isBlank(line) && !line.trim().startsWith("#");
    }

    @Override
    public String getItemKey(final T item) {
        return keySelector.apply(item);
    }

    protected void alertInvalidLine(String line, String message) {
        System.err.println(message);
        System.err.println(line);
        System.err.println(file.getPath());
        System.err.println();
    }
}
