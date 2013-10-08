package forge.view;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

@SuppressWarnings("serial")
public class FDialog extends JDialog {

    public FDialog() {
        this(true);
    }
    
    public FDialog(boolean modal0) {
        super(JOptionPane.getRootFrame(), modal0);
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            setLocationRelativeTo(JOptionPane.getRootFrame());
        }
        super.setVisible(visible);
    }
}