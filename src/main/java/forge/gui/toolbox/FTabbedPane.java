package forge.gui.toolbox;

import javax.swing.JTabbedPane;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class FTabbedPane extends JTabbedPane {

    private static final long serialVersionUID = 2207172560817790885L;

    public FTabbedPane() {
        FSkin.JComponentSkin<FTabbedPane> skin = FSkin.get(this);
        skin.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME));
        skin.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
    }
}
