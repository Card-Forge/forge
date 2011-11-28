/*
 * The files in the directory "net/slightlymagic/braids" and in all subdirectories of it (the "Files") are
 * Copyright 2011 Braids Cabal-Conjurer. They are available under either Forge's
 * main license (the GNU Public License; see LICENSE.txt in Forge's top directory)
 * or under the Apache License, as explained below.
 *
 * The Files are additionally licensed under the Apache License, Version 2.0 (the
 * "Apache License"); you may not use the files in this directory except in
 * compliance with one of its two licenses.  You may obtain a copy of the Apache
 * License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions and
 * limitations under the Apache License.
 *
 */
package net.slightlymagic.braids.util;

import java.util.Iterator;

/**
 * Acts as both immutable Iterator and Iterable; remove method always throws
 * exception.
 * 
 * @param <T>
 *            the generic type
 */
public class ImmutableIterableFrom<T> implements Iterable<T>, Iterator<T> {
    private Iterator<T> iterator;

    /**
     * Wrap an iterable so that it cannot be changed via the remove method.
     * 
     * @param iterable
     *            the iterable to wrap
     */
    public ImmutableIterableFrom(final Iterable<T> iterable) {
        this.iterator = iterable.iterator();
    }

    /**
     * Wrap an iterator so that its container cannot be changed via the remove
     * method.
     * 
     * @param iterator
     *            the iterator to wrap
     */
    public ImmutableIterableFrom(final Iterator<T> iterator) {
        this.iterator = iterator;
    }

    /**
     * This class acts as both an Iterable and an Iterator.
     * 
     * @return the iterator
     */
    public final Iterator<T> iterator() {
        return this;
    }

    /**
     * Returns hasNext from the wrapped [object's] iterator.
     * 
     * @return true, if successful
     */
    public final boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * Returns next from the wrapped [object's] iterator.
     * 
     * @return the t
     */
    public final T next() {
        return iterator.next();
    }

    /**
     * Never succeeeds.
     * 
     */
    public final void remove() {
        throw new UnsupportedOperationException();
    }
}
