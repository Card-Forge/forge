package forge.gui.toolbox;

import javax.swing.JRadioButton;

import forge.gui.toolbox.FSkin.JComponentSkin;

/** 
 * A custom instance of JRadioButton using Forge skin properties.
 */
@SuppressWarnings("serial")
public class FRadioButton  extends JRadioButton {
    /** */
    public FRadioButton() {
        this("");
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public FRadioButton(String s0) {
        super();
        this.setText(s0);
        JComponentSkin<FRadioButton> skin = FSkin.get(this);
        skin.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        skin.setFont(FSkin.getFont(14));
        this.setOpaque(false);
    }
}
