package forge.view.home;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
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
import forge.Singletons;
import forge.control.home.ControlUtilities;
import forge.view.toolbox.FButton;
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
    private final FSkin skin;
    private final JTextPane tpnLicensing;
    private final JLabel lblLicensing;

    private SubButton btnDownloadSetPics, btnDownloadPics, btnDownloadQuestImages, btnReportBug, btnImportPictures,
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
        skin = Singletons.getView().getSkin();

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

        tpnLicensing = new JTextPane();
        tpnLicensing.setOpaque(false);
        tpnLicensing.setForeground(skin.getColor("text"));
        tpnLicensing.setFont(skin.getFont(15));
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
        lblLicensing.setFont(skin.getFont(16));
        lblLicensing.setHorizontalAlignment(SwingConstants.CENTER);
        lblLicensing.setForeground(skin.getColor("text"));
        this.add(lblLicensing, "alignx center, span 2 1, gap 25% 0 5% 0");

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

        pnlContainer.setBorder(new LineBorder(skin.getColor("border"), 1));
        pnlContainer.setBGTexture(new ImageIcon(skin.getImage("bg.texture")));
        pnlContainer.setLayout(new MigLayout("insets 0, wrap"));
        pnlContainer.add(tpnLicensing, "w 90%, gap 5% 0 20px 0, wrap");
        pnlContainer.add(btnClose, "w 300px!, h 40px!, gap 0 0 20px 20px, alignx center");

        overlay.removeAll();
        overlay.setLayout(new MigLayout("insets 0"));
        overlay.add(pnlContainer, "w 50%, gap 25% 0 5% 5%, wrap");
        overlay.showOverlay();
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
