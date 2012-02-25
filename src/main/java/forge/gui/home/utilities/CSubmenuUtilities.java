package forge.gui.home.utilities;

import forge.Command;
import forge.GuiDownloadPicturesLQ;
import forge.GuiDownloadPrices;
import forge.GuiDownloadQuestImages;
import forge.GuiDownloadSetPicturesLQ;
import forge.GuiImportPicture;
import forge.error.BugzReporter;
import forge.gui.home.ICSubmenu;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuUtilities implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    private final Command cmdLicensing = new Command() { @Override
        public void execute() { VSubmenuUtilities.SINGLETON_INSTANCE.showLicensing(); } };
    private final Command cmdPicDownload  = new Command() { @Override
        public void execute() { new GuiDownloadPicturesLQ(null); } };
    private final Command cmdSetDownload = new Command() { @Override
        public void execute() { new GuiDownloadSetPicturesLQ(null); } };
    private final Command cmdQuestImages = new Command() { @Override
        public void execute() { new GuiDownloadQuestImages(null); } };
    private final Command cmdHowToPlay = new Command() { @Override
        public void execute() { VSubmenuUtilities.SINGLETON_INSTANCE.showHowToPlay(); } };

    private final Command cmdDownloadPrices = new Command() {
        @Override
        public void execute() {
            final GuiDownloadPrices gdp = new GuiDownloadPrices();
            gdp.setVisible(true);
        }
    };

    private final Command cmdImportPictures = new Command() {
        @Override
        public void execute() {
            final GuiImportPicture ip = new GuiImportPicture(null);
            ip.setVisible(true);
        }
    };

    private final Command cmdReportBug = new Command() { @Override
        public void execute() {
        final BugzReporter br = new BugzReporter();
        br.setVisible(true);
        }
    };

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        VSubmenuUtilities.SINGLETON_INSTANCE.populate();
        CSubmenuUtilities.SINGLETON_INSTANCE.update();

        final VSubmenuUtilities view = VSubmenuUtilities.SINGLETON_INSTANCE;
        view.getBtnDownloadPics().setCommand(cmdPicDownload);
        view.getBtnDownloadSetPics().setCommand(cmdSetDownload);
        view.getBtnDownloadQuestImages().setCommand(cmdQuestImages);
        view.getBtnReportBug().setCommand(cmdReportBug);
        view.getBtnImportPictures().setCommand(cmdImportPictures);
        view.getBtnHowToPlay().setCommand(cmdHowToPlay);
        view.getBtnDownloadPrices().setCommand(cmdDownloadPrices);
        view.getBtnLicensing().setCommand(cmdLicensing);
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getCommand()
     */
    @Override
    public Command getMenuCommand() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() { }
}
