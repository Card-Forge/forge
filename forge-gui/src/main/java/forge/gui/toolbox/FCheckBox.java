package forge.gui.toolbox;

import javax.swing.JCheckBox;

import forge.gui.toolbox.FSkin.JComponentSkin;

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
        JComponentSkin<FCheckBox> skin = FSkin.get(this);
        skin.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        skin.setFont(FSkin.getFont(14));
        this.setOpaque(false);
    }
    
    public FCheckBox(final String s0, boolean checked) {
        this(s0);
        setSelected(checked);
    }
}
