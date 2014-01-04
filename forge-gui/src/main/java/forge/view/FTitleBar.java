package forge.view;

import java.awt.Dimension;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SpringLayout;

import forge.gui.toolbox.FSkin;

@SuppressWarnings("serial")
public class FTitleBar extends FTitleBarBase {
    private static final FSkin.SkinFont skinFont = FSkin.getFont(12);

    private final JLabel lblTitle = new JLabel();

    public FTitleBar(ITitleBarOwner owner0) {
        super(owner0);
        skin.setMatteBorder(0, 0, 1, 0, bottomEdgeColor);
        owner0.setJMenuBar(this);
        setTitle(owner0.getTitle()); //set default title based on frame title
        setIconImage(owner0.getIconImage()); //set default icon image based on frame icon image
        FSkin.get(lblTitle).setForeground(foreColor);
        FSkin.get(lblTitle).setFont(skinFont);
        addControls();
    }

    @Override
    protected void addControls() {
        add(lblTitle);
        layout.putConstraint(SpringLayout.WEST, lblTitle, 5, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.SOUTH, lblTitle, -5, SpringLayout.SOUTH, this);
        super.addControls();
    }

    @Override
    public void setTitle(String title) {
        this.lblTitle.setText(title);
        updatePreferredSize();
    }

    @Override
    public void setIconImage(Image image) {
        if (image != null) {
            this.lblTitle.setIcon(new ImageIcon(image.getScaledInstance(16, 16, Image.SCALE_AREA_AVERAGING)));
        }
        else {
            this.lblTitle.setIcon(null);
        }
        updatePreferredSize();
    }

    private void updatePreferredSize() {
        int width = skinFont.measureTextWidth(JOptionPane.getRootFrame().getGraphics(), this.lblTitle.getText());
        if (this.lblTitle.getIcon() != null) {
            width += this.lblTitle.getIcon().getIconWidth() + this.lblTitle.getIconTextGap();
        }
        width += btnClose.getPreferredSize().width;
        this.setPreferredSize(new Dimension(width + 10, visibleHeight));
    }
}