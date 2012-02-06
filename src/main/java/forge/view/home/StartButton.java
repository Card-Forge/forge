package forge.view.home;

import javax.swing.JButton;

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
        setRolloverEnabled(true);
        setRolloverIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_OVER));
        setOpaque(false);
        setIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_UP));
        setContentAreaFilled(false);
        setBorder(null);
        setBorderPainted(false);
        setPressedIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_DOWN));
    }
}
