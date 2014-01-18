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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.util.IItemReader;

/**
 * <p>
 * StorageBase class.
 * </p>
 *
 * @param <T> the generic type
 * @author Forge
 * @version $Id: StorageBase.java 13590 2012-01-27 20:46:27Z Max mtg $
 */
public class StorageBase<T> implements IStorage<T> {
    protected final Map<String, T> map;
    private final String name;

    public StorageBase(final String name, final IItemReader<T> io) {
        this.name = name;
        this.map = io.readAll();
    }

    public StorageBase(final String name, final Map<String, T> inMap) {
        this.name = name;
        this.map = inMap;
    }

    @Override
    public T get(final String name) {
        return this.map.get(name);
    }

    @Override
    public final Collection<String> getItemNames() {
        return new ArrayList<String>(this.map.keySet());
    }

    @Override
    public final Iterator<T> iterator() {
        final IStorage<IStorage<T>> folders = getFolders();
        if (folders == null) { //if no folders, just return map iterator
            return this.map.values().iterator();
        }
        //otherwise return iterator for list containing folder items followed by map's items
        ArrayList<T> items = new ArrayList<T>();
        for (IStorage<T> folder : folders) {
            for (T item : folder) {
                items.add(item);
            }
        }
        items.addAll(this.map.values());
        return items.iterator();
    }

    @Override
    public boolean contains(String name) {
        return name == null ? false : this.map.containsKey(name);
    }

    @Override
    public int size() {
        int size = this.map.size();
        if (this.getFolders() != null) {
            size += this.getFolders().size();
        }
        return size;
    }

    @Override
    public T find(Predicate<T> condition) {
        return Iterables.tryFind(this, condition).orNull();
    }

    @Override
    public void add(T item) {
        throw new UnsupportedOperationException("This is a read-only storage");
    }

    @Override
    public void delete(String itemName) {
        throw new UnsupportedOperationException("This is a read-only storage");
    }

    @Override
    public IStorage<IStorage<T>> getFolders() {
        return null; //no nested folders unless getFolders() overridden in a derived class
    }

    /* (non-Javadoc)
     * @see forge.util.IHasName#getName()
     */
    @Override
    public String getName() {
        return name;
    }
}
