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
package forge.util.storage;

import com.google.common.base.Function;
import forge.util.IItemSerializer;
import forge.util.TextUtil;

import java.io.File;

/**
 * <p>
 * StorageImmediatelySerialized class.
 * </p>
 *
 * @param <T> the generic type
 * @author Forge
 * @version $Id: StorageImmediatelySerialized.java 24272 2014-01-15 05:07:59Z drdev $
 */
public class StorageImmediatelySerialized<T> extends StorageBase<T> {
    private final IItemSerializer<T> serializer;
    private final IStorage<IStorage<T>> subfolders;

    private final Function<File, IStorage<T>> nestedFactory = new Function<File, IStorage<T>>() {
        @Override
        public IStorage<T> apply(File file) {
            return new StorageImmediatelySerialized<T>(file.getName(), (IItemSerializer<T>) serializer.getReaderForFolder(file), true);
        }
    };

    /**
     * <p>
     * Constructor for StorageImmediatelySerialized.
     * </p>
     *
     * @param io the io
     */
    public StorageImmediatelySerialized(String name, final IItemSerializer<T> io) {
        this(name, io, false);
    }

    public StorageImmediatelySerialized(String name, final IItemSerializer<T> io, boolean withSubFolders) {
        super(name, io);
        this.serializer = io;
        subfolders = withSubFolders ? new StorageNestedFolders<T>(io.getDirectory(), io.getSubFolders(), nestedFactory) : null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.util.storage.StorageBase#add(T)
     */
    @Override
    public final void add(final T item) {
        String name = serializer.getItemKey(item);
        this.map.put(name, item);
        this.serializer.save(item);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.util.storage.StorageBase#delete(java.lang.String)
     */
    @Override
    public final void delete(final String itemName) {
        try {
            this.serializer.erase(this.map.remove(itemName));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see forge.util.storage.StorageBase#getFolders()
     */
    @Override
    public IStorage<IStorage<T>> getFolders() {
        return subfolders == null ? super.getFolders() : subfolders;
    }
    
    @Override
    public IStorage<T> tryGetFolder(String path) {
        String[] parts = TextUtil.split(path, '/', 2);
        switch( parts.length ) {
            case 0: return this;
            case 1: return parts[0].equals(".") ? this : getFolders().get(parts[0]);
            case 2:
                IStorage<T> subFolder = getFolders().get(parts[0]);
                return subFolder == null ? null : subFolder.tryGetFolder(parts[1]);
        }
     // should not reach this unless split is broken
        throw new IllegalArgumentException(path); 
    }

    @Override
    public IStorage<T> getFolderOrCreate(String path) {
        String[] parts = TextUtil.split(path, '/', 2);
        switch( parts.length ) {
            case 0: return this;
            case 1: return parts[0].equals(".") ? this : getOrCreateSubfolder(parts[0]);
            case 2: return getOrCreateSubfolder(parts[0]).getFolderOrCreate(parts[1]);
        }
     // should not reach this unless split is broken
        throw new IllegalArgumentException(path); 
    }
    
    private IStorage<T> getOrCreateSubfolder(String name) {
        // Have to filter name for incorrect symbols 
        IStorage<T> storage = getFolders().get(name);
        if( null == storage ) {
            storage = new StorageImmediatelySerialized<>(name, serializer);
            subfolders.add(storage);
        }
        return storage;
    }
}
