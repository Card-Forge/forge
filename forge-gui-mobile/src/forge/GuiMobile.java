package forge;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import forge.adventure.stage.MapStage;
import forge.assets.*;
import forge.card.CardRenderer;
import forge.deck.Deck;
import forge.deck.FDeckViewer;
import forge.error.BugReportDialog;
import forge.gamemodes.match.HostedMatch;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.download.GuiDownloadService;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.screens.LoadingOverlay;
import forge.screens.match.MatchController;
import forge.screens.quest.QuestMenu;
import forge.screens.settings.GuiDownloader;
import forge.sound.*;
import forge.toolbox.FOptionPane;
import forge.toolbox.GuiChoose;
import forge.util.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.UpnpServiceConfiguration;

public class GuiMobile implements IGuiBase {
    private final String assetsDir;
    private final ImageFetcher imageFetcher = new LibGDXImageFetcher();
    private final List<Integer> integerChoices = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    public GuiMobile(final String assetsDir0) {
        assetsDir = assetsDir0;
    }

    @Override
    public UpnpServiceConfiguration getUpnpPlatformService() {
        if (GuiBase.isAndroid()) {
            return Forge.getDeviceAdapter().getUpnpPlatformService();
        }
        return new DefaultUpnpServiceConfiguration();
    }

    @Override
    public boolean isRunningOnDesktop() {
        return Gdx.app == null || Gdx.app.getType() == ApplicationType.Desktop;
    }

    @Override
    public boolean isLibgdxPort() {
        return true;
    }

    @Override
    public String getCurrentVersion() {
        return Forge.getDeviceAdapter().getVersionString();
    }

    @Override
    public String getAssetsDir() {
        return assetsDir;
    }

    @Override
    public ImageFetcher getImageFetcher() {
        return imageFetcher;
    }

    @Override
    public void invokeInEdtNow(final Runnable proc) {
        proc.run();
        Gdx.graphics.requestRendering(); //must request rendering in case this procedure wasn't triggered by a local event
    }

    @Override
    public void invokeInEdtLater(final Runnable proc) {
        Gdx.app.postRunnable(proc);
    }

    @Override
    public void invokeInEdtAndWait(final Runnable proc) {
        if (isGuiThread()) {
            proc.run();
        }
        else {
            new WaitRunnable() {
                @Override
                public void run() {
                    proc.run();
                }
            }.invokeAndWait();
        }
    }

    private volatile Thread glThread;

    void captureGlThread() {
        this.glThread = Thread.currentThread();
    }

    @Override
    public boolean isGuiThread() {
        return Thread.currentThread() == glThread;
    }

    @Override
    public ISkinImage getSkinIcon(final FSkinProp skinProp) {
        if (skinProp == null) { return null; }
        return FSkin.getImages().get(skinProp);
    }

    @Override
    public ISkinImage getUnskinnedIcon(final String path) {
        if (isGuiThread()) {
            return new FTextureImage(Forge.getAssets().getTexture(Gdx.files.absolute(path)));
        }

        //use a delay load image to avoid an error if called from background thread
        return new FDelayLoadImage(path);
    }

    @Override
    public ISkinImage getCardArt(final PaperCard card) {
        return CardRenderer.getCardArt(card);
    }

    @Override
    public ISkinImage getCardArt(final PaperCard card, final boolean backFace) {
        return CardRenderer.getCardArt(card, backFace);
    }

    @Override
    public ISkinImage createLayeredImage(final PaperCard paperCard, final FSkinProp background, final String overlayFilename, final float opacity) {
        return new FBufferedImage(background.getWidth(), background.getHeight(), opacity) {
            @Override
            protected void draw(final Graphics g, final float w, final float h) {
                g.drawImage(FSkin.getImages().get(background), 0, 0, background.getWidth(), background.getHeight());
                final float cardImageWidth = 90f;
                final float cardImageHeight = 128f;

                if (FileUtil.doesFileExist(overlayFilename)) {
                    try {
                        final Texture overlay = Forge.getAssets().getTexture(Gdx.files.absolute(overlayFilename));
                        g.drawImage(overlay, (background.getWidth() - overlay.getWidth()) / 2f, (background.getHeight() - overlay.getHeight()) / 2f, overlay.getWidth(), overlay.getHeight());
                    } catch (Exception ignored) {
                    }
                } else if (paperCard != null) {
                    Texture cardImage = ImageCache.getInstance().getImage(paperCard.getCardImageKey(), false);
                    if (cardImage != null)
                        g.drawCardRoundRect(cardImage, null, (background.getWidth() - cardImageWidth) / 2, (background.getHeight() - cardImageHeight) / 3.8f, cardImageWidth, cardImageHeight, false, false, paperCard.isFoil());
                }

                Gdx.graphics.requestRendering(); //ensure image appears right away
            }
        };
    }

