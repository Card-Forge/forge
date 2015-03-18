package forge;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;

import forge.assets.FSkinProp;
import forge.assets.ISkinImage;
import forge.control.GuiTimer;
import forge.download.GuiDownloadService;
import forge.download.GuiDownloader;
import forge.error.BugReportDialog;
import forge.gui.BoxedProductCardListViewer;
import forge.gui.CardListViewer;
import forge.gui.GuiChoose;
import forge.gui.framework.FScreen;
import forge.interfaces.IGuiBase;
import forge.interfaces.IGuiGame;
import forge.interfaces.IGuiTimer;
import forge.item.PaperCard;
import forge.match.HostedMatch;
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
import forge.util.Callback;
import forge.util.FileUtil;

public class GuiDesktop implements IGuiBase {
    @Override
    public boolean isRunningOnDesktop() {
        return true;
    }

    @Override
    public String getCurrentVersion() {
        return BuildInfo.getVersionString();
    }

    @Override
    public String getAssetsDir() {
        return StringUtils.containsIgnoreCase(BuildInfo.getVersionString(), "svn") ?
                "../forge-gui/" : "";
    }

    @Override
    public void invokeInEdtLater(Runnable proc) {
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
            catch (final InterruptedException exn) {
                throw new RuntimeException(exn);
            }
            catch (final InvocationTargetException exn) {
                throw new RuntimeException(exn);
            }
        }
    }

    @Override
    public boolean isGuiThread() {
        return SwingUtilities.isEventDispatchThread();
    }

    @Override
    public IGuiTimer createGuiTimer(Runnable proc, int interval) {
        return new GuiTimer(proc, interval);
    }

    @Override
    public ISkinImage getSkinIcon(FSkinProp skinProp) {
        if (skinProp == null) { return null; }
        return FSkin.getIcon(skinProp);
    }

    @Override
    public ISkinImage getUnskinnedIcon(String path) {
        return new FSkin.UnskinnedIcon(path);
    }

    @Override
    public ISkinImage getCardArt(PaperCard card) {
        return null; //TODO
    }

    @Override
    public ISkinImage createLayeredImage(FSkinProp background, String overlayFilename, float opacity) {
        BufferedImage image = new BufferedImage(background.getWidth(), background.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        FSkin.SkinImage backgroundImage = FSkin.getImage(background);
        FSkin.drawImage(g, backgroundImage, 0, 0, background.getWidth(), background.getHeight());

        if (FileUtil.doesFileExist(overlayFilename)) {
            try {
                ImageIcon overlay = new ImageIcon(overlayFilename);
                if (overlay != null) {
                    g.drawImage(overlay.getImage(), (background.getWidth() - overlay.getIconWidth()) / 2, (background.getHeight() - overlay.getIconHeight()) / 2, overlay.getIconWidth(), overlay.getIconHeight(), null);
                }
            }
            catch (Exception e) {}
        }
        return new FSkin.UnskinnedIcon(image, opacity);
    }

    @Override
    public void showImageDialog(ISkinImage image, String message, String title) {
        FOptionPane.showMessageDialog(message, title, (SkinImage)image);
    }

    @Override
    public int showOptionDialog(String message, String title, FSkinProp icon, String[] options, int defaultOption) {
        return FOptionPane.showOptionDialog(message, title, icon == null ? null : FSkin.getImage(icon), options, defaultOption);
    }

    @Override
    public String showInputDialog(String message, String title, FSkinProp icon, String initialInput, String[] inputOptions) {
        return FOptionPane.showInputDialog(message, title, icon == null ? null : FSkin.getImage(icon), initialInput, inputOptions);
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display) {
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
    public int getAvatarCount() {
        if (FSkin.isLoaded()) {
            return FSkin.getAvatars().size();
        }
        return 0;
    }

    @Override
    public String showFileDialog(String title, String defaultDir) {
        final JFileChooser fc = new JFileChooser(defaultDir);
        final int rc = fc.showDialog(null, title);
        if (rc != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return fc.getSelectedFile().getAbsolutePath();
    }

    @Override
    public void showBugReportDialog(String title, String text, boolean showExitAppBtn) {
        BugReportDialog.show(title, text, showExitAppBtn);
    }

    @Override
    public File getSaveFile(File defaultFile) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(defaultFile);
        fc.showSaveDialog(null);
        return fc.getSelectedFile();
    }

    @Override
    public void download(GuiDownloadService service, Callback<Boolean> callback) {
        new GuiDownloader(service, callback);
    }

    @Override
    public void copyToClipboard(String text) {
        StringSelection ss = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
    }

    @Override
    public void browseToUrl(String url) throws IOException, URISyntaxException {
        Desktop.getDesktop().browse(new URI(url));
    }

    @Override
    public IAudioClip createAudioClip(String filename) {
        return AudioClip.fileExists(filename) ? new AudioClip(filename) : null;
    }

    @Override
    public IAudioMusic createAudioMusic(String filename) {
        return new AudioMusic(filename);
    }

    @Override
    public void startAltSoundSystem(String filename, boolean isSynchronized) {
        new AltSoundSystem(filename, isSynchronized).start();
    }

    @Override
    public void clearImageCache() {
        ImageCache.clear();
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
        Singletons.getView().getFrame().validate();
    }

    public IGuiGame getNewGuiGame() {
        return new CMatchUI();
    }

    @Override
    public HostedMatch hostMatch() {
        final HostedMatch match = new HostedMatch();
        Singletons.getControl().addMatch(match);
        return match;
    }

}