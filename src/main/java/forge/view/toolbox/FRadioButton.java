package forge.view.toolbox;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JRadioButton;

import forge.Singletons;

/** 
 * A custom instance of JRadioButton using Forge skin properties.
 */
public class FRadioButton  extends JRadioButton {
    private static final long serialVersionUID = -2366973722131882766L;
    private final FSkin skin;

    /** */
    public FRadioButton() {
        this("");
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public FRadioButton(String s0) {
        super();
        this.setText(s0);
        this.skin = Singletons.getView().getSkin();
        this.setForeground(skin.getColor(FSkin.Colors.CLR_TEXT));
        this.setBackground(skin.getColor(FSkin.Colors.CLR_HOVER));
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
