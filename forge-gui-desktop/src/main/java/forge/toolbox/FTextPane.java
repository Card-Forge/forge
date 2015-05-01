package forge.toolbox;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import forge.toolbox.FSkin.SkinnedTextPane;

/**
 * A custom instance of JTextArea using Forge skin properties.
 *
 */
@SuppressWarnings("serial")
public class FTextPane extends SkinnedTextPane {

    public FTextPane() {
        super();
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setCaretColor(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setOpaque(false);
        this.setFocusable(false);
        this.setEditable(false);
    }

    public FTextPane(final String str) {
        this();
        this.setText(str);
    }

    //Use constant in StyleConstants
    public void setTextAlignment(final int alignment) {
        final StyledDocument doc = getStyledDocument();
        final SimpleAttributeSet attrSet = new SimpleAttributeSet();
        StyleConstants.setAlignment(attrSet, alignment);
        doc.setParagraphAttributes(0, doc.getLength(), attrSet, false);
    }
}
