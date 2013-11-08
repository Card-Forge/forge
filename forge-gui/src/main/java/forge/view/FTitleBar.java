package forge.view;

import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SpringLayout;
import forge.gui.toolbox.FSkin;

@SuppressWarnings("serial")
public class FTitleBar extends FTitleBarBase {
    private final JLabel lblTitle = new JLabel();
    
    public FTitleBar(FFrame f) {
        super(f);
        f.setJMenuBar(this);
        setTitle(f.getTitle()); //set default title based on frame title
        setIconImage(f.getIconImage()); //set default icon image based on frame icon image
        FSkin.get(lblTitle).setForeground(foreColor);
        addControls();
    }
    
    @Override
    protected void addControls() {
        add(lblTitle);
        layout.putConstraint(SpringLayout.WEST, lblTitle, 1, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.SOUTH, lblTitle, -5, SpringLayout.SOUTH, this);
        super.addControls();
    }

    @Override
    public void setTitle(String title) {
        this.lblTitle.setText(title);
    }
    
    @Override
    public void setIconImage(Image image) {
        if (image != null) {
            this.lblTitle.setIcon(new ImageIcon(image.getScaledInstance(16, 16, Image.SCALE_AREA_AVERAGING)));
        }
        else {
            this.lblTitle.setIcon(null);
        }
    }
}