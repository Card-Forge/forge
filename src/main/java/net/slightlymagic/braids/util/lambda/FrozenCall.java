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

import static net.slightlymagic.braids.util.UtilFunctions.checkNotNull;
import net.slightlymagic.braids.util.UtilFunctions;

/**
 * This embodies a promise to invoke a certain method at a later time; the
 * FrozenCall remembers the arguments to use and the return type.
 * 
 * @param <T>
 *            the return type of apply
 * 
 * @see Thunk
 */
public class FrozenCall<T> implements Thunk<T> {
    private Lambda<T> proc;
    private Object[] args;

    /**
     * Instantiates a new frozen call.
     * 
     * @param proc
     *            the proc
     * @param args
     *            the args
     */
    public FrozenCall(final Lambda<T> proc, final Object[] args) {
        checkNotNull("proc", proc);
        checkNotNull("args", args);

        this.proc = proc;
        this.args = args;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.slightlymagic.braids.util.lambda.Thunk#apply()
     */
    /**
     * Apply.
     *
     * @return <T>
     */
    public final T apply() {
        return proc.apply(args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /**
     * Equals.
     *
     * @param obj Object
     * @return boolean
     */
    @Override
    public final boolean equals(final Object obj) {
        FrozenCall<T> that = UtilFunctions.checkNullOrNotInstance(this, obj);
        if (that == null) {
            return false;
        } else if (!this.proc.equals(that.proc)) {
            return false;
        } else if (this.args.length != that.args.length) {
            return false;
        }

        for (int i = 0; i < args.length; i++) {
            if (this.args[i] == null && that.args[i] != null) {
                return false;
            } else if (!this.args[i].equals(that.args[i])) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return 41 * (41 + this.args.hashCode());
    }
}
