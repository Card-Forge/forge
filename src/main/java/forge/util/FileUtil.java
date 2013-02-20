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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import forge.error.BugReporter;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang.GuiDownloadPictures.Errors;

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
    public static void writeFile(final String filename, final List<String> data) {
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
    public static void writeFile(final File file, final List<String> data) {
        try {
            final BufferedWriter io = new BufferedWriter(new FileWriter(file));
            for (int i = 0; i < data.size(); i++) {
                io.write(data.get(i));
                io.write("\r\n");
            }

            io.flush();
            io.close();
        } catch (final Exception ex) {
            BugReporter.reportException(ex);
            throw new RuntimeException("FileUtil : writeFile() error, problem writing file - " + file + " : " + ex);
        }
    } // writeAllDecks()

    /**
     * <p>
     * readFile.
     * </p>
     * 
     * @param filename
     *            a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
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
                return new ArrayList<String>();
            }
            return FileUtil.readAllLines(new FileReader(file), false);
        } catch (final Exception ex) {
            BugReporter.reportException(ex);
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
        final ArrayList<String> list = new ArrayList<String>();
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
            BugReporter.reportException(ex);
            throw new RuntimeException("FileUtil : readAllLines() error, " + ex);
        }
        return list;
    }

    /**
     * Download url into file.
     * 
     * @param url
     *            the url
     * @param target
     *            the target
     */
    public static void downloadUrlIntoFile(final String url, final File target) {
        try {
            final byte[] buf = new byte[1024];
            int len;

            final Proxy p = Proxy.NO_PROXY;
            final BufferedInputStream in = new BufferedInputStream(new URL(url).openConnection(p).getInputStream());
            final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target));

            // while - read and write file
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);

            } // while - read and write file
            in.close();
            out.flush();
            out.close();
        } catch (final IOException ioex) {
            BugReporter.reportException(ioex, ForgeProps.getLocalized(Errors.OTHER), "deck_temp.html", url);
        }

    }
}
