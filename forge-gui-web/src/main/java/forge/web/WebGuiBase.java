package forge.web;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;
import forge.ImageKeys;
import forge.card.CardEdition;
import forge.gui.download.GuiDownloadService;
import forge.gui.interfaces.IGuiBase;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.util.BuildInfo;
import forge.util.FSerializableFunction;
import forge.util.IHasForgeLog;
import forge.util.ImageFetcher;
import forge.web.images.Foil;
import forge.web.images.WebDownloads;
import forge.web.images.WebImageFetcher;
import forge.web.protocol.ToBrowser.HostChoiceKind;
import forge.web.protocol.ToBrowser.Notice;
import forge.web.protocol.MessageJson;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Forge's interface to the program it runs in, implemented for the web. One "WebUI" thread takes the place of Swing's event thread. */
public final class WebGuiBase implements IGuiBase, IHasForgeLog {
    /** Forge asks for skin pictures, which the browser loads by itself, so every request gets this placeholder. */
    private static final ISkinImage SKIN_IMAGE = new ISkinImage() { };
    private volatile Thread uiThread;
    private final ExecutorService ui = Executors.newSingleThreadExecutor(r -> {
        // Must not start with "Game": ThreadUtil.isGameThread is a name-prefix test
        final Thread t = new Thread(r, "WebUI");
        t.setDaemon(true);
        uiThread = t;
        return t;
    });
    private final String assetsDir = resolveAssetsDir();
    private final ImageFetcher imageFetcher = new WebImageFetcher();
    private volatile Consumer<JsonObject> noticeSink = notice -> { };
    private final HostQuestions hostQuestions = new HostQuestions();

    public void setNoticeSink(final Consumer<JsonObject> sink) {
        noticeSink = sink;
        hostQuestions.setSink(sink);
    }

    HostQuestions hostQuestions() {
        return hostQuestions;
    }

    /** The working folder or the nearest parent that holds Forge's res folder, directly or in forge-gui. A git checkout runs from the repository or a module folder. */
    private static String resolveAssetsDir() {
        for (File dir = new File("").getAbsoluteFile(); dir != null; dir = dir.getParentFile()) {
            for (final File assets : new File[] {dir, new File(dir, "forge-gui")}) {
                if (new File(assets, "res").isDirectory()) {
                    return assets.getPath() + File.separator;
                }
            }
        }
        return "";
    }

    private static Runnable guarded(final Runnable r) {
        return () -> {
            try {
                r.run();
            } catch (final RuntimeException e) {
                netLog.error(e, "WebUI task failed");
            }
        };
    }

    @Override public boolean isRunningOnDesktop() { return true; }
    @Override public boolean isLibgdxPort() { return false; }
    @Override public String getCurrentVersion() { return BuildInfo.getVersionString(); }
    @Override public String getUserAgent() { return "Forge/" + getCurrentVersion() + " (gui-web)"; }
    @Override public String getAssetsDir() { return assetsDir; }
    @Override public ImageFetcher getImageFetcher() { return imageFetcher; }
    @Override public boolean isGuiThread() { return Thread.currentThread() == uiThread; }

    @Override
    public void invokeInEdtNow(final Runnable runnable) {
        if (isGuiThread()) {
            runnable.run();
        } else {
            ui.execute(guarded(runnable));
        }
    }

    @Override
    public void invokeInEdtLater(final Runnable runnable) {
        ui.execute(guarded(runnable));
    }

