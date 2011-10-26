package forge.gui.deckeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;

/**
 * Custom check box class for filter icon.
 */
public class CheckBoxWithIcon extends JCheckBox {
    /* CHOPPIC */
    /* Custom check box class for filter icons */
    private static final long serialVersionUID = -8099263807219520120L;

    private String imagePath = "res/images/deckeditor/";
    private String iconYes;
    private String iconNo;
    private CheckBoxWithIcon cb;

    /**
     * Instantiates a new check box with icon.
     * 
     * @param filterName
     *            the filter name
     * @param toolTip
     *            the tool tip
     */
    CheckBoxWithIcon(final String filterName, final String toolTip) {
        super("", true);
        cb = this;
        iconYes = imagePath + "filter_" + filterName + "_y.png";
        iconNo = imagePath + "filter_" + filterName + "_n.png";
        this.setIcon(new ImageIcon(iconYes));
        this.setToolTipText(toolTip);
        this.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                if (cb.isSelected()) {
                    cb.setIcon(new ImageIcon(iconYes));
                } else {
                    cb.setIcon(new ImageIcon(iconNo));
                }
            }
        });
    }
}
