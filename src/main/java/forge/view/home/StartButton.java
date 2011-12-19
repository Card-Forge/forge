package forge.view.home;

import javax.swing.JButton;

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
        setPressedIcon(v0.getStartButtonDown());
        setRolloverIcon(v0.getStartButtonOver());
        setIcon(v0.getStartButtonUp());
        setOpaque(false);
        setContentAreaFilled(false);
        setBorder(null);
        setBorderPainted(false);
    }
}
