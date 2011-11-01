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

    private final String imagePath = "res/images/deckeditor/";
    private final String iconYes;
    private final String iconNo;
    private final CheckBoxWithIcon cb;

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
        this.cb = this;
        this.iconYes = this.imagePath + "filter_" + filterName + "_y.png";
        this.iconNo = this.imagePath + "filter_" + filterName + "_n.png";
        this.setIcon(new ImageIcon(this.iconYes));
        this.setToolTipText(toolTip);
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                if (CheckBoxWithIcon.this.cb.isSelected()) {
                    CheckBoxWithIcon.this.cb.setIcon(new ImageIcon(CheckBoxWithIcon.this.iconYes));
                } else {
                    CheckBoxWithIcon.this.cb.setIcon(new ImageIcon(CheckBoxWithIcon.this.iconNo));
                }
            }
        });
    }
}
