package forge.toolbox;

import forge.toolbox.FSkin.SkinnedTextArea;

import java.awt.*;

/** 
 * A custom instance of JTextArea using Forge skin properties.
 *
 */
@SuppressWarnings("serial")
public class FTextArea extends SkinnedTextArea {
    private boolean autoSize;

    /** */
    public FTextArea() {
        super();
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setCaretColor(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setOpaque(false);
        this.setWrapStyleWord(true);
        this.setLineWrap(true);
        this.setFocusable(false);
        this.setEditable(false);
    }
    /** @param str {@java.lang.String} */
    public FTextArea(final String str) {
        this();
        this.setText(str);
    }

    public boolean getAutoSize() {
        return this.autoSize;
    }

    public void setAutoSize(boolean autoSize0) {
        if (this.autoSize == autoSize0) { return; }
        this.autoSize = autoSize0;
        if (autoSize0) {
            this.setColumns(1);
        }
        else {
            this.setColumns(0);
        }
    }

    @Override
    protected int getColumnWidth() {
        if (!this.autoSize) {
            return super.getColumnWidth();
        }

        int maxLineWidth = 0;
        FontMetrics metrics = this.getGraphics().getFontMetrics(this.getFont());
        String[] lines = this.getText().split("(\r\n)|(\n)");
        for (int i = 0; i < lines.length; i++) {
            int lineWidth = metrics.stringWidth(lines[i]);
            if (lineWidth > maxLineWidth) {
                maxLineWidth = lineWidth;
            }
        }
        return maxLineWidth; //size to fit longest line by default, letting maximum size create wrapping before that
    }
}
