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
    private ForgePreferences preferences;
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

        if (logFile.exists() && !deleteSucceeded && (logFile.length() != 0)) {
            throw new IllegalStateException("Could not delete existing logFile:" + logFile.getAbsolutePath());
        }

        this.logFileStream = new FileOutputStream(logFile);

        this.oldSystemOut = System.out;
        System.setOut(new PrintStream(new MultiplexOutputStream(System.out, this.logFileStream), true));
        this.oldSystemErr = System.err;
        System.setErr(new PrintStream(new MultiplexOutputStream(System.err, this.logFileStream), true));

        try {
            this.setPreferences(new ForgePreferences("forge.preferences"));
        } catch (final Exception exn) {
            throw new RuntimeException(exn); // NOPMD by Braids on 8/13/11 8:21
                                             // PM
        }

        Constant.Runtime.MILL[0] = this.preferences.isMillingLossCondition();
        Constant.Runtime.DEV_MODE[0] = this.preferences.isDeveloperMode();
        Constant.Runtime.UPLOAD_DRAFT[0] = this.preferences.isUploadDraftAI();
        Constant.Runtime.RANDOM_FOIL[0] = this.preferences.isRandCFoil();

        final HttpUtil pinger = new HttpUtil();
        if (pinger.getURL("http://cardforge.org/draftAI/ping.php").equals("pong")) {
            Constant.Runtime.NET_CONN[0] = true;
        } else {
            Constant.Runtime.UPLOAD_DRAFT[0] = false;
        }

        this.setBuildInfo(new BuildInfo());
    }

    /**
     * Destructor for FModel.
     * 
     * @throws Throwable
     *             indirectly
     */
    @Override
    protected final void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    /**
     * Opposite of constructor; resets all system resources and closes the log
     * file.
     */
    public final void close() {
        System.setOut(this.oldSystemOut);
        System.setErr(this.oldSystemErr);
        try {
            this.logFileStream.close();
        } catch (final IOException e) { // NOPMD by Braids on 8/12/11 10:25 AM
            // ignored
        }
    }

    /**
     * Getter for buildInfo.
     * 
     * @return the buildInfo
     */
    public final BuildInfo getBuildInfo() {
        return this.buildInfo;
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
        return this.preferences;
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
        return this.gameState;
    }

    /**
     * Create and return a new game state.
     * 
     * @return a fresh game state
     */
    public final FGameState resetGameState() {
        this.gameState = new FGameState();
        return this.gameState;
    }

}
