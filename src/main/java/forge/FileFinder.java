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
        fileNames = new ArrayList<String>();
        fName = new ArrayList<String>();
        return findWithFull(startPath, mask, FILES);
    }

    /**
     * <p>
     * getDirectorySize.
     * </p>
     * 
     * @return a long.
     */
    public final long getDirectorySize() {
        return totalLength;
    }

    /**
     * <p>
     * Getter for the field <code>filesNumber</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getFilesNumber() {
        return filesNumber;
    }

    /**
     * <p>
     * Getter for the field <code>directoriesNumber</code>.
     * </p>
     * 
     * @return a long.
     */
    public final long getDirectoriesNumber() {
        return directoriesNumber;
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

        if (p == null) {
            return true;
        }

        m = p.matcher(name);

        return m.matches();
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

        if (startPath == null || mask == null) {
            throw new Exception("Error");
        }
        File topDirectory = new File(startPath);
        if (!topDirectory.exists()) {
            throw new Exception("Error");
        }

        if (!mask.equals("")) {
            p = Pattern.compile(mask, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        filesNumber = 0;
        directoriesNumber = 0;
        totalLength = 0;
        ArrayList<File> res = new ArrayList<File>(100);

        searchWithFull(topDirectory, res, objectType);
        p = null;
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

        File[] list = topDirectory.listFiles();

        for (int i = 0; i < list.length; i++) {

            if (list[i].isDirectory()) {

                if (objectType != FILES && accept(list[i].getName())) {

                    directoriesNumber++;
                    res.add(list[i]);
                }

                searchWithFull(list[i], res, objectType);
            } else {

                if (objectType != DIRECTORIES && accept(list[i].getName())) {
                    if (list[i].getName().contains("full")) {
                        if (fileNames.size() == 0) {
                            fileNames.add(list[i].getName());
                            filesNumber++;
                            totalLength += list[i].length();
                            res.add(list[i]);
                        }
                        fName.add(list[i].getName());
                        if (fileNames.size() >= 1) {
                            if (Collections.indexOfSubList(fileNames, fName) == -1) {
                                fileNames.add(list[i].getName());
                                filesNumber++;
                                totalLength += list[i].length();
                                res.add(list[i]);
                            }
                            fName.remove(0);
                        }
                    }
                }
            }
        }
    }

}
