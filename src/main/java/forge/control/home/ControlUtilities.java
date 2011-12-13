package forge.control.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import forge.GuiDownloadPicturesLQ;
import forge.GuiDownloadQuestImages;
import forge.GuiDownloadSetPicturesLQ;
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
