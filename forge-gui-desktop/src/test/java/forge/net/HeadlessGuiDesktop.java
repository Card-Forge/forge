package forge.net;

import forge.GuiDesktop;
import forge.gamemodes.match.HostedMatch;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;

import java.util.List;

/**
 * Headless implementation of GuiDesktop for automated testing.
 * Overrides methods that require Singletons.getControl() or FSkin to be initialized.
 */
public class HeadlessGuiDesktop extends GuiDesktop {

    @Override
    public HostedMatch hostMatch() {
        // Skip Singletons.getControl().addMatch() which requires full GUI init
        return new HostedMatch();
    }

    @Override
    public IGuiGame getNewGuiGame() {
        return new HeadlessNetworkGuiGame();
    }

    @Override
    public void showSpellShop() {
    }

    @Override
    public void showBazaar() {
    }

    // ========== Dialog methods - override to avoid FSkin dependency ==========

    @Override
    public int showOptionDialog(final String message, final String title,
                                final FSkinProp icon, final List<String> options,
                                final int defaultOption) {
        System.err.println("[HeadlessGuiDesktop] " + title + ": " + message);
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
        return null;
    }

    @Override
    public void showBugReportDialog(final String title, final String text, final boolean showExitAppBtn) {
        System.err.println("[HeadlessGuiDesktop] Bug Report - " + title + ":");
        System.err.println(text);
    }

    // ========== Audio methods - disable in headless mode ==========

    @Override
    public forge.sound.IAudioClip createAudioClip(final String filename) {
        return null;
    }

    @Override
    public forge.sound.IAudioMusic createAudioMusic(final String filename) {
        return null;
    }

    @Override
    public void startAltSoundSystem(final String filename, final boolean isSynchronized) {
    }

    // EDT methods: inherit from GuiDesktop (SwingUtilities.invokeLater/invokeAndWait)
    // to preserve real threading semantics. Running EDT synchronously would hide
    // concurrency bugs between the game thread and EDT.
}
