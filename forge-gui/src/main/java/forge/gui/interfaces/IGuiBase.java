package forge.gui.interfaces;

import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.tutorial.TutorialLayout;
import forge.gamemodes.tutorial.TutorialTarget;
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
    String getUserAgent();

    void invokeInEdtNow(Runnable runnable);
    void invokeInEdtLater(Runnable runnable);
    void invokeInEdtAndWait(Runnable proc);
    void runBackgroundTask(String message, Runnable task);
    boolean isGuiThread();
    default boolean isMatchInProgress() { return false; }

    /**
     * Shows a tutorial note on the match screen without dimming or blocking it, so the cards it talks about stay in
     * view. {@code onNext} runs when the player presses the note's button. Returns false when this GUI has no such
     * panel, in which case the caller shows a dialog instead. Safe to call from any thread.
     */
    default boolean showCoach(final String title, final String text, final String nextLabel, final Runnable onNext) {
        return false;
    }
    /** Removes the note shown by {@link #showCoach}, if any. Safe to call from any thread. */
    default void hideCoach() { }
    /**
     * Which match screen layout is on screen now, so the tutorial can say where things are on this player's screen
     * (desktop, mobile upright, mobile sideways, the compact layouts).
     */
    default TutorialLayout getTutorialLayout() { return TutorialLayout.DESKTOP; }
    /**
     * Points the player at the given parts of the match screen: each is outlined and the rest of the screen is dimmed,
     * and the tutorial note moves out of their way. A part this layout does not have is ignored, and an empty
     * collection clears the highlight. Where a part is folded away (the phase stops of the compact layout) it is
     * opened for as long as it is highlighted. Safe to call from any thread.
     */
    default void highlightGui(final Collection<TutorialTarget> targets) { }
    String getAssetsDir();
    ImageFetcher getImageFetcher();
    ISkinImage getSkinIcon(FSkinProp skinProp);
    ISkinImage getUnskinnedIcon(String path);
    default ISkinImage getCardArt(PaperCard card) { return getCardArt(card, false); }
    ISkinImage getCardArt(PaperCard card, boolean backFace);
    ISkinImage createLayeredImage(PaperCard card, FSkinProp background, String overlayFilename, float opacity);

    void clearImageCache();
    String encodeSymbols(String str, boolean formatReminderText);

    int getAvatarCount();
    int getSleevesCount();

    float getScreenScale();
    void preventSystemSleep(boolean preventSleep);
    void download(GuiDownloadService service, Consumer<Boolean> callback);
    void copyToClipboard(String text);
    void browseToUrl(String url) throws IOException, URISyntaxException;

    void showCardList(String title, String message, List<PaperCard> list);
    boolean showBoxedProduct(String title, String message, List<PaperCard> list);
    void showBugReportDialog(String title, String text, boolean showExitAppBtn);
    void showImageDialog(ISkinImage image, String message, String title);
    int showOptionDialog(String message, String title, FSkinProp icon, List<String> options, int defaultOption);
    String showInputDialog(String message, String title, FSkinProp icon, String initialInput, List<String> inputOptions, boolean isNumeric);
    String showFileDialog(String title, String defaultDir);
    File getSaveFile(File defaultFile);
    <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax, List<T> sourceChoices, List<T> destChoices);
    <T> List<T> getChoices(String message, int min, int max, Collection<T> choices, Collection<T> selected, FSerializableFunction<T, String> display);
    PaperCard chooseCard(String title, String message, List<PaperCard> list);

    boolean isSupportedAudioFormat(File file);
    IAudioClip createAudioClip(String filename);
    IAudioMusic createAudioMusic(String filename);
    void startAltSoundSystem(String filename, boolean isSynchronized);

    void showSpellShop();
    void showBazaar();

    IGuiGame getNewGuiGame();
    HostedMatch hostMatch();

    UpnpServiceConfiguration getUpnpPlatformService();
    /** Returns true if any currently active game is a network game. */
    boolean hasNetGame();

    default void vibrate(int milliseconds, int amplitude) {}
    default void vibrateController(int milliseconds, float amplitude) {}
    default boolean useControllerForHaptics() { return false; }
}