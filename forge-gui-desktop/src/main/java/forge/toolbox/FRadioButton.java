package forge.toolbox;

import forge.toolbox.FSkin.SkinnedRadioButton;

/**
 * A custom instance of JRadioButton using Forge skin properties.
 */
@SuppressWarnings("serial")
public class FRadioButton  extends SkinnedRadioButton {
    /** */
    public FRadioButton() {
        this("", null);
    }

    public FRadioButton(final String s0) {
        this(s0, null);
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public FRadioButton(final String s0, final Boolean selected) {
        super();
        this.setText(s0);
        if (null != selected) {
            this.setSelected(selected.booleanValue());
        }
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setFont(FSkin.getFont(14));
        this.setOpaque(false);
        this.setFocusable(false);
    }
}
