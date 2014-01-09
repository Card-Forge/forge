package forge.gui.toolbox;

import forge.gui.toolbox.FSkin.SkinnedTabbedPane;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class FTabbedPane extends SkinnedTabbedPane {
    private static final long serialVersionUID = 2207172560817790885L;

    public FTabbedPane() {
        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME));
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
    }
}
