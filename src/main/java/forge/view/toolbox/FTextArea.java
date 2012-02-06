package forge.view.toolbox;

import javax.swing.JTextArea;

/** 
 * A custom instance of JTextArea using Forge skin properties.
 *
 */
@SuppressWarnings("serial")
public class FTextArea extends JTextArea {
    /** */
    public FTextArea() {
        super();
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setOpaque(false);
        this.setWrapStyleWord(true);
        this.setLineWrap(true);
        this.setFocusable(false);
        this.setEditable(false);
    }
}
