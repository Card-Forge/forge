package forge;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.sound.sampled.AudioSystem;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.UpnpServiceConfiguration;

import forge.download.GuiDownloader;
import forge.error.BugReportDialog;
import forge.gamemodes.match.HostedMatch;
import forge.gui.BoxedProductCardListViewer;
import forge.gui.CardListChooser;
import forge.gui.CardListViewer;
import forge.gui.FThreads;
import forge.gui.GuiChoose;
import forge.gui.download.GuiDownloadService;
import forge.gui.framework.FScreen;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorQuestCardShop;
import forge.screens.match.CMatchUI;
import forge.sound.AltSoundSystem;
import forge.sound.AudioClip;
import forge.sound.AudioMusic;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.util.BuildInfo;
import forge.util.FSerializableFunction;
import forge.util.FileUtil;
import forge.util.ImageFetcher;
import forge.util.OperatingSystem;
import forge.util.SwingImageFetcher;

public class GuiDesktop implements IGuiBase {
    private ImageFetcher imageFetcher = new SwingImageFetcher();

    @Override
    public UpnpServiceConfiguration getUpnpPlatformService() {
        return new DefaultUpnpServiceConfiguration();
    }

    @Override
    public boolean isRunningOnDesktop() {
        return true;
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
        return StringUtils.containsIgnoreCase(BuildInfo.getVersionString(), "git") ?
                // FIXME: replace this hardcoded value!!
                "../forge-gui/" : "";
    }

    @Override
    public ImageFetcher getImageFetcher() {
        return imageFetcher;
    }

    @Override
    public void invokeInEdtNow(final Runnable proc) {
        proc.run();
    }

    @Override
    public void invokeInEdtLater(final Runnable proc) {
        SwingUtilities.invokeLater(proc);
    }

    @Override
    public void invokeInEdtAndWait(final Runnable proc) {
        if (SwingUtilities.isEventDispatchThread()) {
            // Just run in the current thread.
            proc.run();
        }
        else {
            try {
                SwingUtilities.invokeAndWait(proc);
            }
            catch (final InterruptedException | InvocationTargetException exn) {
                throw new RuntimeException(exn);
            }
        }
    }

    @Override
    public boolean isGuiThread() {
        return SwingUtilities.isEventDispatchThread();
    }

    @Override
    public ISkinImage getSkinIcon(final FSkinProp skinProp) {
        if (skinProp == null) { return null; }
        return FSkin.getIcon(skinProp);
    }

    @Override
    public ISkinImage getUnskinnedIcon(final String path) {
        return new FSkin.UnskinnedIcon(path);
    }

    @Override
    public ISkinImage getCardArt(final PaperCard card) {
        return null; //TODO
    }

    @Override
    public ISkinImage getCardArt(final PaperCard card, final boolean backFace) {
        return null; //TODO
    }

