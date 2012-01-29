package forge.view.toolbox;

import javax.swing.JTextArea;

import forge.Singletons;

/** 
 * A custom instance of JTextArea using Forge skin properties.
 *
 */
@SuppressWarnings("serial")
public class FTextArea extends JTextArea {
    private final FSkin skin;
    /** */
    public FTextArea() {
        super();
        this.skin = Singletons.getView().getSkin();
        this.setForeground(skin.getColor(FSkin.Colors.CLR_TEXT));
        this.setOpaque(false);
        this.setWrapStyleWord(true);
        this.setLineWrap(true);
        this.setFocusable(false);
        this.setEditable(false);
    }
}
