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

import java.util.Collection;

import com.google.common.base.Predicate;

import forge.util.IHasName;

/**
 * TODO: Write javadoc for this type.
 *
 * @param <T> the generic type
 */
public interface IStorage<T> extends Iterable<T>, IHasName {
    T get(final String name);
    T find(final Predicate<T> condition);
    Collection<String> getItemNames();
    boolean contains(final String name);
    int size();
    void add(final T deck);
    void delete(final String deckName);
    IStorage<IStorage<T>> getFolders();
    IStorage<T> tryGetFolder(String path);
    IStorage<T> getFolderOrCreate(String path);
}