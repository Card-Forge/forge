package forge.gui.interfaces;

import forge.gamemodes.match.HostedMatch;
import forge.gui.download.GuiDownloadService;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.util.FSerializableFunction;
import forge.util.ImageFetcher;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.jupnp.UpnpServiceConfiguration;

public interface IGuiBase {
    boolean isRunningOnDesktop();
    boolean isLibgdxPort();
    String getCurrentVersion();
    String getAssetsDir();
    ImageFetcher getImageFetcher();
    void invokeInEdtNow(Runnable runnable);
    void invokeInEdtLater(Runnable runnable);
    void invokeInEdtAndWait(Runnable proc);
    boolean isGuiThread();
    ISkinImage getSkinIcon(FSkinProp skinProp);
    ISkinImage getUnskinnedIcon(String path);
    ISkinImage getCardArt(PaperCard card);
    ISkinImage getCardArt(PaperCard card, boolean backFace);
    ISkinImage createLayeredImage(PaperCard card, FSkinProp background, String overlayFilename, float opacity);
    void showBugReportDialog(String title, String text, boolean showExitAppBtn);
    void showImageDialog(ISkinImage image, String message, String title);
    int showOptionDialog(String message, String title, FSkinProp icon, List<String> options, int defaultOption);
    String showInputDialog(String message, String title, FSkinProp icon, String initialInput, List<String> inputOptions, boolean isNumeric);
    <T> List<T> getChoices(String message, int min, int max, Collection<T> choices, Collection<T> selected, FSerializableFunction<T, String> display);
    <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax, List<T> sourceChoices, List<T> destChoices);
    String showFileDialog(String title, String defaultDir);
    File getSaveFile(File defaultFile);
    void download(GuiDownloadService service, Consumer<Boolean> callback);
    void refreshSkin();
    void showCardList(String title, String message, List<PaperCard> list);
    boolean showBoxedProduct(String title, String message, List<PaperCard> list);
    PaperCard chooseCard(String title, String message, List<PaperCard> list);
    int getAvatarCount();
    int getSleevesCount();
    void copyToClipboard(String text);
    void browseToUrl(String url) throws IOException, URISyntaxException;
    boolean isSupportedAudioFormat(File file);
    IAudioClip createAudioClip(String filename);
    IAudioMusic createAudioMusic(String filename);
    void startAltSoundSystem(String filename, boolean isSynchronized);
    void clearImageCache();
    void showSpellShop();
    void showBazaar();
    IGuiGame getNewGuiGame();
    HostedMatch hostMatch();
    void runBackgroundTask(String message, Runnable task);
    String encodeSymbols(String str, boolean formatReminderText);
    void preventSystemSleep(boolean preventSleep);
    float getScreenScale();
    UpnpServiceConfiguration getUpnpPlatformService();

    /** Returns true if any currently active game is a network game. */
    boolean hasNetGame();
}