package forge.control.home;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import forge.AllZone;
import forge.Command;
import forge.GuiDownloadPicturesLQ;
import forge.GuiDownloadPrices;
import forge.GuiDownloadQuestImages;
import forge.GuiDownloadSetPicturesLQ;
import forge.GuiImportPicture;
import forge.Singletons;
import forge.error.BugzReporter;
import forge.game.GameType;
import forge.gui.deckeditor.DeckEditorBase;
import forge.gui.deckeditor.DeckEditorConstructed;
import forge.gui.deckeditor.DeckEditorLimited;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang;
import forge.view.home.ViewUtilities;
import forge.view.toolbox.FSkin;

/** 
 * Controls logic and listeners for Utilities panel of the home screen.
 *
 */
public class ControlUtilities {
    private ViewUtilities view;
    private final MouseListener madLicensing;
    private final Command cmdDeckEditor, cmdPicDownload, cmdSetDownload,
        cmdQuestImages, cmdReportBug, cmdImportPictures, cmdHowToPlay, cmdDownloadPrices;

    /**
     * 
     * Controls logic and listeners for Utilities panel of the home screen.
     * 
     * @param v0 &emsp; ViewUtilities
     */
    @SuppressWarnings("serial")
    public ControlUtilities(ViewUtilities v0) {
        this.view = v0;

        madLicensing = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                view.showLicensing();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                view.getLblLicensing().setForeground(FSkin.getColor(FSkin.Colors.CLR_HOVER));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                view.getLblLicensing().setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            }
        };

        cmdDeckEditor = new Command() { @Override
            public void execute() { showDeckEditor(GameType.Constructed, null); } };

        cmdPicDownload = new Command() { @Override
            public void execute() { doDownloadPics(); } };

        cmdSetDownload = new Command() { @Override
            public void execute() { doDownloadSetPics(); } };

        cmdQuestImages = new Command() { @Override
            public void execute() { doDownloadQuestImages(); } };

        cmdReportBug = new Command() {
            @Override
            public void execute() {
                final BugzReporter br = new BugzReporter();
                br.setVisible(true);
            }
        };

        cmdImportPictures = new Command() {
            @Override
            public void execute() {
                final GuiImportPicture ip = new GuiImportPicture(null);
                ip.setVisible(true);
            }
        };

        cmdHowToPlay = new Command() {
            @Override
            public void execute() {
                final String text = ForgeProps.getLocalized(Lang.HowTo.MESSAGE);

                final JTextArea area = new JTextArea(text, 25, 40);
                area.setWrapStyleWord(true);
                area.setLineWrap(true);
                area.setEditable(false);
                area.setOpaque(false);

                JOptionPane.showMessageDialog(null, new JScrollPane(area), ForgeProps.getLocalized(Lang.HowTo.TITLE),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };

        cmdDownloadPrices = new Command() {
            @Override
            public void execute() {
                final GuiDownloadPrices gdp = new GuiDownloadPrices();
                gdp.setVisible(true);
            }
        };

        addListeners();
    }

    /** @return ViewUtilities */
    public ViewUtilities getView() {
        return view;
    }

    /** */
    public void addListeners() {
        this.view.getBtnDownloadPics().setCommand(cmdPicDownload);
        this.view.getBtnDownloadSetPics().setCommand(cmdSetDownload);
        this.view.getBtnDownloadQuestImages().setCommand(cmdQuestImages);
        this.view.getBtnReportBug().setCommand(cmdReportBug);
        this.view.getBtnImportPictures().setCommand(cmdImportPictures);
        this.view.getBtnHowToPlay().setCommand(cmdHowToPlay);
        this.view.getBtnDownloadPrices().setCommand(cmdDownloadPrices);
        this.view.getBtnDeckEditor().setCommand(cmdDeckEditor);

        this.view.getLblLicensing().removeMouseListener(madLicensing);
        this.view.getLblLicensing().addMouseListener(madLicensing);
    }

    private void doDownloadPics() {
        new GuiDownloadPicturesLQ(null);
    }

    private void doDownloadSetPics() {
        new GuiDownloadSetPicturesLQ(null);
    }

    private void doDownloadQuestImages() {
        new GuiDownloadQuestImages(null);
    }

    /**
     * @param gt0 &emsp; GameType
     * @param d0 &emsp; Deck
     */
    @SuppressWarnings("unchecked")
    public <T> void showDeckEditor(GameType gt0, T d0) {

        DeckEditorBase<?, T> editor = null;
        if (gt0 == GameType.Constructed) {
            editor = (DeckEditorBase<?, T>) new DeckEditorConstructed();
        }
        if (gt0 == GameType.Draft) {
            editor = (DeckEditorBase<?, T>) new DeckEditorLimited(AllZone.getDecks().getDraft());
        }
        if (gt0 == GameType.Sealed) {
            editor = (DeckEditorBase<?, T>) new DeckEditorLimited(AllZone.getDecks().getSealed());
        }


        final Command exit = new Command() {
            private static final long serialVersionUID = -9133358399503226853L;

            @Override
            public void execute() {
                Singletons.getControl().getControlHome().getControlConstructed().updateDeckLists();
                //view.getParentView().getControlSealed().updateDeckLists();
            }
        };

        editor.show(exit);

        if (d0 != null) {
            editor.getController().setModel(d0);
        }

        editor.setVisible(true);
    }
}
