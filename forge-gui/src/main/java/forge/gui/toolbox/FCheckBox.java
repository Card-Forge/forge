package forge.gui.toolbox;

import forge.gui.toolbox.FSkin.SkinnedCheckBox;

/** 
 * A custom instance of JCheckBox using Forge skin properties.
 */
@SuppressWarnings("serial")
public class FCheckBox extends SkinnedCheckBox {
    public FCheckBox() {
        this("");
    }

    public FCheckBox(final String s0) {
        super(s0);
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setFont(FSkin.getFont(14));
        this.setOpaque(false);
    }
    
    public FCheckBox(final String s0, boolean checked) {
        this(s0);
        setSelected(checked);
    }
}
