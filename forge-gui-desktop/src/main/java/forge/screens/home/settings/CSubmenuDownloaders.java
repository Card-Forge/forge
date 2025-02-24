package forge.screens.home.settings;

import javax.swing.SwingUtilities;

import forge.control.FControl;
import forge.download.AutoUpdater;
import forge.download.GuiDownloader;
import forge.gui.ImportDialog;
import forge.gui.UiCommand;
import forge.gui.download.GuiDownloadAchievementImages;
import forge.gui.download.GuiDownloadPicturesHQ;
import forge.gui.download.GuiDownloadPicturesLQ;
import forge.gui.download.GuiDownloadPrices;
import forge.gui.download.GuiDownloadQuestImages;
import forge.gui.download.GuiDownloadSetPicturesLQ;
import forge.gui.download.GuiDownloadSkins;
import forge.gui.error.BugReporter;
import forge.gui.framework.ICDoc;
import forge.util.RSSReader;

import java.util.concurrent.CompletableFuture;

import static forge.localinstance.properties.ForgeConstants.GITHUB_COMMITS_ATOM;

/**
 * Controls the utilities submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuDownloaders implements ICDoc {
    SINGLETON_INSTANCE;

    private final UiCommand cmdLicensing = VSubmenuDownloaders.SINGLETON_INSTANCE::showLicensing;
    private final UiCommand cmdCheckForUpdates = () -> new AutoUpdater(false).attemptToUpdate(CompletableFuture.supplyAsync(() -> RSSReader.getCommitLog(GITHUB_COMMITS_ATOM, FControl.instance.getBuildTimeStamp(), FControl.instance.getSnapsTimestamp())));

    private final UiCommand cmdPicDownload = () -> new GuiDownloader(new GuiDownloadPicturesLQ()).show();
    private final UiCommand cmdPicDownloadHQ = () -> new GuiDownloader(new GuiDownloadPicturesHQ()).show();
    private final UiCommand cmdSetDownload = () -> new GuiDownloader(new GuiDownloadSetPicturesLQ()).show();
    private final UiCommand cmdQuestImages = () -> new GuiDownloader(new GuiDownloadQuestImages()).show();
    private final UiCommand cmdAchievementImages = () -> new GuiDownloader(new GuiDownloadAchievementImages()).show();
    private final UiCommand cmdDownloadPrices = () -> new GuiDownloader(new GuiDownloadPrices()).show();
    private final UiCommand cmdDownloadSkins = () -> new GuiDownloader(new GuiDownloadSkins()).show();
    private final UiCommand cmdHowToPlay = VSubmenuDownloaders.SINGLETON_INSTANCE::showHowToPlay;
    private final UiCommand cmdListImageData = VSubmenuDownloaders.SINGLETON_INSTANCE::showCardandImageAuditData;
    private final UiCommand cmdImportPictures = () -> new ImportDialog(null, null).show();
    private final UiCommand cmdReportBug = () -> BugReporter.reportBug(null);

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        final VSubmenuDownloaders view = VSubmenuDownloaders.SINGLETON_INSTANCE;
        view.setCheckForUpdatesCommand(cmdCheckForUpdates);
        view.setDownloadPicsCommand(cmdPicDownload);
        view.setDownloadPicsHQCommand(cmdPicDownloadHQ);
        view.setDownloadSetPicsCommand(cmdSetDownload);
        view.setDownloadQuestImagesCommand(cmdQuestImages);
        view.setDownloadAchievementImagesCommand(cmdAchievementImages);
        view.setListImageDataCommand(cmdListImageData);
        view.setReportBugCommand(cmdReportBug);
        view.setImportPicturesCommand(cmdImportPictures);
        view.setHowToPlayCommand(cmdHowToPlay);
        view.setDownloadPricesCommand(cmdDownloadPrices);
        view.setDownloadSkinsCommand(cmdDownloadSkins);
        view.setLicensingCommand(cmdLicensing);
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        SwingUtilities.invokeLater(VSubmenuDownloaders.SINGLETON_INSTANCE::focusTopButton);
    }

}
