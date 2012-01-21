package forge.control.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import forge.Command;
import forge.GuiDownloadPicturesLQ;
import forge.GuiDownloadPrices;
import forge.GuiDownloadQuestImages;
import forge.GuiDownloadSetPicturesLQ;
import forge.GuiImportPicture;
import forge.Singletons;
import forge.deck.Deck;
import forge.error.BugzReporter;
import forge.game.GameType;
import forge.gui.deckeditor.DeckEditorCommon;
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
    private final MouseAdapter madLicensing;
    private final ActionListener actDeckEditor, actPicDownload, actSetDownload,
        actQuestImages, actReportBug, actImportPictures, actHowToPlay, actDownloadPrices;

    /**
     * 
     * Controls logic and listeners for Utilities panel of the home screen.
     * 
     * @param v0 &emsp; ViewUtilities
     */
    public ControlUtilities(ViewUtilities v0) {
        this.view = v0;

        madLicensing = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                view.showLicensing();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                view.getLblLicensing().setForeground(Singletons.getView().getSkin().getColor(FSkin.SkinProp.CLR_HOVER));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                view.getLblLicensing().setForeground(Singletons.getView().getSkin().getColor(FSkin.SkinProp.CLR_TEXT));
            }
        };

        actDeckEditor = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                showDeckEditor(null, null);
            }
        };

        actPicDownload = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                doDownloadPics();
            }
        };

        actSetDownload = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                doDownloadSetPics();
            }
        };

        actQuestImages = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                doDownloadQuestImages();
            }
        };

        actReportBug = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final BugzReporter br = new BugzReporter();
                br.setVisible(true);
            }
        };

        actImportPictures = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final GuiImportPicture ip = new GuiImportPicture(null);
                ip.setVisible(true);
            }
        };

        actHowToPlay = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
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

        actDownloadPrices = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
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
        this.view.getBtnDownloadPics().removeActionListener(actPicDownload);
        this.view.getBtnDownloadPics().addActionListener(actPicDownload);
        this.view.getBtnDownloadSetPics().removeActionListener(actSetDownload);
        this.view.getBtnDownloadSetPics().addActionListener(actSetDownload);
        this.view.getBtnDownloadQuestImages().removeActionListener(actQuestImages);
        this.view.getBtnDownloadQuestImages().addActionListener(actQuestImages);
        this.view.getBtnReportBug().removeActionListener(actReportBug);
        this.view.getBtnReportBug().addActionListener(actReportBug);
        this.view.getBtnImportPictures().removeActionListener(actImportPictures);
        this.view.getBtnImportPictures().addActionListener(actImportPictures);
        this.view.getBtnHowToPlay().removeActionListener(actHowToPlay);
        this.view.getBtnHowToPlay().addActionListener(actHowToPlay);
        this.view.getBtnDownloadPrices().removeActionListener(actDownloadPrices);
        this.view.getBtnDownloadPrices().addActionListener(actDownloadPrices);
        this.view.getBtnDeckEditor().removeActionListener(actDeckEditor);
        this.view.getBtnDeckEditor().addActionListener(actDeckEditor);
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
    public void showDeckEditor(GameType gt0, Deck d0) {
        if (gt0 == null) {
            gt0 = GameType.Constructed;
        }

        DeckEditorCommon editor = new DeckEditorCommon(gt0);

        final Command exit = new Command() {
            private static final long serialVersionUID = -9133358399503226853L;

            @Override
            public void execute() {
                view.getParentView().getConstructedController().updateDeckNames();
                view.getParentView().getSealedController().updateDeckLists();
            }
        };

        editor.show(exit);

        if (d0 != null) {
            editor.getCustomMenu().showDeck(d0, gt0);
        }

        editor.setVisible(true);
    }
}
