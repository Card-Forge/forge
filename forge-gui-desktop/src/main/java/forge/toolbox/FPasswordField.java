package forge.toolbox;

import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import forge.gui.MouseUtil;
import forge.interfaces.ITextField;
import forge.toolbox.FSkin.SkinnedPasswordField;

/** 
 * A custom instance of JPasswordField using Forge skin properties.
 *
 */
@SuppressWarnings("serial")
public class FPasswordField extends SkinnedPasswordField implements ITextField {
    private static final FSkin.SkinColor textColor = FSkin.getColor(FSkin.Colors.CLR_TEXT);
    private static final FSkin.SkinColor ghostTextColor = textColor.stepColor(20);
    private static final FSkin.SkinColor backColor = FSkin.getColor(FSkin.Colors.CLR_THEME2);

    private String ghostText;
    private boolean showGhostTextWithFocus;

    public FPasswordField() {
        this.setForeground(textColor);
        this.setBackground(backColor);
        this.setCaretColor(textColor);
        this.setMargin(new Insets(3, 3, 2, 3));
        this.setOpaque(true);
        this.setFocusable(true);
        if (this.isEditable()) {
            MouseUtil.setComponentCursor(this, Cursor.TEXT_CURSOR);
        }

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                final FPasswordField field = (FPasswordField)e.getSource();
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
                FPasswordField field = (FPasswordField)e.getSource();
                if (field.ghostText != null && field.isEmpty() && !field.showGhostTextWithFocus) {
                    field.repaint();
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    public boolean isEmpty() {
        String text = this.getText();
        return (text == null || text.isEmpty());
    }
    
    @SuppressWarnings("deprecation")
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
        if ("".equals(this.ghostText)) { ghostText0 = null; } //don't allow empty string to make other logic easier
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
