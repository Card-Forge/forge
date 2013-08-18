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

//reads and writeDeck Deck objects
/**
 * <p>
 * DeckManager class.
 * </p>
 *
 * @param <T> the generic type
 * @author Forge
 * @version $Id: DeckManager.java 13590 2012-01-27 20:46:27Z Max mtg $
 */
public class StorageBase<T> implements IStorage<T> {
    protected final Map<String, T> map;


    public StorageBase(final IItemReader<T> io) {
        this.map = io.readAll();
    }

    public StorageBase(final Map<String, T> inMap) {
        this.map = inMap;
    }    

    @Override
    public T get(final String name) {
        return this.map.get(name);
    }
    

    @Override
    public final Collection<String> getNames() {
        return new ArrayList<String>(this.map.keySet());
    }

    @Override
    public Iterator<T> iterator() {
        return this.map.values().iterator();
    }

    @Override
    public boolean contains(String name) {
        return name == null ? false : this.map.containsKey(name);
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public T find(Predicate<T> condition) {
        return Iterables.tryFind(map.values(), condition).orNull();
    }

    @Override
    public void add(T deck) {
        throw new UnsupportedOperationException("This is a read-only storage");
        
    }

    @Override
    public void delete(String deckName) {
        throw new UnsupportedOperationException("This is a read-only storage");
        
    }
}
