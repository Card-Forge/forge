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
package net.slightlymagic.braids.util.generator;

import com.google.code.jyield.Generator;
import com.google.code.jyield.Yieldable;

/**
 * Creates a Generator from an array; generators are a handy substitute for
 * passing around and creating temporary lists, collections, and arrays.
 * 
 * @param <T>
 *            the generic type {@link com.google.code.jyield.Generator}
 */
public class GeneratorFromArray<T> implements Generator<T> {
    private T[] array;

    /**
     * Create a Generator from an array.
     * 
     * @param array
     *            from which to generate items
     */
    public GeneratorFromArray(final T[] array) {
        this.array = array;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.google.code.jyield.Generator#generate(com.google.code.jyield.Yieldable
     * )
     */

    /**
     * Submits all of the array's elements to the yieldable.
     * 
     * @param yy
     *            the yieldable which receives the elements
     */
    @Override
    public final void generate(final Yieldable<T> yy) {
        for (T item : array) {
            yy.yield(item);
        }
    }
}
