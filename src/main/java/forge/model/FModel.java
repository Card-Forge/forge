package forge.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import arcane.util.MultiplexOutputStream;

/**
 * The default Model implementation for Forge.
 *
 * This used to be an interface, but it seems unlikely that we will ever use a 
 * different model.
 *
 * In case we need to convert it into an interface in the future, all fields of
 * this class must be either private or public static final.
 */
public class FModel {
    /**
     * Constructor.
     *
     * @throws FileNotFoundException if we could not find or write to the log file.
     */
    public FModel() throws FileNotFoundException {
        final File logFile = new File("forge.log");
        final boolean deleteSucceeded = logFile.delete();

        if (logFile.exists() && !deleteSucceeded) {
            throw new IllegalStateException("Could not delete existing logFile:" + logFile.getAbsolutePath());
        }

        // This used to be a BufferedOutputStream, but that seems inappropriate for a log.  --Braids
        final OutputStream logFileStream = new FileOutputStream(logFile);

        System.setOut(new PrintStream(new MultiplexOutputStream(System.out, logFileStream), true));
        System.setErr(new PrintStream(new MultiplexOutputStream(System.err, logFileStream), true));

    }
}
