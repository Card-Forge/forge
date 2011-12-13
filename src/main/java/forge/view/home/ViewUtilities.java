package forge.view.home;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.MatteBorder;

import forge.AllZone;
import forge.control.home.ControlUtilities;

import net.miginfocom.swing.MigLayout;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class ViewUtilities extends JPanel {
    private ControlUtilities control;
    
    private SubButton btnDownloadSetPics, btnDownloadPics, btnDownloadQuestImages;
    /**
     * 
     * TODO: Write javadoc for Constructor.
     * @param v0 &emsp; HomeTopLevel
     */
    public ViewUtilities(HomeTopLevel v0) {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap, ay center"));

        btnDownloadPics = new SubButton("Download LQ Card Pictures");
        this.add(btnDownloadPics, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%, gaptop 5%");
        
        btnDownloadSetPics = new SubButton("Download LQ Set Pictures");
        this.add(btnDownloadSetPics, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");
        
        btnDownloadQuestImages = new SubButton("Download Quest Images");
        this.add(btnDownloadQuestImages, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        SubButton btnDownloadPrices = new SubButton("Download Card Prices");
        this.add(btnDownloadPrices, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        SubButton btnImportPics = new SubButton("Import Pictures");
        this.add(btnImportPics, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        SubButton btnReportBug = new SubButton("Report a Bug");
        this.add(btnReportBug, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        SubButton btnStackReport = new SubButton("Stack Report");
        this.add(btnStackReport, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        SubButton btnHowToPlay = new SubButton("How To Play");
        this.add(btnHowToPlay, "h 30px!, w 50%!, gapleft 25%, gapbottom 2%");

        JLabel lblAbout = new JLabel("About Forge here: Licensing, etc.");
        lblAbout.setBorder(new MatteBorder(1, 0, 0, 0, AllZone.getSkin().getColor("borders")));
        this.add(lblAbout, "w 80%, gapleft 10%, gaptop 5%");
        
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
    
    /**
     * 
     * TODO: Write javadoc for this method.
     * @return the ControlUtilities
     */
    public ControlUtilities getController() {
        return ViewUtilities.this.control;
    }
    
} //end class ViewUtilities
