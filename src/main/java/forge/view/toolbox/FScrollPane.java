package forge.view.toolbox;

import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;

import forge.AllZone;

/** 
 * A very basic extension of JScrollPane to centralize common styling changes.
 *
 */
@SuppressWarnings("serial")
public class FScrollPane extends JScrollPane {
    private FSkin skin;
    /**
     * A very basic extension of JScrollPane to centralize common styling changes.
     * 
     * @param c0 {@link java.awt.Component}
     */
    public FScrollPane(Component c0) {
        super(c0);
        //setOpaque(false);
        getViewport().setOpaque(false);

        skin = AllZone.getSkin();
        setBorder(new LineBorder(skin.getColor("borders"), 1));
        setBackground(skin.getColor("zebra"));
    }
}
