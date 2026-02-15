package forge.net;

import forge.GuiDesktop;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.net.NetworkDebugLogger;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;

import java.util.List;

/**
 * Headless implementation of GuiDesktop for automated testing.
 * Overrides methods that require Singletons.getControl() or FSkin to be initialized.
 *
 * This allows running full AI games without a display server because:
 * 1. GuiDesktop.hostMatch() normally calls Singletons.getControl().addMatch()
 * 2. GuiDesktop.getNewGuiGame() returns CMatchUI which requires full GUI
 * 3. showSpellShop/showBazaar require Singletons.getControl()
 * 4. Dialog methods use FSkin which isn't initialized in headless mode
 *
 * Part of the Headless Full Game Testing infrastructure.
 */
public class HeadlessGuiDesktop extends GuiDesktop {

    // Track the last match for result extraction
    private static volatile HostedMatch lastMatch;

    // Flag to track if testing environment message has been logged
    private static boolean testingEnvironmentLogged = false;

    // Note: No static initializer - tests must call NetworkDebugLogger.setTestMode(true)
    // AFTER setting GuiBase.setInterface() to avoid initialization order issues.

    /**
     * Create a HostedMatch without registering it with Singletons.getControl().
     * This allows games to run without full GUI initialization.
     * The match is stored for later result extraction.
     *
     * @return A new HostedMatch instance
     */
    @Override
    public HostedMatch hostMatch() {
        // Log testing environment banner on first match (lazy initialization)
        logTestingEnvironment();
        // Skip Singletons.getControl().addMatch() which requires full GUI init
        lastMatch = new HostedMatch();
        return lastMatch;
    }

    /**
     * Log a message indicating this is a testing environment.
     * Called lazily on first match to avoid static initialization issues.
     */
    private static synchronized void logTestingEnvironment() {
        if (!testingEnvironmentLogged) {
            testingEnvironmentLogged = true;
            NetworkDebugLogger.log("================================================================================");
            NetworkDebugLogger.log("TESTING ENVIRONMENT - HeadlessGuiDesktop Active");
            NetworkDebugLogger.log("This log was created from automated test infrastructure.");
            NetworkDebugLogger.log("================================================================================");
        }
    }

    /**
     * Get the last hosted match for result extraction.
     *
     * @return The last HostedMatch, or null if none
     */
    public static HostedMatch getLastMatch() {
        return lastMatch;
    }

    /**
     * Clear the last match reference.
     */
    public static void clearLastMatch() {
        lastMatch = null;
    }

    /**
     * Return a headless network GUI implementation for AI spectating.
     * Uses HeadlessNetworkGuiGame which supports delta sync testing.
     *
     * @return A HeadlessNetworkGuiGame instance
     */
    @Override
    public IGuiGame getNewGuiGame() {
        return new HeadlessNetworkGuiGame();
    }

    /**
     * No-op in headless mode - requires Singletons.getControl().
     */
    @Override
    public void showSpellShop() {
        // No-op - requires Singletons.getControl().setCurrentScreen()
    }

    /**
     * No-op in headless mode - requires Singletons.getControl().
     */
    @Override
    public void showBazaar() {
        // No-op - requires Singletons.getControl().setCurrentScreen()
    }

    // ========== Dialog methods - override to avoid FSkin dependency ==========

    /**
     * Show option dialog without GUI - prints to stderr and returns default option.
     * This prevents NPE when FSkin images aren't loaded.
     */
    @Override
    public int showOptionDialog(final String message, final String title,
                                final FSkinProp icon, final List<String> options,
                                final int defaultOption) {
        System.err.println("[HeadlessGuiDesktop] " + title + ": " + message);
        if (options != null && !options.isEmpty()) {
            System.err.println("[HeadlessGuiDesktop] Options: " + options + ", returning: " + defaultOption);
        }
        return defaultOption;
    }

    /**
     * Show image dialog without GUI - prints to stderr.
     */
    @Override
    public void showImageDialog(final ISkinImage image, final String message, final String title) {
        System.err.println("[HeadlessGuiDesktop] " + title + ": " + message);
    }

    /**
     * Show input dialog without GUI - returns initial input or first option.
     */
    @Override
    public String showInputDialog(final String message, final String title,
                                  final FSkinProp icon, final String initialInput,
                                  final List<String> inputOptions, boolean isNumeric) {
        System.err.println("[HeadlessGuiDesktop] Input: " + title + ": " + message);
        if (initialInput != null) {
            return initialInput;
        }
        if (inputOptions != null && !inputOptions.isEmpty()) {
            return inputOptions.get(0);
        }
        return isNumeric ? "0" : "";
    }

    /**
     * Show file dialog without GUI - returns null (no file selected).
     */
    @Override
    public String showFileDialog(final String title, final String defaultDir) {
        System.err.println("[HeadlessGuiDesktop] File dialog: " + title);
        return null;
    }

    /**
     * Show bug report dialog without GUI - prints to stderr.
     */
    @Override
    public void showBugReportDialog(final String title, final String text, final boolean showExitAppBtn) {
        System.err.println("[HeadlessGuiDesktop] Bug Report - " + title + ":");
        System.err.println(text);
    }

    // ========== Audio methods - disable in headless mode ==========

    /**
     * Return null to disable audio clips in headless mode.
     */
    @Override
    public forge.sound.IAudioClip createAudioClip(final String filename) {
        return null;  // No audio in headless mode
    }

    /**
     * Return null to disable music in headless mode.
     */
    @Override
    public forge.sound.IAudioMusic createAudioMusic(final String filename) {
        return null;  // No audio in headless mode
    }

    /**
     * No-op in headless mode - don't start sound system.
     */
    @Override
    public void startAltSoundSystem(final String filename, final boolean isSynchronized) {
        // No-op - no audio in headless mode
    }

    // ========== EDT methods - execute immediately in headless mode ==========

    /**
     * Execute runnable immediately in headless mode.
     * In normal GUI mode, this uses SwingUtilities.invokeLater which requires an EDT.
     * Since headless testing has no EDT, we execute immediately to prevent deadlocks.
     */
    @Override
    public void invokeInEdtLater(final Runnable proc) {
        // Execute immediately - no EDT in headless mode
        proc.run();
    }

    /**
     * Execute runnable immediately in headless mode.
     */
    @Override
    public void invokeInEdtNow(final Runnable proc) {
        proc.run();
    }

    /**
     * Execute runnable immediately and wait (same as invokeInEdtNow in headless mode).
     */
    @Override
    public void invokeInEdtAndWait(final Runnable proc) {
        proc.run();
    }
}
