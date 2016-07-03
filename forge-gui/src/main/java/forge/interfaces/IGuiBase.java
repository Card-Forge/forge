package forge.interfaces;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;

import forge.assets.FSkinProp;
import forge.assets.ISkinImage;
import forge.download.GuiDownloadService;
import forge.item.PaperCard;
import forge.match.HostedMatch;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.util.Callback;

public interface IGuiBase {
    boolean isRunningOnDesktop();
    String getCurrentVersion();
    String getAssetsDir();
    void invokeInEdtNow(Runnable runnable);
    void invokeInEdtLater(Runnable runnable);
    void invokeInEdtAndWait(Runnable proc);
    boolean isGuiThread();
    ISkinImage getSkinIcon(FSkinProp skinProp);
    ISkinImage getUnskinnedIcon(String path);
    ISkinImage getCardArt(PaperCard card);
    ISkinImage createLayeredImage(FSkinProp background, String overlayFilename, float opacity);
    void showBugReportDialog(String title, String text, boolean showExitAppBtn);
    void showImageDialog(ISkinImage image, String message, String title);
    int showOptionDialog(String message, String title, FSkinProp icon, List<String> options, int defaultOption);
    String showInputDialog(String message, String title, FSkinProp icon, String initialInput, List<String> inputOptions);
    <T> List<T> getChoices(String message, int min, int max, Collection<T> choices, T selected, Function<T, String> display);
    <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax, List<T> sourceChoices, List<T> destChoices);
    String showFileDialog(String title, String defaultDir);
    File getSaveFile(File defaultFile);
    void download(GuiDownloadService service, Callback<Boolean> callback);
    void showCardList(String title, String message, List<PaperCard> list);
    boolean showBoxedProduct(String title, String message, List<PaperCard> list);
    PaperCard chooseCard(String title, String message, List<PaperCard> list);
    int getAvatarCount();
    void copyToClipboard(String text);
    void browseToUrl(String url) throws IOException, URISyntaxException;
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
}