package forge.view.home;

import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.control.home.ControlUtilities;
import forge.view.toolbox.FButton;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FOverlay;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FSkin;

/**
 * Assembles swing components for "Utilities" mode menu.
 * 
 */
@SuppressWarnings("serial")
public class ViewUtilities extends JPanel {
    private final HomeTopLevel parentView;
    private final ControlUtilities control;
    private final JTextPane tpnLicensing;
    private final JLabel lblLicensing;

    private FLabel btnDownloadSetPics, btnDownloadPics, btnDownloadQuestImages, btnReportBug, btnImportPictures,
            btnHowToPlay, btnDownloadPrices, btnDeckEditor;

    private final String license = "Forge License Information" + "\r\n\r\n"
            + "This program is free software : you can redistribute it and/or modify "
            + "it under the terms of the GNU General Public License as published by "
            + "the Free Software Foundation, either version 3 of the License, or "
            + "(at your option) any later version." + "\r\n\r\n"
            + "This program is distributed in the hope that it will be useful, "
            + "but WITHOUT ANY WARRANTY; without even the implied warranty of "
            + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the "
            + "GNU General Public License for more details." + "\r\n\r\n"
            + "You should have received a copy of the GNU General Public License "
            + "along with this program.  If not, see <http://www.gnu.org/licenses/>.";

    /**
     * 
     * Assembles swing components for "Utilities" mode menu.
     * 
     * @param v0
     *            &emsp; HomeTopLevel
     */
    public ViewUtilities(HomeTopLevel v0) {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap, ay center"));
        parentView = v0;
        final String constraintsLBL = "w 90%!, h 20px!, gap 5% 0 3px 8px";
        final String constraintsBTN = "h 30px!, w 50%!, gap 25% 0 0 0";

        btnDownloadPics = new FLabel.Builder().opaque(true).hoverable(true)
                .text("Download LQ Card Pictures").fontScaleFactor(0.5).build();
        final FLabel lblPics = new FLabel.Builder().fontAlign(SwingConstants.CENTER)
                .text("Download default card picture for each card.").fontStyle(Font.ITALIC).build();

        this.add(btnDownloadPics, constraintsBTN);
        this.add(lblPics, constraintsLBL);

        btnDownloadSetPics = new FLabel.Builder().opaque(true).hoverable(true)
                .text("Download LQ Set Pictures").fontScaleFactor(0.5).build();
        final FLabel lblSets = new FLabel.Builder().fontAlign(SwingConstants.CENTER)
                .text("Download full card picture sets for all cards from legacy releases of MTG.")
                .fontStyle(Font.ITALIC).build();

        this.add(btnDownloadSetPics, constraintsBTN);
        this.add(lblSets, constraintsLBL);

        btnDownloadQuestImages = new FLabel.Builder().opaque(true).hoverable(true)
                .text("Download Quest Images").fontScaleFactor(0.5).build();
        final FLabel lblQuest = new FLabel.Builder().fontAlign(SwingConstants.CENTER)
            .text("Download tokens and icons used in Quest mode.").fontStyle(Font.ITALIC).build();

        this.add(btnDownloadQuestImages, constraintsBTN);
        this.add(lblQuest, constraintsLBL);

        btnDownloadPrices = new FLabel.Builder().opaque(true).hoverable(true)
                .text("Download Card Prices").fontScaleFactor(0.5).build();
        final FLabel lblPrices = new FLabel.Builder().fontAlign(SwingConstants.CENTER)
                .text("Download up-to-date price list for in-game card shops.")
                .fontStyle(Font.ITALIC).build();

        this.add(btnDownloadPrices, constraintsBTN);
        this.add(lblPrices, constraintsLBL);

        btnImportPictures = new FLabel.Builder().opaque(true).hoverable(true)
                .text("Import Pictures").fontScaleFactor(0.5).build();
        final FLabel lblImport = new FLabel.Builder().fontAlign(SwingConstants.CENTER)
                .text("Import card pictures from a local version of Forge.")
                .fontStyle(Font.ITALIC).build();
        this.add(btnImportPictures, constraintsBTN);
        this.add(lblImport, constraintsLBL);

        btnReportBug = new FLabel.Builder().opaque(true).hoverable(true)
                .text("Report a Bug").fontScaleFactor(0.5).build();
        final FLabel lblReport = new FLabel.Builder().fontAlign(SwingConstants.CENTER)
                .text("Something broken?").fontStyle(Font.ITALIC).build();

        this.add(btnReportBug, constraintsBTN);
        this.add(lblReport, constraintsLBL);

        btnDeckEditor = new FLabel.Builder().opaque(true).hoverable(true)
                .text("Deck Editor").fontScaleFactor(0.5).build();
        final FLabel lblEditor = new FLabel.Builder().fontAlign(SwingConstants.CENTER)
            .text("Build or edit a deck using all cards available in Forge.")
            .fontStyle(Font.ITALIC).build();
        this.add(btnDeckEditor, constraintsBTN);
        this.add(lblEditor, constraintsLBL);

        btnHowToPlay = new FLabel.Builder().opaque(true).hoverable(true).text("How To Play");
        this.add(btnHowToPlay, constraintsBTN);

        tpnLicensing = new JTextPane();
        tpnLicensing.setOpaque(false);
        tpnLicensing.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tpnLicensing.setFont(FSkin.getFont(15));
        tpnLicensing.setAlignmentX(SwingConstants.CENTER);
        tpnLicensing.setFocusable(false);
        tpnLicensing.setEditable(false);
        tpnLicensing.setBorder(null);
        tpnLicensing.setText(license);

        StyledDocument doc = tpnLicensing.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        lblLicensing = new JLabel("Click For License Information");
        lblLicensing.setFont(FSkin.getFont(16));
        lblLicensing.setHorizontalAlignment(SwingConstants.CENTER);
        lblLicensing.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.add(lblLicensing, "alignx center, gap 5% 0 5% 0");

        control = new ControlUtilities(this);
    }

    /** */
    public void showLicensing() {
        final FOverlay overlay = AllZone.getOverlay();
        final FButton btnClose = new FButton();
        final FPanel pnlContainer = new FPanel();

        btnClose.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                overlay.hideOverlay();
            }
        });
        btnClose.setText("Close");

        pnlContainer.setBorder(new LineBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS), 1));
        pnlContainer.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME));
        pnlContainer.setLayout(new MigLayout("insets 0, wrap"));
        pnlContainer.add(tpnLicensing, "w 90%, gap 5% 0 20px 0, wrap");
        pnlContainer.add(btnClose, "w 300px!, h 40px!, gap 0 0 20px 20px, alignx center");

        overlay.removeAll();
        overlay.setLayout(new MigLayout("insets 0"));
        overlay.add(pnlContainer, "w 50%, gap 25% 0 5% 5%, wrap");
        overlay.showOverlay();
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnDownloadPics() {
        return btnDownloadPics;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnDownloadSetPics() {
        return btnDownloadSetPics;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnDownloadQuestImages() {
        return btnDownloadQuestImages;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnReportBug() {
        return btnReportBug;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnImportPictures() {
        return btnImportPictures;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnHowToPlay() {
        return btnHowToPlay;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnDownloadPrices() {
        return btnDownloadPrices;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnDeckEditor() {
        return btnDeckEditor;
    }

    /** @return JLabel */
    public JLabel getLblLicensing() {
        return lblLicensing;
    }

    /** @return ControlUtilities */
    public ControlUtilities getController() {
        return ViewUtilities.this.control;
    }

    /** @return HomeTopLevel */
    public HomeTopLevel getParentView() {
        return parentView;
    }
} // end class ViewUtilities
