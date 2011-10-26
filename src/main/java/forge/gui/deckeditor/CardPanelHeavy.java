package forge.gui.deckeditor;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import net.miginfocom.swing.MigLayout;
import arcane.ui.CardPanel;
import arcane.ui.ViewPanel;
import forge.Card;
import forge.GuiDisplayUtil;
import forge.ImagePreviewPanel;
import forge.Singletons;
import forge.gui.game.CardDetailPanel;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * This panel is to be placed in the right part of a deck editor.
 */
public class CardPanelHeavy extends CardPanelBase {

    private static final long serialVersionUID = -7134546689397508597L;

    private JButton changeStateButton = new JButton();

    /*
     * Removed Oct 25 2011 - Hellfish private JButton changePictureButton = new
     * JButton(); private JButton removePictureButton = new JButton();
     */

    // Controls to show card details
    /** The detail. */
    protected CardDetailPanel detail = new CardDetailPanel(null);

    /** The picture. */
    protected CardPanel picture = new CardPanel(null);

    /** The picture view panel. */
    protected ViewPanel pictureViewPanel = new ViewPanel();

    // fake card to allow picture changes
    /** The c card hq. */
    public Card cCardHQ;

    /** Constant <code>previousDirectory</code>. */
    protected static File previousDirectory = null;

    /**
     * Instantiates a new card panel heavy.
     */
    public CardPanelHeavy() {
        changeStateButton.setVisible(false);
        changeStateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                changeStateButton_actionPerformed(e);
            }
        });
        if (!Singletons.getModel().getPreferences().lafFonts) {
            changeStateButton.setFont(new java.awt.Font("Dialog", 0, 10));
        }

        /*
         * Removed Oct 25 2011 - Hellfish
         * changePictureButton.setText("Change picture...");
         * changePictureButton.addActionListener(new
         * java.awt.event.ActionListener() { public void
         * actionPerformed(ActionEvent e) {
         * changePictureButton_actionPerformed(e); } }); if
         * (!Singletons.getModel().getPreferences().lafFonts)
         * changePictureButton.setFont(new java.awt.Font("Dialog", 0, 10));
         * 
         * removePictureButton.setText("Remove picture...");
         * removePictureButton.addActionListener(new
         * java.awt.event.ActionListener() { public void
         * actionPerformed(ActionEvent e) {
         * removePictureButton_actionPerformed(e); } }); if
         * (!Singletons.getModel().getPreferences().lafFonts)
         * removePictureButton.setFont(new java.awt.Font("Dialog", 0, 10));
         */

        pictureViewPanel.setCardPanel(picture);

        this.setLayout(new MigLayout("fill, ins 0"));
        this.add(detail, "w 239, h 323, grow, flowy, wrap");
        /*
         * Removed Oct 25 2011 - Hellfish this.add(changeStateButton,
         * "align 50% 0%, split 3, flowx"); this.add(changePictureButton,
         * "align 50% 0%"); this.add(removePictureButton, "align 50% 0%, wrap");
         */
        this.add(changeStateButton, "align 50% 0%, flowx, wrap");
        this.add(pictureViewPanel, "wmin 239, hmin 323, grow");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.gui.deckeditor.CardPanelBase#showCard(forge.item.InventoryItem)
     */
    public final void showCard(final InventoryItem card) {
        Card card2 = card instanceof CardPrinted ? ((CardPrinted) card).toForgeCard() : null;
        detail.setCard(card2);
        setCard(card2);
    }

    /**
     * Sets the card.
     * 
     * @param c
     *            the new card
     */
    public final void setCard(final Card c) {
        if (picture.getCard() != null) {
            if (picture.getCard().isInAlternateState()) {
                picture.getCard().changeState();
            }
        }
        picture.setCard(c);

        if (c.hasAlternateState()) {
            changeStateButton.setVisible(true);
            if (c.isFlip()) {
                changeStateButton.setText("Flip");
            } else {
                changeStateButton.setText("Transform");
            }
        } else {
            changeStateButton.setVisible(false);
        }
    }

    /**
     * <p>
     * changeStateButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    final void changeStateButton_actionPerformed(final ActionEvent e) {
        Card cur = picture.getCard();
        cur.changeState();

        picture.setCard(cur);
        detail.setCard(cur);
    }

    /**
     * <p>
     * changePictureButton_actionPerformed. Removed Oct 25 2011 - Hellfish
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    final void changePictureButton_actionPerformed(final ActionEvent e) {
        if (cCardHQ != null) {
            File file = getImportFilename();
            if (file != null) {
                String fileName = GuiDisplayUtil.cleanString(cCardHQ.getName()) + ".jpg";
                File base = ForgeProps.getFile(NewConstants.IMAGE_BASE);
                File f = new File(base, fileName);
                f.delete();

                try {
                    org.apache.commons.io.FileUtils.copyFile(file, f);
                } catch (IOException e1) {
                    // TODO Auto-generated catch block ignores the exception,
                    // but sends it to System.err and probably forge.log.
                    e1.printStackTrace();
                }
                setCard(cCardHQ);
            }
        }
    }

    /**
     * <p>
     * getImportFilename.
     * </p>
     * 
     * @return a {@link java.io.File} object.
     */
    private File getImportFilename() {
        JFileChooser chooser = new JFileChooser(previousDirectory);
        ImagePreviewPanel preview = new ImagePreviewPanel();
        chooser.setAccessory(preview);
        chooser.addPropertyChangeListener(preview);
        chooser.addChoosableFileFilter(dckFilter);
        int returnVal = chooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            previousDirectory = file.getParentFile();
            return file;
        }

        return null;

    }

    /** The dck filter. */
    protected FileFilter dckFilter = new FileFilter() {

        @Override
        public boolean accept(final File f) {
            return f.getName().endsWith(".jpg") || f.isDirectory();
        }

        @Override
        public String getDescription() {
            return "*.jpg";
        }

    };

    /**
     * <p>
     * removePictureButton_actionPerformed
     * </p>
     * . Removed Oct 25 2011 - Hellfish
     * 
     * @param e
     *            the e
     */
    final void removePictureButton_actionPerformed(final ActionEvent e) {
        if (cCardHQ != null) {
            String options[] = { "Yes", "No" };
            int value = JOptionPane.showOptionDialog(null, "Do you want delete " + cCardHQ.getName() + " picture?",
                    "Delete picture", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
                    options[1]);
            if (value == 0) {
                String fileName = GuiDisplayUtil.cleanString(cCardHQ.getName()) + ".jpg";
                File base = ForgeProps.getFile(NewConstants.IMAGE_BASE);
                File f = new File(base, fileName);
                f.delete();
                JOptionPane.showMessageDialog(null, "Picture " + cCardHQ.getName() + " deleted.", "Delete picture",
                        JOptionPane.INFORMATION_MESSAGE);
                setCard(cCardHQ);
            }
        }
    }

    /**
     * Gets the card.
     * 
     * @return the card
     */
    public final Card getCard() {
        return detail.getCard();
    }

}
