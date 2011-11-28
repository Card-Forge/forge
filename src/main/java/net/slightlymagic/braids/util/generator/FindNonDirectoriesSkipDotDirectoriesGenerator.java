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

import java.io.File;

import com.google.code.jyield.Generator;
import com.google.code.jyield.Yieldable;

/**
 * This is a generator over all of the non-directories residing in a given
 * starting directory and all subdirectories of it that do NOT start with a dot;
 * this prevents the code from descending into .svn directories.
 * 
 * For documentation on Java-Yield and its generators, see
 * {@link com.google.code.jyield.Generator}
 */
public class FindNonDirectoriesSkipDotDirectoriesGenerator implements Generator<File> {
    private File startDir;

    /**
     * Create a generator at a given starting directory.
     * 
     * One can invoke this generator more than once by calling its generate
     * method.
     * 
     * @param startDir
     *            the directory to start in; we ignore this directory's name, so
     *            if it starts with a dot, we treat it as if it didn't.
     */
    public FindNonDirectoriesSkipDotDirectoriesGenerator(final File startDir) {
        this.startDir = startDir;
    }

    /**
     * Standard generate method.
     * 
     * <p>
     * Yields results to the given Yieldable. Convert Generator instances to
     * Iterables with YieldUtils.toIterable.
     * </p>
     * 
     * See
     *
     * @param yy the yy
     * {@link com.google.code.jyield.YieldUtils#toIterable(com.google.code.jyield.Generator)}
     */
    public final void generate(final Yieldable<File> yy) {
        String[] list = startDir.list();

        for (String filename : list) {
            File entry = new File(startDir, filename);

            if (entry.isDirectory()) {
                if (!filename.startsWith(".")) {
                    FindNonDirectoriesSkipDotDirectoriesGenerator child =
                            new FindNonDirectoriesSkipDotDirectoriesGenerator(
                            entry);
                    child.generate(yy);
                    child = null;
                }
                // else do nothing, because it's a dot directory
            } else {
                // Use this instead of a return statement.
                yy.yield(entry);
            }
        }
    }
}