    @Override
    public void invokeInEdtAndWait(final Runnable proc) {
        if (isGuiThread()) {
            proc.run();
            return;
        }
        try {
            ui.submit(proc).get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public void runBackgroundTask(final String message, final Runnable task) {
        final Thread t = new Thread(guarded(task), "WebBackground");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        // A single-option dialog is a message box (SOptionPane.showMessageDialog), which needs no answer
        if (options == null || options.size() <= 1) {
            noticeSink.accept(MessageJson.encode(new Notice(title, message, icon == FSkinProp.ICO_ERROR || icon == FSkinProp.ICO_WARNING)));
            return defaultOption;
        }
        final List<Integer> answer = hostQuestions.ask(HostChoiceKind.confirm, title, message, options, 1, 1, null, null);
        if (answer != null && !answer.isEmpty()) {
            final int picked = answer.get(0);
            if (picked >= 0 && picked < options.size()) {
                return picked;
            }
        }
        netLog.warn("Host dialog answered with its default: {} / {}", title, message);
        return defaultOption;
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions, final boolean isNumeric) {
        netLog.warn("Host input dialog answered with its default: {} / {}", title, message);
        return initialInput;
    }

    /** A choice the host needs outside a match, such as a net deck category. The browser answers it. */
    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final Collection<T> selected, final FSerializableFunction<T, String> display) {
        final List<T> all = new ArrayList<>(choices);
        final List<String> options = new ArrayList<>();
        for (final T choice : all) {
            options.add(display == null ? String.valueOf(choice) : display.apply(choice));
        }
        // A bonus booster's set or a reward card is chosen by showing each set as its pack and each card as itself
        final boolean packs = !all.isEmpty() && all.stream().allMatch(CardEdition.class::isInstance);
        final boolean cards = !all.isEmpty() && all.stream().allMatch(PaperCard.class::isInstance);
        final List<String> images = packs ? all.stream().map(e -> boosterImage((CardEdition) e)).toList()
                : cards ? all.stream().map(c -> Foil.key((PaperCard) c)).toList() : null;
        final List<Integer> answer = hostQuestions.ask(HostChoiceKind.choices, null, message, options, min, max, images, packs ? "pack" : cards ? "card" : null);
        final List<T> result = new ArrayList<>();
        if (answer != null) {
            for (final int i : answer) {
                if (i >= 0 && i < all.size()) {
                    result.add(all.get(i));
                }
            }
        }
        // No usable answer, so take the first choice
        if (result.isEmpty() && min > 0 && !all.isEmpty()) {
            netLog.warn("Host choice unanswered, taking the first: {}", message);
            result.add(all.get(0));
        }
        return result;
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax, final List<T> sourceChoices, final List<T> destChoices) {
        netLog.warn("Host order dialog answered with its default: {}", title);
        return new ArrayList<>(sourceChoices);
    }

    @Override
    public PaperCard chooseCard(final String title, final String message, final List<PaperCard> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        final List<Integer> answer = hostQuestions.ask(HostChoiceKind.choices, title, message,
                list.stream().map(c -> c.getDisplayName() + " (" + c.getEdition() + ")").toList(), 1, 1,
                list.stream().map(Foil::key).toList(), "card");
        if (answer != null && !answer.isEmpty() && answer.get(0) >= 0 && answer.get(0) < list.size()) {
            return list.get(answer.get(0));
        }
        netLog.warn("Host card choice unanswered, taking the first: {}", title);
        return list.get(0);
    }

    @Override
    public void browseToUrl(final String url) throws IOException, URISyntaxException {
        final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null) {
            desktop.browse(new URI(url));
        }
    }
    @Override public boolean hasNetGame() { return false; }

    /** The image key of a set's booster picture, named as BoosterPack names its first one. */
    public static String boosterImage(final CardEdition edition) {
        return ImageKeys.BOOSTER_PREFIX + edition.getCode() + (edition.getCntBoosterPictures() > 1 ? "_1" : "");
    }

    @Override public ISkinImage getSkinIcon(final FSkinProp skinProp) { return SKIN_IMAGE; }
    @Override public ISkinImage getUnskinnedIcon(final String path) { return SKIN_IMAGE; }
    @Override public ISkinImage getCardArt(final PaperCard card, final boolean backFace) { return null; }
    @Override public ISkinImage createLayeredImage(final PaperCard card, final FSkinProp background, final String overlayFilename, final float opacity) { return null; }
    @Override public void clearImageCache() { }
    @Override public String encodeSymbols(final String str, final boolean formatReminderText) { return str; }
    @Override public int getAvatarCount() { return 0; }
    @Override public int getSleevesCount() { return 0; }
    @Override public float getScreenScale() { return 1f; }
    @Override public void preventSystemSleep(final boolean preventSleep) { }
    @Override public void download(final GuiDownloadService service, final Consumer<Boolean> callback) {
        WebDownloads.run(service, callback, noticeSink);
    }
    @Override public void copyToClipboard(final String text) { }
    @Override public void showCardList(final String title, final String message, final List<PaperCard> list) { }
    @Override public boolean showBoxedProduct(final String title, final String message, final List<PaperCard> list) { return false; }
    @Override public void showBugReportDialog(final String title, final String text, final boolean showExitAppBtn) { netLog.error("{}: {}", title, text); }
    @Override public void showImageDialog(final ISkinImage image, final String message, final String title) { }
    @Override public String showFileDialog(final String title, final String defaultDir) { return null; }
    @Override public File getSaveFile(final File defaultFile) { return null; }
    // The browser plays the audio, so this client supports the formats that the browser understands
    @Override public boolean isSupportedAudioFormat(final File file) {
        final String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".m4a");
    }
    @Override public IAudioClip createAudioClip(final String filename) { return null; }
    @Override public IAudioMusic createAudioMusic(final String filename) { return null; }
    @Override public void startAltSoundSystem(final String filename, final boolean isSynchronized) { }
    @Override public void showSpellShop() { }
    @Override public void showBazaar() { }
}
