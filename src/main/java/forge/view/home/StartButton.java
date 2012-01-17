package forge.view.home;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import forge.Singletons;
import forge.view.toolbox.FSkin;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class StartButton extends JButton {
    /**
     * @param v0 &emsp; HomeTopLevel
     */
    public StartButton(HomeTopLevel v0) {
        super();
        final FSkin skin = Singletons.getView().getSkin();
        setRolloverEnabled(true);
        setRolloverIcon(new ImageIcon(skin.getImage("button.startDOWN")));
        setOpaque(false);
        setIcon(new ImageIcon(skin.getImage("button.startUP")));
        setContentAreaFilled(false);
        setBorder(null);
        setBorderPainted(false);

        // For some reason, setPressedIcon doesn't work (probably related to thread safety).
        // So, the "down" image is used for the "over" state. Perhaps later can
        // change back. Doublestrike 12-01-11.
        //setPressedIcon(new ImageIcon(skin.getImage("button.startDOWN")));
    }
}
