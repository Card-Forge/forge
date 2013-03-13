package forge.gui.toolbox;

import javax.swing.JCheckBox;

/** 
 * A custom instance of JCheckBox using Forge skin properties.
 */
@SuppressWarnings("serial")
public class FCheckBox extends JCheckBox {
    public FCheckBox() {
        this("");
    }

    public FCheckBox(final String s0) {
        super(s0);
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setFont(FSkin.getFont(14));
        this.setOpaque(false);
    }
}
