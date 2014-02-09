package forge.gui.toolbox;

import forge.gui.toolbox.FSkin.SkinnedSpinner;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;

/** 
 * A custom instance of JSpinner using Forge skin properties.  Only numeric
 * integer spinners are implemented, since that's all we've needed so far.
 *
 */
@SuppressWarnings("serial")
public class FSpinner extends SkinnedSpinner {
    public static class Builder {
        //========== Default values for FTextField are set here.
        private int    initialValue = 0;
        private int    minValue = 0;
        private int    maxValue = Integer.MAX_VALUE;
        private String toolTip;

        public FSpinner build() { return new FSpinner(this); }

        public Builder initialValue(int i0) { initialValue = i0; return this; }
        public Builder minValue(int i0)     { minValue = i0; return this; }
        public Builder maxValue(int i0)     { maxValue = i0; return this; }

        public Builder tooltip(String s0) { toolTip = s0; return this; }
    }

    private FSpinner(Builder builder) {
        this.setOpaque(false);
        
        this.setModel(new SpinnerNumberModel(builder.initialValue, builder.minValue, builder.maxValue, 1));
        this.setEditor(new JSpinner.NumberEditor(this, "##"));
        JFormattedTextField txt = this.getTextField();
        ((NumberFormatter)txt.getFormatter()).setAllowsInvalid(false);
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        this.setCaretColor(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        txt.setMargin(new Insets(5, 5, 5, 5));
        txt.setOpaque(true);
        
        this.setToolTipText(builder.toolTip);
        this.setFocusable(true);
    }
}
