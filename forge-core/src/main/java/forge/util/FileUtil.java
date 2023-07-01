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
package forge.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * <p>
 * FileUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class FileUtil {

    private FileUtil() {
        throw new AssertionError();
    }
    
    /**
     * Takes two paths and combines them into a valid path string
     * for the current OS.
     * <p>
     * Similar to the Path.Combine() function in .Net.
     */
    public static String pathCombine(String path1, String path2) {
        File file1 = new File(path1);
        File file2 = new File(file1, path2);
        return file2.getPath();
    }      

    /**
     * <p>
     * doesFileExist.
     * </p>
     * 
     * @param filename
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean doesFileExist(final String filename) {
        final File f = new File(filename);
        return f.exists();
    }

    public static boolean isDirectoryWithFiles(final String path) {
        if (path == null) return false;
        final File f = new File(path);
        final String[] fileList = f.list();
        return fileList!=null && fileList.length > 0;
    }

    public static boolean ensureDirectoryExists(final String path) {
        return ensureDirectoryExists(new File(path));
    }
    public static boolean ensureDirectoryExists(final File dir) {
        return (dir.exists() && dir.isDirectory()) || dir.mkdirs();
    }

    public static boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            for (String filename : dir.list()) {
                if (!deleteDirectory(new File(dir, filename))) {
                    return false; 
                } 
            }
        }
        return dir.delete();
    }

    public static boolean deleteFile(String filename) {
        try {
            File file = new File(filename);
            return file.delete();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void copyFile(String sourceFilename, String destFilename) {
        File source = new File(sourceFilename);
        if (!source.exists()) { return; } //if source doesn't exist, nothing to copy

        try (InputStream is = new FileInputStream(source);
             OutputStream os = new FileOutputStream(new File(destFilename))){
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeFile(String filename, String text) {
        FileUtil.writeFile(new File(filename), text);
    }

    public static void writeFile(File file, String text) {
        try (PrintWriter p = new PrintWriter(file)) {
            p.print(text);
        } catch (final Exception ex) {
            throw new RuntimeException("FileUtil : writeFile() error, problem writing file - " + file + " : " + ex);
        }
    }

    /**
     * <p>
     * writeFile.
     * </p>
     * 
     * @param filename
     *            a {@link java.lang.String} object.
     * @param data
     *            a {@link java.util.List} object.
     */
    public static void writeFile(String filename, List<String> data) {
        FileUtil.writeFile(new File(filename), data);
    }

    // writes each element of ArrayList on a separate line
    // this is used to write a file of Strings
    // this will create a new file if needed
    // if filename already exists, it is deleted
    /**
     * <p>
     * writeFile.
     * </p>
     * 
     * @param file
     *            a {@link java.io.File} object.
     * @param data
     *            a {@link java.util.List} object.
     */
    public static void writeFile(File file, Collection<?> data) {
        try (PrintWriter p = new PrintWriter(file)) {
            for (Object o : data) {
                p.println(o);
            }
        } catch (final Exception ex) {
            throw new RuntimeException("FileUtil : writeFile() error, problem writing file - " + file + " : " + ex);
        }
    } // writeAllDecks()

    public static String readFileToString(String filename) {
    	return readFileToString(new File(filename));
    }

    public static String readFileToString(File file) {
        return TextUtil.join(readFile(file), "\n");
    }

    public static List<String> readFile(final String filename) {
        return FileUtil.readFile(new File(filename));
    }

    // reads line by line and adds each line to the ArrayList
    // this will return blank lines as well
    // if filename not found, returns an empty ArrayList
    /**
     * <p>
     * readFile.
     * </p>
     * 
     * @param file
     *            a {@link java.io.File} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static List<String> readFile(final File file) {
        try {
            if ((file == null) || !file.exists()) {
                return new ArrayList<>();
            }
            return FileUtil.readAllLines(file, false);
        } catch (final Exception ex) {
            throw new RuntimeException("FileUtil : readFile() error, " + ex);
        }
    } // readFile()

    /**
     * Read all lines.
     *
     * @param reader the reader
     * @return the list
     */
    public static List<String> readAllLines(final Reader reader) {
        return FileUtil.readAllLines(reader, false);
    }

    /**
     * Reads all lines from given reader to a list of strings.
     *
     * @param reader is a reader (e.g. FileReader, InputStreamReader)
     * @param mayTrim defines whether to trim lines.
     * @return list of strings
     */
    public static List<String> readAllLines(final Reader reader, final boolean mayTrim) {
        final List<String> list = new ArrayList<>();
        try {
            final BufferedReader in = new BufferedReader(reader);
            String line;
            while ((line = in.readLine()) != null) {
                if (mayTrim) {
                    line = line.trim();
                }
                list.add(line);
            }
            in.close();
        } catch (final IOException ex) {
            throw new RuntimeException("FileUtil : readAllLines() error, " + ex);
        }
        return list;
    }
    /**
     * Reads all lines from given file to a list of strings.
     *
     * @param file is the File to read.
     * @param mayTrim defines whether to trim lines.
     * @return list of strings
     */
    public static List<String> readAllLines(final File file, final boolean mayTrim) {
        final List<String> list = new ArrayList<>();
        try {
            final BufferedReader in = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                if (mayTrim) {
                    line = line.trim();
                }
                list.add(line);
            }
            in.close();
        } catch (final IOException ex) {
            throw new RuntimeException("FileUtil : readAllLines() error, " + ex);
        }
        return list;
    }

    // returns a list of <name, url> pairs.  if the name is not in the file, it is synthesized from the url
    public static List<Pair<String, String>> readNameUrlFile(String nameUrlFile) {
        Pattern lineSplitter = Pattern.compile(Pattern.quote(" "));
        Pattern replacer = Pattern.compile(Pattern.quote("%20"));

        List<Pair<String, String>> list = new ArrayList<>();

        for (String line : readFile(nameUrlFile)) {
            if (StringUtils.isBlank(line) || line.startsWith("#")) {
                continue;
            }
            
            String[] parts = lineSplitter.split(line, 2);
            if (2 == parts.length) {
                list.add(Pair.of(replacer.matcher(parts[0]).replaceAll(" "), parts[1]));
            } else {
                // figure out the filename from the URL
                Pattern pathSplitter = Pattern.compile(Pattern.quote("/"));
                String[] pathParts = pathSplitter.split(parts[0]);
                String last = pathParts[pathParts.length - 1];
                list.add(Pair.of(replacer.matcher(last).replaceAll(" "), parts[0]));
            }
        }

        return list;
    }

    public static String readFileToString(final URL url) {
        return TextUtil.join(readFile(url), "\n");
    }

    public static List<String> readFile(final URL url) {
        final List<String> lines = new ArrayList<>();
        ThreadUtil.executeWithTimeout(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        lines.add(line);
                    }
                }
                return null;
            }
        }, 5000); //abort reading file if it takes longer than 5 seconds
        return lines;
    }

    public static String getParent(final String resourcePath) {
        File f = new File(resourcePath);
        if (f.getParentFile().getName() != null)
            return f.getParentFile().getName();
        return "";
    }
}
