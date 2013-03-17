package forge.gui.home.settings;

import javax.swing.SwingUtilities;

import forge.Command;
import forge.error.BugReporter;
import forge.gui.DialogMigrateProfile;
import forge.gui.download.GuiDownloadPicturesLQ;
import forge.gui.download.GuiDownloadPrices;
import forge.gui.download.GuiDownloadQuestImages;
import forge.gui.download.GuiDownloadSetPicturesLQ;
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

    private final Command cmdLicensing = new Command() { @Override
        public void execute() { VSubmenuDownloaders.SINGLETON_INSTANCE.showLicensing(); } };
    private final Command cmdPicDownload  = new Command() { @Override
        public void execute() { new GuiDownloadPicturesLQ(); } };
    private final Command cmdSetDownload = new Command() { @Override
        public void execute() { new GuiDownloadSetPicturesLQ(); } };
    private final Command cmdQuestImages = new Command() { @Override
        public void execute() { new GuiDownloadQuestImages(); } };
    private final Command cmdDownloadPrices = new Command() { @Override
        public void execute() { new GuiDownloadPrices(); } };
    private final Command cmdHowToPlay = new Command() { @Override
        public void execute() { VSubmenuDownloaders.SINGLETON_INSTANCE.showHowToPlay(); } };
    private final Command cmdImportPictures = new Command() { @Override
        public void execute() { new DialogMigrateProfile(null, null); } };
    private final Command cmdReportBug = new Command() { @Override
        public void execute() { BugReporter.reportBug(null); }
    };

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

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
