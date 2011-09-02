package forge.gui.deckeditor;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import net.miginfocom.swing.MigLayout;

import arcane.ui.CardPanel;
import arcane.ui.ViewPanel;

import forge.Card;
import forge.GuiDisplayUtil;
import forge.ImagePreviewPanel;
import forge.card.CardPrinted;
import forge.gui.game.CardDetailPanel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.swing.OldGuiNewGame;

/** 
 * This panel is to be placed in the right part of a deck editor
 *
 */
public class CardPanelHeavy extends CardPanelBase {

    private static final long serialVersionUID = -7134546689397508597L;

    private JButton changePictureButton = new JButton();
    private JButton removePictureButton = new JButton();

    // Controls to show card details 
    protected CardDetailPanel detail = new CardDetailPanel(null);
    protected CardPanel picture = new CardPanel(null);
    protected ViewPanel pictureViewPanel = new ViewPanel();

    // fake card to allow picture changes 
    public Card cCardHQ;
    /** Constant <code>previousDirectory</code> */
    protected static File previousDirectory = null;

    public CardPanelHeavy() {
        changePictureButton.setText("Change picture...");
        changePictureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changePictureButton_actionPerformed(e);
            }
        });
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            changePictureButton.setFont(new java.awt.Font("Dialog", 0, 10));

        removePictureButton.setText("Remove picture...");
        removePictureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removePictureButton_actionPerformed(e);
            }
        });
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            removePictureButton.setFont(new java.awt.Font("Dialog", 0, 10));
        
        pictureViewPanel.setCardPanel(picture);
        
        this.setLayout(new MigLayout("fill, ins 0"));
        this.add(detail, "w 239, h 323, grow, flowy, wrap");
        this.add(changePictureButton, "align 50% 0%, split 2, flowx");
        this.add(removePictureButton, "align 50% 0%, wrap");
        this.add(pictureViewPanel, "wmin 239, hmin 323, grow");
    }

    public void showCard(CardPrinted card) {
        Card card2 = card.toForgeCard();
        detail.setCard(card2);
        setCard(card2);
    }
    public void setCard(Card c) {
        picture.setCard(c);
    }

    /**
     * <p>
     * changePictureButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void changePictureButton_actionPerformed(ActionEvent e) {
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
                    // TODO Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
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

    protected FileFilter dckFilter = new FileFilter() {

        @Override
        public boolean accept(File f) {
            return f.getName().endsWith(".jpg") || f.isDirectory();
        }

        @Override
        public String getDescription() {
            return "*.jpg";
        }

    };


    void removePictureButton_actionPerformed(ActionEvent e) {
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

    public Card getCard() { return detail.getCard(); }

}
