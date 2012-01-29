package forge.view.toolbox;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;

import forge.Singletons;

/** 
 * A custom instance of JCheckBox using Forge skin properties.
 */
public class FCheckBox extends JCheckBox {
    private static final long serialVersionUID = -8633657166511001814L;
    private final FSkin skin;

    /** */
    public FCheckBox() {
        this("");
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public FCheckBox(final String s0) {
        super(s0);
        this.skin = Singletons.getView().getSkin();
        this.setForeground(skin.getColor(FSkin.SkinProp.CLR_TEXT));
        this.setBackground(skin.getColor(FSkin.SkinProp.CLR_HOVER));
        this.setFont(skin.getFont(14));
        this.setOpaque(false);

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setOpaque(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setOpaque(false);
            }
        });
    }
}
