package forge.screens.home.settings;

import javax.swing.SwingUtilities;

import forge.UiCommand;
import forge.download.GuiDownloadPicturesLQ;
import forge.download.GuiDownloadPrices;
import forge.download.GuiDownloadQuestImages;
import forge.download.GuiDownloadSetPicturesLQ;
import forge.download.GuiDownloader;
import forge.error.BugReporter;
import forge.gui.ImportDialog;
import forge.gui.framework.ICDoc;

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
    private final UiCommand cmdPicDownload = new UiCommand() {
        @Override public void run() {
            new GuiDownloader(new GuiDownloadPicturesLQ()).show();
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
        view.setDownloadPicsCommand(cmdPicDownload);
        view.setDownloadSetPicsCommand(cmdSetDownload);
        view.setDownloadQuestImagesCommand(cmdQuestImages);
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
