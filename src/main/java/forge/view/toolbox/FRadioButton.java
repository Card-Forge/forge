package forge.view.toolbox;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JRadioButton;

/** 
 * A custom instance of JRadioButton using Forge skin properties.
 */
@SuppressWarnings("serial")
public class FRadioButton  extends JRadioButton {
    /** */
    public FRadioButton() {
        this("");
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public FRadioButton(String s0) {
        super();
        this.setText(s0);
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_HOVER));
        this.setFont(FSkin.getFont(14));
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
