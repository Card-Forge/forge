package forge.gui.toolbox;

import java.awt.Insets;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/** 
 * A custom instance of JTextArea using Forge skin properties.
 *
 */
@SuppressWarnings("serial")
public class FTextField extends JTextField {
    /** 
     * Uses the Builder pattern to facilitate/encourage inline styling.
     * Credit to Effective Java 2 (Joshua Bloch).
     * Methods in builder can be chained. To declare:
     * <code>new FTextField.Builder().method1(foo).method2(bar).method3(baz)...</code>
     * <br>and then call build() to make the widget (don't forget that part).
    */
    public static class Builder {
        //========== Default values for FTextField are set here.
        private int     maxLength = 0; // <=0 indicates no maximum
        private boolean readonly  = false;
        private String  text;
        private String  toolTip;

        public FTextField build() { return new FTextField(this); }

        public Builder maxLength(int i0)    { maxLength = i0; return this; }
        public Builder readonly(boolean b0) { readonly = b0; return this; }
        public Builder readonly()           { return readonly(true); }

        public Builder text(String s0)    { text = s0; return this; }
        public Builder tooltip(String s0) { toolTip = s0; return this; }
    }

    private FTextField(Builder builder) {
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        this.setCaretColor(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setMargin(new Insets(5, 5, 5, 5));
        this.setOpaque(true);
        
        if (0 < builder.maxLength)
        {
            this.setDocument(new _LengthLimitedDocument(builder.maxLength));
        }
        
        this.setEditable(!builder.readonly);
        this.setText(builder.text);
        this.setToolTipText(builder.toolTip);
        this.setFocusable(true);
    }

    private static class _LengthLimitedDocument extends PlainDocument {
        private final int _limit;
        
        _LengthLimitedDocument(int limit) { _limit = limit; }

        // called each time a character is typed or a string is pasted
        @Override
        public void insertString(int offset, String s, AttributeSet attributeSet)
                throws BadLocationException {
            if (_limit <= this.getLength()) {
                return;
            }
            int newLen = this.getLength() + s.length();
            if (_limit < newLen) {
                s = s.substring(0, _limit - this.getLength());
            }
            super.insertString(offset, s, attributeSet);
        }
    }
}
