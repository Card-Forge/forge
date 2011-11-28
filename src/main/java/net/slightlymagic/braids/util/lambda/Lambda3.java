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
package net.slightlymagic.braids.util.lambda;

/**
 * The Class Lambda3.
 * 
 * @param <R>
 *            the generic type
 * @param <A1>
 *            the generic type
 * @param <A2>
 *            the generic type
 * @param <A3>
 *            the generic type
 */
public abstract class Lambda3<R, A1, A2, A3> implements Lambda<R> {

    /**
     * Apply.
     * 
     * @param arg1
     *            the arg1
     * @param arg2
     *            the arg2
     * @param arg3
     *            the arg3
     * @return the r
     */
    public abstract R apply(A1 arg1, A2 arg2, A3 arg3);

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.slightlymagic.braids.util.lambda.Lambda#apply(java.lang.Object[])
     */

    // TODO @Override
    /**
     * Apply.
     *
     * @param args Object[]
     * @return R
     */
    @SuppressWarnings("unchecked")
    public final R apply(final Object[] args) {
        return apply((A1) args[0], (A2) args[1], (A3) args[2]);
    }

}
