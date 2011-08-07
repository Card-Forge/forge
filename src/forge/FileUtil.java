package forge;

import forge.error.ErrorViewer;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * <p>FileUtil class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class FileUtil {
    /**
     * <p>doesFileExist.</p>
     *
     * @param filename a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean doesFileExist(String filename) {
        File f = new File(filename);
        return f.exists();
    }

    /**
     * <p>writeFile.</p>
     *
     * @param filename a {@link java.lang.String} object.
     * @param data a {@link java.util.List} object.
     */
    public static void writeFile(String filename, List<String> data) {
        writeFile(new File(filename), data);
    }

    //writes each element of ArrayList on a separate line
    //this is used to write a file of Strings
    //this will create a new file if needed
    //if filename already exists, it is deleted
    /**
     * <p>writeFile.</p>
     *
     * @param file a {@link java.io.File} object.
     * @param data a {@link java.util.List} object.
     */
    public static void writeFile(File file, List<String> data) {
        try {
            Collections.sort(data);

            BufferedWriter io = new BufferedWriter(new FileWriter(file));
            for (int i = 0; i < data.size(); i++)
                io.write(data.get(i) + "\r\n");

            io.flush();
            io.close();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("FileUtil : writeFile() error, problem writing file - " + file + " : " + ex);
        }
    }//writeAllDecks()

    /**
     * <p>readFile.</p>
     *
     * @param filename a {@link java.lang.String} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<String> readFile(String filename) {
        return readFile(new File(filename));
    }

    //reads line by line and adds each line to the ArrayList
    //this will return blank lines as well
    //if filename not found, returns an empty ArrayList
    /**
     * <p>readFile.</p>
     *
     * @param file a {@link java.io.File} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<String> readFile(File file) {
        ArrayList<String> list = new ArrayList<String>();
        BufferedReader in;

        try {
            if (file == null || !file.exists()) return list;


            in = new BufferedReader(new FileReader(file));

            String line;
            while ((line = in.readLine()) != null)
                list.add(line);
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("FileUtil : readFile() error, " + ex);
        }

        return list;
    }//readFile()
}
