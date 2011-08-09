package forge;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;

/**
 * Custom check box class for filter icon
 * 
 */
public class GuiFilterCheckBox extends JCheckBox {
    /* CHOPPIC */
    /* Custom check box class for filter icons */
    private static final long serialVersionUID = -8099263807219520120L;

    private String imagePath = "res/images/deckeditor/";
    private String iconYes;
    private String iconNo;
    private GuiFilterCheckBox cb;

    GuiFilterCheckBox(String filterName, String toolTip) {
        super("", true);
        cb = this;
        iconYes = imagePath + "filter_" + filterName + "_y.png";
        iconNo = imagePath + "filter_" + filterName + "_n.png";
        this.setIcon(new ImageIcon(iconYes));
        this.setToolTipText(toolTip);
        this.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (cb.isSelected()) {
                    cb.setIcon(new ImageIcon(iconYes));
                } else {
                    cb.setIcon(new ImageIcon(iconNo));
                }
            }
        });
    }
}
