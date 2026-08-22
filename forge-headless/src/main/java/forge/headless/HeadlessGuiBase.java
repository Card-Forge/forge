package forge.headless;

import forge.gamemodes.match.HostedMatch;
import forge.gui.download.GuiDownloadService;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.util.BuildInfo;
import forge.util.ImageFetcher;

import org.jupnp.UpnpServiceConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import forge.util.FSerializableFunction;

/**
 * Minimal headless implementation of IGuiBase for simulation mode.
 * Most methods are no-ops or throw UnsupportedOperationException.
 */
public class HeadlessGuiBase implements IGuiBase {

    @Override
    public boolean isRunningOnDesktop() {
        return false;
    }

    @Override
    public boolean isLibgdxPort() {
        return false;
    }

    @Override
    public String getCurrentVersion() {
        return BuildInfo.getVersionString();
    }

    @Override
    public String getAssetsDir() {
        // For development builds (git or SNAPSHOT), look for resources in the forge-gui module
        String version = BuildInfo.getVersionString().toLowerCase();
        if (version.contains("git") || version.contains("snapshot")) {
            // Check if we're running from forge-headless/target directory
            File targetResDir = new File("../../forge-gui/res");
            if (targetResDir.exists() && targetResDir.isDirectory()) {
                return "../../forge-gui/";
            }
            // Fallback for other development scenarios
            return "../forge-gui/";
        }
        // For release builds, resources are in the current directory
        return "";
    }

    @Override
    public ImageFetcher getImageFetcher() {
        return null; // Not needed for headless simulation
    }

    @Override
    public void invokeInEdtNow(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void invokeInEdtLater(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void invokeInEdtAndWait(Runnable proc) {
        proc.run();
    }

    @Override
    public boolean isGuiThread() {
        return true;
    }

    @Override
    public ISkinImage getSkinIcon(FSkinProp skinProp) {
        return null;
    }

    @Override
    public ISkinImage getUnskinnedIcon(String path) {
        return null;
    }

    @Override
    public ISkinImage getCardArt(PaperCard card) {
        return null;
    }

    @Override
    public ISkinImage getCardArt(PaperCard card, boolean backFace) {
        return null;
    }

    @Override
    public ISkinImage createLayeredImage(PaperCard card, FSkinProp background, String overlayFilename, float opacity) {
        return null;
    }

    @Override
    public void showBugReportDialog(String title, String text, boolean showExitAppBtn) {
        System.err.println("Bug Report: " + title);
        System.err.println(text);
    }

    @Override
    public void showImageDialog(ISkinImage image, String message, String title) {
        System.out.println(title + ": " + message);
    }

    @Override
    public int showOptionDialog(String message, String title, FSkinProp icon, List<String> options, int defaultOption) {
        return defaultOption;
    }

    @Override
    public String showInputDialog(String message, String title, FSkinProp icon, String initialInput, List<String> inputOptions, boolean isNumeric) {
        return initialInput;
    }

    @Override
    public <T> List<T> getChoices(String message, int min, int max, Collection<T> choices, Collection<T> selected, FSerializableFunction<T, String> display) {
        return List.copyOf(selected);
    }

    @Override
    public <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax, List<T> sourceChoices, List<T> destChoices) {
        return destChoices;
    }

    @Override
    public String showFileDialog(String title, String defaultDir) {
        return defaultDir;
    }

    @Override
    public File getSaveFile(File defaultFile) {
        return defaultFile;
    }

    @Override
    public void download(GuiDownloadService service, Consumer<Boolean> callback) {
        throw new UnsupportedOperationException("Downloads not supported in headless mode");
    }

    public void refreshSkin() {
        // No-op
    }

    @Override
    public boolean hasNetGame() {
        return false;
    }

    @Override
    public void showCardList(String title, String message, List<PaperCard> list) {
        System.out.println(title + ": " + message);
    }

    @Override
    public boolean showBoxedProduct(String title, String message, List<PaperCard> list) {
        return false;
    }

    @Override
    public PaperCard chooseCard(String title, String message, List<PaperCard> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public int getAvatarCount() {
        return 0;
    }

    @Override
    public int getSleevesCount() {
        return 0;
    }

    @Override
    public void copyToClipboard(String text) {
        // No-op
    }

    @Override
    public void browseToUrl(String url) throws IOException, URISyntaxException {
        System.out.println("Browse to: " + url);
    }

    @Override
    public boolean isSupportedAudioFormat(File file) {
        // Headless mode doesn't support audio
        return false;
    }

    @Override
    public IAudioClip createAudioClip(String filename) {
        return null;
    }

    @Override
    public IAudioMusic createAudioMusic(String filename) {
        return null;
    }

    @Override
    public void startAltSoundSystem(String filename, boolean isSynchronized) {
        // No-op
    }

    @Override
    public void clearImageCache() {
        // No-op
    }

    @Override
    public void showSpellShop() {
        throw new UnsupportedOperationException("Spell shop not supported in headless mode");
    }

    @Override
    public void showBazaar() {
        throw new UnsupportedOperationException("Bazaar not supported in headless mode");
    }

    @Override
    public IGuiGame getNewGuiGame() {
        return null;
    }

    @Override
    public HostedMatch hostMatch() {
        return null;
    }

    @Override
    public void runBackgroundTask(String message, Runnable task) {
        task.run();
    }

    @Override
    public String encodeSymbols(String str, boolean formatReminderText) {
        return str;
    }

    @Override
    public void preventSystemSleep(boolean preventSleep) {
        // No-op
    }

    @Override
    public float getScreenScale() {
        return 1.0f;
    }

    @Override
    public UpnpServiceConfiguration getUpnpPlatformService() {
        return null;
    }
}
