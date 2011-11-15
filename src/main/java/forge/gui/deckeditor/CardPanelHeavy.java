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

    private final JButton changeStateButton = new JButton();

    /*
     * Removed Oct 25 2011 - Hellfish private JButton changePictureButton = new
     * JButton(); private JButton removePictureButton = new JButton();
     */

    // Controls to show card details
    /** The detail. */
    private CardDetailPanel detail = new CardDetailPanel(null);

    /** The picture. */
    private CardPanel picture = new CardPanel(null);

    /** The picture view panel. */
    private ViewPanel pictureViewPanel = new ViewPanel();

    // fake card to allow picture changes
    /** The c card hq. */
    private Card cCardHQ;

    /** Constant <code>previousDirectory</code>. */
    private static File previousDirectory = null;

    /**
     * Instantiates a new card panel heavy.
     */
    public CardPanelHeavy() {
        this.changeStateButton.setVisible(false);
        this.changeStateButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                CardPanelHeavy.this.changeStateButtonActionPerformed(e);
            }
        });
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            this.changeStateButton.setFont(new java.awt.Font("Dialog", 0, 10));
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

        this.pictureViewPanel.setCardPanel(this.picture);

        this.setLayout(new MigLayout("fill, ins 0"));
        this.add(this.detail, "w 239, h 323, grow, flowy, wrap");
        /*
         * Removed Oct 25 2011 - Hellfish this.add(changeStateButton,
         * "align 50% 0%, split 3, flowx"); this.add(changePictureButton,
         * "align 50% 0%"); this.add(removePictureButton, "align 50% 0%, wrap");
         */
        this.add(this.changeStateButton, "align 50% 0%, flowx, wrap");
        this.add(this.pictureViewPanel, "wmin 239, hmin 323, grow");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.gui.deckeditor.CardPanelBase#showCard(forge.item.InventoryItem)
     */
    @Override
    public final void showCard(final InventoryItem card) {
        final Card card2 = card instanceof CardPrinted ? ((CardPrinted) card).toForgeCard() : null;
        this.detail.setCard(card2);
        this.setCard(card2);
    }

    /**
     * Sets the card.
     * 
     * @param c
     *            the new card
     */
    public final void setCard(final Card c) {
        if (this.picture.getCard() != null) {
            if (this.picture.getCard().isInAlternateState()) {
                this.picture.getCard().setState("Original");
            }
        }
        this.picture.setCard(c);

        if (c.hasAlternateState()) {
            this.changeStateButton.setVisible(true);
            if (c.isFlip()) {
                this.changeStateButton.setText("Flip");
            } else {
                this.changeStateButton.setText("Transform");
            }
        } else {
            this.changeStateButton.setVisible(false);
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
    final void changeStateButtonActionPerformed(final ActionEvent e) {
        final Card cur = this.picture.getCard();
        if(cur.isInAlternateState()) {
            cur.setState("Original");
        }
        else {
            if(cur.isFlip()) {
                cur.setState("Flipped");
            }
            if(cur.isDoubleFaced()) {
                cur.setState("Transformed");
            }
        }

        this.picture.setCard(cur);
        this.detail.setCard(cur);
    }

    /**
     * <p>
     * changePictureButton_actionPerformed. Removed Oct 25 2011 - Hellfish
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    private void changePictureButtonActionPerformed(final ActionEvent e) {
        if (this.cCardHQ != null) {
            final File file = this.getImportFilename();
            if (file != null) {
                final String fileName = GuiDisplayUtil.cleanString(this.cCardHQ.getName()) + ".jpg";
                final File base = ForgeProps.getFile(NewConstants.IMAGE_BASE);
                final File f = new File(base, fileName);
                f.delete();

                try {
                    org.apache.commons.io.FileUtils.copyFile(file, f);
                } catch (final IOException e1) {
                    // TODO Auto-generated catch block ignores the exception,
                    // but sends it to System.err and probably forge.log.
                    e1.printStackTrace();
                }
                this.setCard(this.cCardHQ);
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
        final JFileChooser chooser = new JFileChooser(CardPanelHeavy.previousDirectory);
        final ImagePreviewPanel preview = new ImagePreviewPanel();
        chooser.setAccessory(preview);
        chooser.addPropertyChangeListener(preview);
        chooser.addChoosableFileFilter(this.dckFilter);
        final int returnVal = chooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = chooser.getSelectedFile();
            CardPanelHeavy.previousDirectory = file.getParentFile();
            return file;
        }

        return null;

    }

    /** The dck filter. */
    private FileFilter dckFilter = new FileFilter() {

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
    final void removePictureButtonActionPerformed(final ActionEvent e) {
        if (this.cCardHQ != null) {
            final String[] options = { "Yes", "No" };
            final int value = JOptionPane.showOptionDialog(null, "Do you want delete " + this.cCardHQ.getName()
                    + " picture?", "Delete picture", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                    options, options[1]);
            if (value == 0) {
                final String fileName = GuiDisplayUtil.cleanString(this.cCardHQ.getName()) + ".jpg";
                final File base = ForgeProps.getFile(NewConstants.IMAGE_BASE);
                final File f = new File(base, fileName);
                f.delete();
                JOptionPane.showMessageDialog(null, "Picture " + this.cCardHQ.getName() + " deleted.",
                        "Delete picture", JOptionPane.INFORMATION_MESSAGE);
                this.setCard(this.cCardHQ);
            }
        }
    }

    /**
     * Gets the card.
     * 
     * @return the card
     */
    public final Card getCard() {
        return this.detail.getCard();
    }

}
