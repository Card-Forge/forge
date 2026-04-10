package forge.net;

import forge.GuiDesktop;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.net.IHasNetLog;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;

import java.util.List;

/**
 * Headless GuiDesktop for automated testing — the {@code GuiBase} interface that
 * enables running without a display. Overrides UI methods (dialogs, audio, FSkin)
 * with no-ops or defaults. {@link #getNewGuiGame()} returns {@link HeadlessNetworkGuiGame},
 * which is the server-side GUI for hosted games.
 *
 * <p>Set via {@link TestUtils#ensureFModelInitialized()}.
 */
public class HeadlessGuiDesktop extends GuiDesktop implements IHasNetLog {

    private static volatile HostedMatch lastMatch;
    private static boolean testingEnvironmentLogged = false;

    // Tests must call NetworkLogConfig.setTestMode(true) AFTER GuiBase.setInterface()
    // to avoid initialization order issues.

    @Override
    public HostedMatch hostMatch() {
        logTestingEnvironment();
        lastMatch = new HostedMatch();
        return lastMatch;
    }

    private static synchronized void logTestingEnvironment() {
        if (!testingEnvironmentLogged) {
            testingEnvironmentLogged = true;
            netLog.info("================================================================================");
            netLog.info("TESTING ENVIRONMENT - HeadlessGuiDesktop Active");
            netLog.info("This log was created from automated test infrastructure.");
            netLog.info("================================================================================");
        }
    }

    public static HostedMatch getLastMatch() {
        return lastMatch;
    }

    public static void clearLastMatch() {
        lastMatch = null;
    }

    @Override
    public IGuiGame getNewGuiGame() {
        return new HeadlessNetworkGuiGame();
    }

    @Override
    public void showSpellShop() { }

    @Override
    public void showBazaar() { }

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

    @Override
    public void showImageDialog(final ISkinImage image, final String message, final String title) {
        System.err.println("[HeadlessGuiDesktop] " + title + ": " + message);
    }

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

    @Override
    public String showFileDialog(final String title, final String defaultDir) {
        System.err.println("[HeadlessGuiDesktop] File dialog: " + title);
        return null;
    }

    @Override
    public void showBugReportDialog(final String title, final String text, final boolean showExitAppBtn) {
        System.err.println("[HeadlessGuiDesktop] Bug Report - " + title + ":");
        System.err.println(text);
    }

    @Override
    public forge.sound.IAudioClip createAudioClip(final String filename) {
        return null;
    }

    @Override
    public forge.sound.IAudioMusic createAudioMusic(final String filename) {
        return null;
    }

    @Override
    public void startAltSoundSystem(final String filename, final boolean isSynchronized) { }

    // EDT methods intentionally NOT overridden — inherits real SwingUtilities
    // threading from GuiDesktop to preserve two-thread (Netty + EDT) concurrency
    // so tests can catch race conditions.
}
