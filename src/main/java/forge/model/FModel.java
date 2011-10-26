package forge.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import net.slightlymagic.braids.util.progress_monitor.BraidsProgressMonitor;
import arcane.util.MultiplexOutputStream;
import forge.Constant;
import forge.HttpUtil;
import forge.properties.ForgePreferences;

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
    // private static final int NUM_INIT_PHASES = 1;

    private final transient OutputStream logFileStream;
    private final transient PrintStream oldSystemOut;
    private final transient PrintStream oldSystemErr;
    private BuildInfo buildInfo;

    /** The preferences. */
    public ForgePreferences preferences;
    private FGameState gameState;

    /**
     * Constructor.
     * 
     * @param theMonitor
     *            a progress monitor (from the View) that shows the progress of
     *            the model's initialization.
     * 
     * @throws FileNotFoundException
     *             if we could not find or write to the log file.
     */
    public FModel(final BraidsProgressMonitor theMonitor) throws FileNotFoundException {
        /*
         * To be implemented later. -Braids BraidsProgressMonitor monitor; if
         * (theMonitor == null) { monitor = new
         * BaseProgressMonitor(NUM_INIT_PHASES, 1); } else { monitor =
         * theMonitor; }
         */

        final File logFile = new File("forge.log");
        final boolean deleteSucceeded = logFile.delete();

        if (logFile.exists() && !deleteSucceeded && logFile.length() != 0) {
            throw new IllegalStateException("Could not delete existing logFile:" + logFile.getAbsolutePath());
        }

        logFileStream = new FileOutputStream(logFile);

        oldSystemOut = System.out;
        System.setOut(new PrintStream(new MultiplexOutputStream(System.out, logFileStream), true));
        oldSystemErr = System.err;
        System.setErr(new PrintStream(new MultiplexOutputStream(System.err, logFileStream), true));

        try {
            setPreferences(new ForgePreferences("forge.preferences"));
        } catch (Exception exn) {
            throw new RuntimeException(exn); // NOPMD by Braids on 8/13/11 8:21
                                             // PM
        }

        Constant.Runtime.Mill[0] = preferences.millingLossCondition;
        Constant.Runtime.DevMode[0] = preferences.developerMode;
        Constant.Runtime.UpldDrft[0] = preferences.uploadDraftAI;
        Constant.Runtime.RndCFoil[0] = preferences.randCFoil;

        final HttpUtil pinger = new HttpUtil();
        if (pinger.getURL("http://cardforge.org/draftAI/ping.php").equals("pong")) {
            Constant.Runtime.NetConn[0] = true;
        } else {
            Constant.Runtime.UpldDrft[0] = false;
        }

        setBuildInfo(new BuildInfo());
    }

    /**
     * Destructor for FModel.
     * 
     * @throws Throwable
     *             indirectly
     */
    @Override
    protected final void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * Opposite of constructor; resets all system resources and closes the log
     * file.
     */
    public final void close() {
        System.setOut(oldSystemOut);
        System.setErr(oldSystemErr);
        try {
            logFileStream.close();
        } catch (IOException e) { // NOPMD by Braids on 8/12/11 10:25 AM
            // ignored
        }
    }

    /**
     * Getter for buildInfo.
     * 
     * @return the buildInfo
     */
    public final BuildInfo getBuildInfo() {
        return buildInfo;
    }

    /**
     * Setter for buildInfo.
     * 
     * @param neoBuildInfo
     *            the buildInfo to set
     */
    protected final void setBuildInfo(final BuildInfo neoBuildInfo) {
        this.buildInfo = neoBuildInfo;
    }

    /**
     * Gets the preferences.
     * 
     * @return the preferences
     */
    public final ForgePreferences getPreferences() {
        return preferences;
    }

    /**
     * Sets the preferences.
     * 
     * @param neoPreferences
     *            the preferences to set
     */
    public final void setPreferences(final ForgePreferences neoPreferences) {
        this.preferences = neoPreferences;
    }

    /**
     * Getter for gameState.
     * 
     * @return the game state
     */
    public final FGameState getGameState() {
        return gameState;
    }

    /**
     * Create and return a new game state.
     * 
     * @return a fresh game state
     */
    public final FGameState resetGameState() {
        gameState = new FGameState();
        return gameState;
    }

}
