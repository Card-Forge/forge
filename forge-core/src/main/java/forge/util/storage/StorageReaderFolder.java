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

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

/**
 * This class treats every file in the given folder as a source for a named
 * object. The descendant should implement read method to deserialize a single
 * item. So that readAll will return a map of Name => Object as read from disk
 *
 * @param <T> the generic type
 */
public abstract class StorageReaderFolder<T> extends StorageReaderBase<T> {
    /**
     * @return the directory
     */
    public File getDirectory() {
        return directory;
    }

    protected final File directory;

    /**
     * Instantiates a new storage reader folder.
     *
     * @param itemDir0 the item dir0
     */
    public StorageReaderFolder(final File itemDir0, Function<? super T, String> keySelector0) {
        super(keySelector0);

        this.directory = itemDir0;

        if (this.directory == null) {
            throw new IllegalArgumentException("No directory specified");
        }
        try {
            if (this.directory.isFile()) {
                throw new IOException("Not a directory");
            } else {
                this.directory.mkdirs();
                if (!this.directory.isDirectory()) {
                    throw new IOException("Directory can't be created");
                }
            }
        } catch (final IOException ex) {
            throw new RuntimeException("StorageReaderFolder.ctor() error, " + ex.getMessage());
        }
    }

    public final List<String> objectsThatFailedToLoad = new ArrayList<String>();

    /* (non-Javadoc)
     * @see forge.util.IItemReader#readAll()
     */
    @Override
    public Map<String, T> readAll() {
        final Map<String, T> result = new TreeMap<String, T>();

        final File[] files = this.directory.listFiles(this.getFileFilter());
        for (final File file : files) {
            try {
                final T newDeck = this.read(file);
                if (null == newDeck) {
                    final String msg = "An object stored in " + file.getPath() + " failed to load.\nPlease submit this as a bug with the mentioned file/directory attached.";
                    throw new RuntimeException(msg);
                }
                String newKey = keySelector.apply(newDeck);
                if (result.containsKey(newKey)) {
                    System.err.println("StorageReader: Overwriting an object with key " + newKey);
                }
                result.put(newKey, newDeck);
            } catch (final NoSuchElementException ex) {
                final String message = String.format("%s failed to load because ---- %s", file.getName(), ex.getMessage());
                objectsThatFailedToLoad.add(message);
            }
        }
        return result;
    }

    /**
     * Read the object from file.
     *
     * @param file the file
     * @return the object deserialized by inherited class
     */
    protected abstract T read(File file);

    /**
     * TODO: Write javadoc for this method.
     * 
     * @return FilenameFilter to pick only relevant objects for deserialization
     */
    protected abstract FilenameFilter getFileFilter();

    @Override
    public String getItemKey(T item) {
        return keySelector.apply(item);
    }

    // methods handling nested folders are provided. It's up to consumer whether to use these or not.
    @Override
    public Iterable<File> getSubFolders() {
        File[] list = this.directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && !file.isHidden();
            }
        });
        return Arrays.asList(list);
    }
}