    @Override
    public ISkinImage createLayeredImage(final PaperCard paperCard, final FSkinProp background, final String overlayFilename, final float opacity) {
        final BufferedImage image = new BufferedImage(background.getWidth(), background.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        final FSkin.SkinImage backgroundImage = FSkin.getImage(background);
        FSkin.drawImage(g, backgroundImage, 0, 0, background.getWidth(), background.getHeight());
        final int cardImageWidth = 90;
        final int cardImageHeight = 128;

        if (FileUtil.doesFileExist(overlayFilename)) {
            final ImageIcon overlay = new ImageIcon(overlayFilename);
            g.drawImage(overlay.getImage(), (background.getWidth() - overlay.getIconWidth()) / 2, (background.getHeight() - overlay.getIconHeight()) / 2, overlay.getIconWidth(), overlay.getIconHeight(), null);
        } else if (paperCard != null) {
            BufferedImage cardImage = ImageCache.scaleImage(paperCard.getCardImageKey(), cardImageWidth, cardImageHeight, false, null);
            if (cardImage != null)
                g.drawImage(cardImage, (background.getWidth() - cardImageWidth) / 2, (background.getHeight() - cardImageHeight) / 4, cardImageWidth, cardImageHeight, null);
        }
        return new FSkin.UnskinnedIcon(image, opacity);
    }

    @Override
    public void showImageDialog(final ISkinImage image, final String message, final String title) {
        FOptionPane.showMessageDialog(message, title, (SkinImage)image);
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        return FOptionPane.showOptionDialog(message, title, icon == null ? null : FSkin.getImage(icon), options, defaultOption);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions, boolean isNumeric) {
        return FOptionPane.showInputDialog(message, title, icon == null ? null : FSkin.getImage(icon), initialInput, inputOptions);
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final Collection<T> selected, final FSerializableFunction<T, String> display) {
        /*if ((choices != null && !choices.isEmpty() && choices.iterator().next() instanceof GameObject) || selected instanceof GameObject) {
            System.err.println("Warning: GameObject passed to GUI! Printing stack trace.");
            Thread.dumpStack();
        }*/
        return GuiChoose.getChoices(message, min, max, choices, selected, display);
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices) {
        /*if ((sourceChoices != null && !sourceChoices.isEmpty() && sourceChoices.iterator().next() instanceof GameObject)
                || (destChoices != null && !destChoices.isEmpty() && destChoices.iterator().next() instanceof GameObject)) {
            System.err.println("Warning: GameObject passed to GUI! Printing stack trace.");
            Thread.dumpStack();
        }*/
        return GuiChoose.order(title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices);
    }

    @Override
    public void showCardList(final String title, final String message, final List<PaperCard> list) {
        final CardListViewer cardView = new CardListViewer(title, message, list);
        cardView.setVisible(true);
        cardView.dispose();
    }

    @Override
    public boolean showBoxedProduct(final String title, final String message, final List<PaperCard> list) {
        final BoxedProductCardListViewer viewer = new BoxedProductCardListViewer(title, message, list);
        viewer.setVisible(true);
        viewer.dispose();
        return viewer.skipTheRest();
    }

    @Override
    public PaperCard chooseCard(String title, String message, List<PaperCard> list) {
        final CardListChooser cardListChooser = new CardListChooser(title, message, list);
        cardListChooser.setVisible(true);
        cardListChooser.dispose();
        return cardListChooser.getSelectedCard();
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
        final JFileChooser fc = new JFileChooser(defaultDir);
        final int rc = fc.showDialog(null, title);
        if (rc != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return fc.getSelectedFile().getAbsolutePath();
    }

    @Override
    public void showBugReportDialog(final String title, final String text, final boolean showExitAppBtn) {
        BugReportDialog.show(title, text, showExitAppBtn);
    }

    @Override
    public File getSaveFile(final File defaultFile) {
        final JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(defaultFile);
        int result = fc.showSaveDialog(null);
        return result == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
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
        final StringSelection ss = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
    }

    @Override
    public void browseToUrl(final String url) throws IOException, URISyntaxException {
        Desktop.getDesktop().browse(new URI(url));
    }

    @Override
    public boolean isSupportedAudioFormat(File file) {
        try {
            return AudioSystem.getAudioFileFormat(file) != null;
        }
        catch (Exception e) {
            System.err.printf("Unable to open audio resource '%s': %s\n", file.getPath(), e.getMessage());
            return false;
        }
    }

    @Override
    public IAudioClip createAudioClip(final String filename) {
        return AudioClip.fileExists(filename) ? new AudioClip(filename) : null;
    }

    @Override
    public IAudioMusic createAudioMusic(final String filename) {
        return new AudioMusic(filename);
    }

    @Override
    public void startAltSoundSystem(final String filename, final boolean isSynchronized) {
        new AltSoundSystem(filename, isSynchronized).start();
    }

    @Override
    public void clearImageCache() {
        ImageCache.clear();
        ImageKeys.clearMissingCards();
    }

    @Override
    public void showSpellShop() {
        Singletons.getControl().setCurrentScreen(FScreen.QUEST_CARD_SHOP);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                new CEditorQuestCardShop(FModel.getQuest(), CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture()));
    }

    @Override
    public void showBazaar() {
        Singletons.getControl().setCurrentScreen(FScreen.QUEST_BAZAAR);
        FScreen.QUEST_BAZAAR.getController().update();
        Singletons.getView().getFrame().validate();
    }

    @Override
    public IGuiGame getNewGuiGame() {
        return new CMatchUI();
    }

    @Override
    public HostedMatch hostMatch() {
        final HostedMatch match = new HostedMatch();
        Singletons.getControl().addMatch(match);
        return match;
    }

    @Override
    public void runBackgroundTask(String message, Runnable task) {
        //TODO: Show loading overlay
        FThreads.invokeInBackgroundThread(task);
    }

    @Override
    public String encodeSymbols(String str, boolean formatReminderText) {
        return FSkin.encodeSymbols(str, formatReminderText);
    }

    @Override
    public void preventSystemSleep(boolean preventSleep) {
        OperatingSystem.preventSystemSleep(preventSleep);
    }

    private static float initializeScreenScale() {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        AffineTransform at = gc.getDefaultTransform();
        double scaleX = at.getScaleX();
        double scaleY = at.getScaleY();
        return (float) Math.min(scaleX, scaleY);
    }
    static float screenScale = initializeScreenScale();

    @Override
    public boolean hasNetGame() {
        if (Singletons.getView() == null || Singletons.getView().getNavigationBar() == null) {
            return false;
        }
        return Singletons.getView().getNavigationBar().hasNetGame();
    }

    @Override
    public float getScreenScale() {
        return screenScale;
    }
}
