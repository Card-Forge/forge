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

/**
 * This exception indicates the particular method (or part of a method) being
 * called has not been implemented; getting this exception is generally
 * considered a programming error.
 * 
 * Throwing this exception does not necessarily mean the method will be
 * implemented at any point in the future.
 */
public class NotImplementedError extends RuntimeException {

    private static final long serialVersionUID = -6714022569997781370L;

    /**
     * No-arg constructor; this usually means the entire method or block from
     * which it is thrown has not been implemented.
     */
    public NotImplementedError() {
        super();
    }

    /**
     * Indicates what has not been implemented.
     * 
     * @param message
     *            indicates what exactly has not been implemented. May include
     *            information about future plans to implement the described
     *            section of code.
     */
    public NotImplementedError(final String message) {
        super(message);
    }

    /**
     * Like the no-arg constructor, but with a cause parameter.
     * 
     * @param cause
     *            the exception that caused this one to be thrown
     * 
     * @see #NotImplementedError()
     */
    public NotImplementedError(final Throwable cause) {
        super(cause);
    }

    /**
     * Like the String constructor, but with a cause parameter.
     * 
     * @param message
     *            indicates what exactly has not been implemented. May include
     *            information about future plans to implement the described
     *            section of code.
     * 
     * @param cause
     *            the exception that caused this one to be thrown
     * 
     * @see #NotImplementedError(String)
     */
    public NotImplementedError(final String message, final Throwable cause) {
        super(message, cause);
    }
}
