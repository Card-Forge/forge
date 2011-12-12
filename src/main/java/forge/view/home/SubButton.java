package forge.view.home;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.border.LineBorder;

import forge.AllZone;
import forge.view.toolbox.FSkin;

/** 
 * Standard button used for for submenus on the home screen.
 *
 */
@SuppressWarnings("serial")
public class SubButton extends JButton {
    /** */
    public SubButton() {
        this("");
    }

    /**
     * 
     * Standard button used for for submenus on the home screen.
     *
     * @param txt0 &emsp; String
     */
    public SubButton(String txt0) {
        super(txt0);
        final FSkin skin = AllZone.getSkin();
        setBorder(new LineBorder(skin.getColor("borders"), 1));
        setBackground(skin.getColor("inactive"));
        setForeground(skin.getColor("text"));

        this.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                setBackground(skin.getColor("hover"));
            }

            public void mouseExited(MouseEvent e) {
                setBackground(skin.getColor("inactive"));
            }
        });
    }
}
