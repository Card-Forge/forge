package forge;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.google.common.base.Function;

import forge.animation.GuiTimer;
import forge.assets.FBufferedImage;
import forge.assets.FDelayLoadImage;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinProp;
import forge.assets.FTextureImage;
import forge.assets.ISkinImage;
import forge.assets.ImageCache;
import forge.card.CardRenderer;
import forge.deck.Deck;
import forge.deck.FDeckViewer;
import forge.download.GuiDownloadService;
import forge.error.BugReportDialog;
import forge.interfaces.IGuiBase;
import forge.interfaces.IGuiGame;
import forge.interfaces.IGuiTimer;
import forge.item.PaperCard;
import forge.match.HostedMatch;
import forge.properties.ForgeConstants;
import forge.screens.match.MatchController;
import forge.screens.quest.QuestMenu;
import forge.screens.settings.GuiDownloader;
import forge.sound.AudioClip;
import forge.sound.AudioMusic;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.toolbox.FOptionPane;
import forge.toolbox.GuiChoose;
import forge.util.Callback;
import forge.util.FileUtil;
import forge.util.ThreadUtil;
import forge.util.WaitCallback;
import forge.util.WaitRunnable;

public class GuiMobile implements IGuiBase {
    private final String assetsDir;
    
    public GuiMobile(String assetsDir0) {
        assetsDir = assetsDir0;
    }

    @Override
    public boolean isRunningOnDesktop() {
        return Gdx.app.getType() == ApplicationType.Desktop;
    }

    @Override
    public String getCurrentVersion() {
        return Forge.CURRENT_VERSION;
    }

    @Override
    public String getAssetsDir() {
        return assetsDir;
    }

    @Override
    public void invokeInEdtLater(Runnable proc) {
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

    @Override
    public boolean isGuiThread() {
        return !ThreadUtil.isGameThread();
    }

    @Override
    public IGuiTimer createGuiTimer(Runnable proc, int interval) {
        return new GuiTimer(proc, interval);
    }

    @Override
    public ISkinImage getSkinIcon(FSkinProp skinProp) {
        if (skinProp == null) { return null; }
        return FSkin.getImages().get(skinProp);
    }

    @Override
    public ISkinImage getUnskinnedIcon(String path) {
        if (isGuiThread()) {
            return new FTextureImage(new Texture(Gdx.files.absolute(path)));
        }

        //use a delay load image to avoid an error if called from background thread
        return new FDelayLoadImage(path);
    }

    @Override
    public ISkinImage getCardArt(PaperCard card) {
        return CardRenderer.getCardArt(card);
    }

    @Override
    public ISkinImage createLayeredImage(final FSkinProp background, final String overlayFilename, final float opacity) {
        return new FBufferedImage(background.getWidth(), background.getHeight(), opacity) {
            @Override
            protected void draw(Graphics g, float w, float h) {
                g.drawImage(FSkin.getImages().get(background), 0, 0, background.getWidth(), background.getHeight());

                if (FileUtil.doesFileExist(overlayFilename)) {
                    try {
                        Texture overlay = new Texture(Gdx.files.absolute(overlayFilename));
                        if (overlay != null) {
                            g.drawImage(overlay, (background.getWidth() - overlay.getWidth()) / 2, (background.getHeight() - overlay.getHeight()) / 2, overlay.getWidth(), overlay.getHeight());
                        }
                    }
                    catch (Exception e) {}
                }

                Gdx.graphics.requestRendering(); //ensure image appears right away
            }
        };
    }

    @Override
    public void showImageDialog(final ISkinImage image, final String message, final String title) {
        new WaitCallback<Integer>() {
            @Override
            public void run() {
                FOptionPane.showMessageDialog(message, title, (FImage)image, this);
            }
        }.invokeAndWait();
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final String[] options, final int defaultOption) {
        return new WaitCallback<Integer>() {
            @Override
            public void run() {
                FOptionPane.showOptionDialog(message, title, icon == null ? null : FSkin.getImages().get(icon), options, defaultOption, this);
            }
        }.invokeAndWait();
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final String[] inputOptions) {
        return new WaitCallback<String>() {
            @Override
            public void run() {
                FOptionPane.showInputDialog(message, title, icon == null ? null : FSkin.getImages().get(icon), initialInput, inputOptions, this);
            }
        }.invokeAndWait();
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display) {
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
    public void showBugReportDialog(String title, String text, boolean showExitAppBtn) {
        BugReportDialog.show(title, text, showExitAppBtn);
    }

    @Override
    public void showCardList(final String title, final String message, final List<PaperCard> list) {
        Deck deck = new Deck(title + " - " + message);
        deck.getMain().addAllFlat(list);
        FDeckViewer.show(deck);
    }

    @Override
    public boolean showBoxedProduct(final String title, final String message, final List<PaperCard> list) {
        Deck deck = new Deck(title + " - " + message); //TODO: Make this nicer
        deck.getMain().addAllFlat(list);
        FDeckViewer.show(deck);
        return false;
    }

    @Override
    public int getAvatarCount() {
        if (FSkin.isLoaded()) {
            return FSkin.getAvatars().size();
        }
        return 0;
    }

    @Override
    public String showFileDialog(String title, String defaultDir) {
        return ForgeConstants.USER_GAMES_DIR + "Test.fgs"; //TODO: Show dialog
    }

    @Override
    public File getSaveFile(File defaultFile) {
        return defaultFile; //TODO: Show dialog
    }

    @Override
    public void download(GuiDownloadService service, Callback<Boolean> callback) {
        new GuiDownloader(service, callback);
    }

    @Override
    public void copyToClipboard(String text) {
        Forge.getClipboard().setContents(text);
    }

    @Override
    public void browseToUrl(String url) {
        Gdx.net.openURI(url);
    }

    @Override
    public IAudioClip createAudioClip(String filename) {
        return AudioClip.createClip(ForgeConstants.SOUND_DIR + filename);
    }

    @Override
    public IAudioMusic createAudioMusic(String filename) {
        return new AudioMusic(filename);
    }

    @Override
    public void startAltSoundSystem(String filename, boolean isSynchronized) {
        //TODO: Support alt sound system
    }

    @Override
    public void clearImageCache() {
        ImageCache.clear();
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
        return Forge.hostedMatch = new HostedMatch();
    }

    @Override
    public void netMessage(final String origin, final String message) {
    }
}