    @Override
    public void showImageDialog(final ISkinImage image, final String message, final String title) {
        if (Forge.isMobileAdventureMode) {
            FThreads.invokeInEdtNowOrLater(() -> MapStage.getInstance().showImageDialog("Achievement Earned\n"+message, (FBufferedImage)image, null));
            return;
        }
        new WaitCallback<Integer>() {
            @Override
            public void run() {
                FOptionPane.showMessageDialog(message, title, (FImage)image, this);
            }
        }.invokeAndWait();
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        return new WaitCallback<Integer>() {
            @Override
            public void run() {
                FOptionPane.showOptionDialog(message, title, icon == null ? null : FSkin.getImages().get(icon), options, defaultOption, this);
            }
        }.invokeAndWait();
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions, boolean isNumeric) {
        return new WaitCallback<String>() {
            @Override
            public void run() {
                FOptionPane.showInputDialog(message, title, initialInput, inputOptions, this, isNumeric);
            }
        }.invokeAndWait();
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final Collection<T> selected, final FSerializableFunction<T, String> display) {
        return new WaitCallback<List<T>>() {
            @Override
            public void run() {
                GuiChoose.getChoices(message, min, max, choices, selected, display, this);
            }
        }.invokeAndWait();
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices) {
        return new WaitCallback<List<T>>() {
            @Override
            public void run() {
                GuiChoose.order(title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, null, this);
            }
        }.invokeAndWait();
    }

    @Override
    public void showBugReportDialog(final String title, final String text, final boolean showExitAppBtn) {
        BugReportDialog.show(title, text, showExitAppBtn);
    }

    @Override
    public void showCardList(final String title, final String message, final List<PaperCard> list) {
        final Deck deck = new Deck(title + " - " + message);
        deck.getMain().addAllFlat(list);
        FDeckViewer.show(deck, true);
    }

    @Override
    public boolean showBoxedProduct(final String title, final String message, final List<PaperCard> list) {
        final Deck deck = new Deck(title + " - " + message); //TODO: Make this nicer
        deck.getMain().addAllFlat(list);
        FDeckViewer.show(deck);
        return false;
    }

    @Override
    public PaperCard chooseCard(final String title, final String message, final List<PaperCard> list) {
        return new WaitCallback<PaperCard>() {
            @Override
            public void run() {
                GuiChoose.one(title + " - " + message, list, this);
            }
        }.invokeAndWait();
    }

    @Override
    public int getAvatarCount() {
        if (FSkin.isLoaded()) {
            return FSkin.getAvatars().size();
        }
        return 0;
    }

    @Override
    public int getSleevesCount() {
        if (FSkin.isLoaded()) {
            return FSkin.getSleeves().size();
        }
        return 0;
    }

    @Override
    public String showFileDialog(final String title, final String defaultDir) {
        //TODO Android FilePicker varies, since we cant test all possible android versions, just return a selection..
        List<Integer> v = getChoices(title, 0, 1, integerChoices, null, null);
        if (v == null || v.isEmpty())
            return null;
        return defaultDir + "state" + v.get(0) + ".txt";
    }

    @Override
    public File getSaveFile(final File defaultFile) {
        //TODO Android FilePicker varies, since we cant test all possible android versions, just return a selection..
        List<Integer> v = getChoices(Localizer.getInstance().getMessage("lblSelectGameStateFile"), 0, 1, integerChoices, null, null);
        if (v == null || v.isEmpty())
            return null;
        return new File(ForgeConstants.USER_GAMES_DIR + "state" + v.get(0) + ".txt");
    }

    @Override
    public void download(final GuiDownloadService service, final Consumer<Boolean> callback) {
        new GuiDownloader(service, callback).show();
    }

    @Override
    public void refreshSkin() {
        //todo refresh skin selector
    }

    @Override
    public void copyToClipboard(final String text) {
        Forge.getClipboard().setContents(text);
    }

    @Override
    public void browseToUrl(final String url) {
        Gdx.net.openURI(url);
    }

    @Override
    public boolean isSupportedAudioFormat(File file) {
        return Forge.getDeviceAdapter().isSupportedAudioFormat(file);
    }

    @Override
    public IAudioClip createAudioClip(final String filename) {
        return AudioClip.createClip(SoundSystem.instance.getSoundResource(filename));
    }

    @Override
    public IAudioMusic createAudioMusic(final String filename) {
        return new AudioMusic(filename);
    }

    @Override
    public void startAltSoundSystem(final String filename, final boolean isSynchronized) {
        //TODO: Support alt sound system
    }

    @Override
    public void clearImageCache() {
        ImageCache.getInstance().clear();
        ImageKeys.clearMissingCards();
    }

    @Override
    public void showSpellShop() {
        QuestMenu.showSpellShop();
    }

    @Override
    public void showBazaar() {
        QuestMenu.showBazaar();
    }

    @Override
    public IGuiGame getNewGuiGame() {
        return MatchController.instance;
    }

    @Override
    public HostedMatch hostMatch() {
        return MatchController.hostMatch();
    }

    @Override
    public void runBackgroundTask(String message, Runnable task) {
        LoadingOverlay.runBackgroundTask(message, task);
    }

    @Override
    public String encodeSymbols(String str, boolean formatReminderText) {
        return str; //not needed for mobile
    }

    @Override
    public void preventSystemSleep(boolean preventSleep) {
        Forge.getDeviceAdapter().preventSystemSleep(preventSleep);
    }

    @Override
    public boolean hasNetGame() {
        return MatchController.instance.isNetGame();
    }

    @Override
    public float getScreenScale() {
        return 1f;
    }
}
