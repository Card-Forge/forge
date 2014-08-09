package forge.toolbox;

import forge.gui.MouseUtil;
import forge.interfaces.ITextField;
import forge.toolbox.FSkin.SkinnedTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** 
 * A custom instance of JTextArea using Forge skin properties.
 *
 */
@SuppressWarnings("serial")
public class FTextField extends SkinnedTextField implements ITextField {
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
        private String  ghostText;
        private boolean showGhostTextWithFocus;

        public FTextField build() { return new FTextField(this); }

        public Builder maxLength(int i0)    { maxLength = i0; return this; }
        public Builder readonly(boolean b0) { readonly = b0; return this; }
        public Builder readonly()           { return readonly(true); }

        public Builder text(String s0)                    { text = s0; return this; }
        public Builder tooltip(String s0)                 { toolTip = s0; return this; }
        public Builder ghostText(String s0)               { ghostText = s0; return this; }
        public Builder showGhostTextWithFocus(boolean b0) { showGhostTextWithFocus = b0; return this; }
        public Builder showGhostTextWithFocus()           { return showGhostTextWithFocus(true); }
    }

    public static final int HEIGHT = 25; //TODO: calculate this somehow instead of hard-coding it
    private static final FSkin.SkinColor textColor = FSkin.getColor(FSkin.Colors.CLR_TEXT);
    private static final FSkin.SkinColor ghostTextColor = textColor.stepColor(20);
    private static final FSkin.SkinColor backColor = FSkin.getColor(FSkin.Colors.CLR_THEME2);

    private String ghostText;
    private boolean showGhostTextWithFocus;

    private FTextField(Builder builder) {
        this.setForeground(textColor);
        this.setBackground(backColor);
        this.setCaretColor(textColor);
        this.setMargin(new Insets(3, 3, 2, 3));
        this.setOpaque(true);

        if (builder.maxLength > 0) {
            this.setDocument(new _LengthLimitedDocument(builder.maxLength));
        }

        this.setEditable(!builder.readonly);
        this.setText(builder.text);
        this.setToolTipText(builder.toolTip);
        this.setFocusable(true);
        if (this.isEditable()) {
            MouseUtil.setComponentCursor(this, Cursor.TEXT_CURSOR);
        }

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                final FTextField field = (FTextField)e.getSource();
                if (field.isEmpty()) {
                    if (field.ghostText != null && !field.showGhostTextWithFocus) {
                        field.repaint();
                    }
                }
                else { //if not empty, select all text when focused
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            field.selectAll();
                        }
                    });
                }
            }

            @Override
            public void focusLost(final FocusEvent e) {
                FTextField field = (FTextField)e.getSource();
                if (field.ghostText != null && field.isEmpty() && !field.showGhostTextWithFocus) {
                    field.repaint();
                }
            }
        });
        this.showGhostTextWithFocus = builder.showGhostTextWithFocus;
        this.ghostText = builder.ghostText;
        if (this.ghostText == "") { this.ghostText = null; } //don't allow empty string to make other logic easier
    }

    public boolean isEmpty() {
        String text = this.getText();
        return (text == null || text.isEmpty());
    }
    
    public int getAutoSizeWidth() {
        FontMetrics metrics = this.getFontMetrics(this.getFont());
        return metrics.stringWidth(this.getText()) + 12;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (this.ghostText != null && this.isEmpty() && (this.showGhostTextWithFocus || !this.hasFocus())) {
            //TODO: Make ghost text look more like regular text
            final Insets margin = this.getMargin();
            final Graphics2D g2d = (Graphics2D)g.create();
            g2d.setFont(this.getFont());
            FSkin.setGraphicsColor(g2d, ghostTextColor);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawString(this.ghostText, margin.left + 2, getBaseline(getWidth(), getHeight())); //account for borders (TODO: why +15?)
            g2d.dispose();
        }
    }

    public String getGhostText() {
        return this.ghostText;
    }

    public void setGhostText(String ghostText0) {
        if (ghostText0 == "") { ghostText0 = null; } //don't allow empty string to make other logic easier
        if (this.ghostText == ghostText0) { return; }
        this.ghostText = ghostText0;
        if (this.isEmpty()) {
            this.repaint();
        }
    }

    public boolean getShowGhostTextWithFocus() {
        return this.showGhostTextWithFocus;
    }

    public void setShowGhostTextWithFocus(boolean showGhostTextWithFocus0) {
        if (this.showGhostTextWithFocus == showGhostTextWithFocus0) { return; }
        this.showGhostTextWithFocus = showGhostTextWithFocus0;
        if (this.isEmpty() && this.hasFocus()) {
            this.repaint();
        }
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

    public void addChangeListener(ChangeListener listener) {
        this.getDocument().addDocumentListener(listener);
    }

    public static abstract class ChangeListener implements DocumentListener {
        @Override
        public void changedUpdate(DocumentEvent e) {
            textChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            textChanged();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            textChanged();
        }

        public abstract void textChanged();
    }
}
