package forge.screens.home.settings;

import forge.UiCommand;
import forge.download.*;
import forge.error.BugReporter;
import forge.gui.ImportDialog;
import forge.gui.framework.ICDoc;

import javax.swing.*;

/**
 * Controls the utilities submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuDownloaders implements ICDoc {
    SINGLETON_INSTANCE;

    private final UiCommand cmdLicensing = new UiCommand() {
        @Override public void run() {
            VSubmenuDownloaders.SINGLETON_INSTANCE.showLicensing();
        }
    };
    private final UiCommand cmdCheckForUpdates = new UiCommand() {
        @Override
        public void run() {
            new AutoUpdater(false).attemptToUpdate();
        }
    };

    private final UiCommand cmdPicDownload = new UiCommand() {
        @Override public void run() {
            new GuiDownloader(new GuiDownloadPicturesLQ()).show();
        }
    };
    private final UiCommand cmdPicDownloadHQ = new UiCommand() {
        @Override public void run() {
            new GuiDownloader(new GuiDownloadPicturesHQ()).show();
        }
    };
    private final UiCommand cmdSetDownload = new UiCommand() {
        @Override public void run() {
            new GuiDownloader(new GuiDownloadSetPicturesLQ()).show();
        }
    };
    private final UiCommand cmdQuestImages = new UiCommand() {
        @Override public void run() {
            new GuiDownloader(new GuiDownloadQuestImages()).show();
        }
    };
    private final UiCommand cmdAchievementImages = new UiCommand() {
        @Override public void run() {
            new GuiDownloader(new GuiDownloadAchievementImages()).show();
        }
    };
    private final UiCommand cmdDownloadPrices = new UiCommand() {
        @Override public void run() {
            new GuiDownloader(new GuiDownloadPrices()).show();
        }
    };
    private final UiCommand cmdHowToPlay = new UiCommand() {
        @Override public void run() {
            VSubmenuDownloaders.SINGLETON_INSTANCE.showHowToPlay();
        }
    };
    private final UiCommand cmdListImageData = new UiCommand() {
        @Override public void run() {
            VSubmenuDownloaders.SINGLETON_INSTANCE.showCardandImageAuditData();
        }
    };
    private final UiCommand cmdImportPictures = new UiCommand() {
        @Override public void run() {
            new ImportDialog(null, null).show();
        }
    };
    private final UiCommand cmdReportBug = new UiCommand() {
        @Override public void run() {
            BugReporter.reportBug(null);
        }
    };

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
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
        view.setLicensingCommand(cmdLicensing);
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                VSubmenuDownloaders.SINGLETON_INSTANCE.focusTopButton();
            }
        });
    }

}
