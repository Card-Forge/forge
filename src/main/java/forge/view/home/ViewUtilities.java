package forge.view.home;

import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.control.home.ControlUtilities;
import forge.view.toolbox.FSkin;

/** 
 * Assembles swing components for "Utilities" mode menu.
 *
 */
@SuppressWarnings("serial")
public class ViewUtilities extends JPanel {
    private HomeTopLevel parentView;
    private ControlUtilities control;
    private FSkin skin;
    private JTextArea tarLicensing;

    private SubButton btnDownloadSetPics, btnDownloadPics, btnDownloadQuestImages,
        btnReportBug, btnImportPictures, btnHowToPlay, btnDownloadPrices,
        btnDeckEditor;
    /**
     * 
     * Assembles swing components for "Utilities" mode menu.
     * @param v0 &emsp; HomeTopLevel
     */
    public ViewUtilities(HomeTopLevel v0) {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap, ay center"));
        parentView = v0;
        skin = AllZone.getSkin();

        btnDownloadPics = new SubButton("Download LQ Card Pictures");
        this.add(btnDownloadPics, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%, gaptop 5%");

        btnDownloadSetPics = new SubButton("Download LQ Set Pictures");
        this.add(btnDownloadSetPics, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        btnDownloadQuestImages = new SubButton("Download Quest Images");
        this.add(btnDownloadQuestImages, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        btnDownloadPrices = new SubButton("Download Card Prices");
        this.add(btnDownloadPrices, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        btnImportPictures = new SubButton("Import Pictures");
        this.add(btnImportPictures, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        btnReportBug = new SubButton("Report a Bug");
        this.add(btnReportBug, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        btnDeckEditor = new SubButton("Deck Editor");
        this.add(btnDeckEditor, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        btnHowToPlay = new SubButton("How To Play");
        this.add(btnHowToPlay, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        /*
         * slapshot5 - I think this is useless here.  If it serves a purpose, just uncomment,
         * and hook it up in ControlUtilities.
         * 
         * doublestrike - too right
         */
        // TODO make this a dock shortcut
        /*
        SubButton btnStackReport = new SubButton("Stack Report");
        this.add(btnStackReport, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");
        */

        tarLicensing = new JTextArea();
        tarLicensing.setOpaque(false);
        tarLicensing.setForeground(skin.getColor("text"));
        tarLicensing.setFont(skin.getFont1().deriveFont(Font.PLAIN, 15));
        tarLicensing.setAlignmentX(SwingConstants.CENTER);
        tarLicensing.setLineWrap(true);
        tarLicensing.setWrapStyleWord(true);
        tarLicensing.setFocusable(false);
        tarLicensing.setEditable(false);
        tarLicensing.setBorder(null);
        tarLicensing.setText("Click here for license information.");

        this.add(tarLicensing, "w 80%!, gapleft 10%, ax center");

        ViewUtilities.this.control = new ControlUtilities(this);
    }

    /** @return SubButton */
    public SubButton getBtnDownloadPics() {
        return btnDownloadPics;
    }

    /** @return SubButton */
    public SubButton getBtnDownloadSetPics() {
        return btnDownloadSetPics;
    }

    /** @return SubButton */
    public SubButton getBtnDownloadQuestImages() {
        return btnDownloadQuestImages;
    }

    /** @return SubButton */
    public SubButton getBtnReportBug() {
        return btnReportBug;
    }

    /** @return SubButton */
    public SubButton getBtnImportPictures() {
        return btnImportPictures;
    }

    /** @return SubButton */
    public SubButton getBtnHowToPlay() {
        return btnHowToPlay;
    }

    /** @return SubButton */
    public SubButton getBtnDownloadPrices() {
        return btnDownloadPrices;
    }

    /** @return SubButton */
    public SubButton getBtnDeckEditor() {
        return btnDeckEditor;
    }

    /** @return JTextArea */
    public JTextArea getTarLicensing() {
        return tarLicensing;
    }

    /** @return ControlUtilities */
    public ControlUtilities getController() {
        return ViewUtilities.this.control;
    }

    /** @return HomeTopLevel */
    public HomeTopLevel getParentView() {
        return parentView;
    }
} //end class ViewUtilities
