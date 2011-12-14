package forge.control.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import forge.GuiDownloadPicturesLQ;
import forge.GuiDownloadPrices;
import forge.GuiDownloadQuestImages;
import forge.GuiDownloadSetPicturesLQ;
import forge.GuiImportPicture;
import forge.error.BugzReporter;
import forge.properties.ForgeProps;
import forge.properties.NewConstants.Lang;
import forge.view.home.ViewUtilities;

/** 
 * Controls logic and listeners for Utilities panel of the home screen.
 *
 */
public class ControlUtilities {
    private ViewUtilities view;

    /**
     * 
     * Controls logic and listeners for Utilities panel of the home screen.
     * 
     * @param v0 &emsp; ViewUtilities
     */
    public ControlUtilities(ViewUtilities v0) {
        this.view = v0;
        addListeners();
    }

    /** @return ViewUtilities */
    public ViewUtilities getView() {
        return view;
    }

    /** */
    public void addListeners() {
        this.view.getBtnDownloadPics().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                doDownloadPics();
            }
        });

        this.view.getBtnDownloadSetPics().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                doDownloadSetPics();
            }
        });

        this.view.getBtnDownloadQuestImages().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                doDownloadQuestImages();
            }
        });

        this.view.getBtnReportBug().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final BugzReporter br = new BugzReporter();
                br.setVisible(true);
            }
        });
        this.view.getBtnImportPictures().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final GuiImportPicture ip = new GuiImportPicture(null);
                ip.setVisible(true);
            }
        });
        this.view.getBtnHowToPlay().addActionListener(new ActionListener() {
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
        });
        this.view.getBtnDownloadPrices().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final GuiDownloadPrices gdp = new GuiDownloadPrices();
                gdp.setVisible(true);
            }
        });
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
}
