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
package forge;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * FileFinder class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class FileFinder {

    private Pattern p = null;
    private Matcher m = null;
    private long totalLength = 0;
    private int filesNumber = 0;
    private long directoriesNumber = 0;
    /** Constant <code>FILES=0</code>. */
    private static final int FILES = 0;
    /** Constant <code>DIRECTORIES=1</code>. */
    private static final int DIRECTORIES = 1;
    private ArrayList<String> fileNames;
    private ArrayList<String> fName;

    /**
     * <p>
     * Constructor for FileFinder.
     * </p>
     */
    public FileFinder() {
    }

    /**
     * <p>
     * findFiles.
     * </p>
     * 
     * @param startPath
     *            a {@link java.lang.String} object.
     * @param mask
     *            a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws Exception
     *             the exception
     */
    public final List<File> findFiles(final String startPath, final String mask) throws Exception {
        this.fileNames = new ArrayList<String>();
        this.fName = new ArrayList<String>();
        return this.findWithFull(startPath, mask, FileFinder.FILES);
    }

    /**
     * <p>
     * getDirectorySize.
     * </p>
     * 
     * @return a long.
     */
    public final long getDirectorySize() {
        return this.totalLength;
    }

    /**
     * <p>
     * Getter for the field <code>filesNumber</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getFilesNumber() {
        return this.filesNumber;
    }

    /**
     * <p>
     * Getter for the field <code>directoriesNumber</code>.
     * </p>
     * 
     * @return a long.
     */
    public final long getDirectoriesNumber() {
        return this.directoriesNumber;
    }

    /**
     * <p>
     * accept.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    private boolean accept(final String name) {

        if (this.p == null) {
            return true;
        }

        this.m = this.p.matcher(name);

        return this.m.matches();
    }

    /**
     * <p>
     * findWithFull.
     * </p>
     * 
     * @param startPath
     *            a {@link java.lang.String} object.
     * @param mask
     *            a {@link java.lang.String} object.
     * @param objectType
     *            a int.
     * @return a {@link java.util.List} object.
     * @throws java.lang.Exception
     *             if any.
     */
    private List<File> findWithFull(final String startPath, final String mask, final int objectType) throws Exception {

        if ((startPath == null) || (mask == null)) {
            throw new Exception("Error");
        }
        final File topDirectory = new File(startPath);
        if (!topDirectory.exists()) {
            throw new Exception("Error");
        }

        if (!mask.equals("")) {
            this.p = Pattern.compile(mask, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        this.filesNumber = 0;
        this.directoriesNumber = 0;
        this.totalLength = 0;
        final ArrayList<File> res = new ArrayList<File>(100);

        this.searchWithFull(topDirectory, res, objectType);
        this.p = null;
        return res;
    }

    /**
     * <p>
     * searchWithFull.
     * </p>
     * 
     * @param topDirectory
     *            a {@link java.io.File} object.
     * @param res
     *            a {@link java.util.List} object.
     * @param objectType
     *            a int.
     */
    private void searchWithFull(final File topDirectory, final List<File> res, final int objectType) {

        final File[] list = topDirectory.listFiles();

        for (final File element : list) {

            if (element.isDirectory()) {

                if ((objectType != FileFinder.FILES) && this.accept(element.getName())) {

                    this.directoriesNumber++;
                    res.add(element);
                }

                this.searchWithFull(element, res, objectType);
            } else {

                if ((objectType != FileFinder.DIRECTORIES) && this.accept(element.getName())) {
                    if (element.getName().contains("full")) {
                        if (this.fileNames.size() == 0) {
                            this.fileNames.add(element.getName());
                            this.filesNumber++;
                            this.totalLength += element.length();
                            res.add(element);
                        }
                        this.fName.add(element.getName());
                        if (this.fileNames.size() >= 1) {
                            if (Collections.indexOfSubList(this.fileNames, this.fName) == -1) {
                                this.fileNames.add(element.getName());
                                this.filesNumber++;
                                this.totalLength += element.length();
                                res.add(element);
                            }
                            this.fName.remove(0);
                        }
                    }
                }
            }
        }
    }

}
