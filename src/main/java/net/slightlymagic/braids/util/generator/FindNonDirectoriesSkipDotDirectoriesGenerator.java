/** Licensed under both the GPL and the Apache 2.0 License. */
package net.slightlymagic.braids.util.generator;

import com.google.code.jyield.Generator;
import com.google.code.jyield.Yieldable;

import java.io.File;

/**
 * This is a generator over all of the non-directories residing in a given
 * starting directory and all subdirectories of it that do NOT start with a 
 * dot; this prevents the code from descending into .svn directories.
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
	 * @param startDir  the directory to start in; we ignore this directory's
	 * name, so if it starts with a dot, we treat it as if it didn't.
	 */
	public FindNonDirectoriesSkipDotDirectoriesGenerator(File startDir) {
		this.startDir = startDir;
	}

	/**
	 * Standard generate method.
	 * 
	 * <p>Yields results to the given Yieldable.  Convert Generator instances to
	 * Iterables with YieldUtils.toIterable.</p>
	 * 
	 * See {@link com.google.code.jyield.YieldUtils#toIterable(com.google.code.jyield.Generator)}
	 */
	public void generate(Yieldable<File> yy) {
		String[] list = startDir.list();
		
		for (String filename : list) {
			File entry = new File(startDir, filename);
			
			if (entry.isDirectory()) {
				if (!filename.startsWith(".")) {
					FindNonDirectoriesSkipDotDirectoriesGenerator child = new FindNonDirectoriesSkipDotDirectoriesGenerator(entry);
					child.generate(yy);
					child = null;
				}
				// else do nothing, because it's a dot directory
			}
			else {
				// Use this instead of a return statement.
				yy.yield(entry);
			}
		}
	}
}
