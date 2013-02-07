package forge.gui.home;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;

import forge.gui.toolbox.FSkin;

@SuppressWarnings("serial")
public class StartButton extends JButton {
    public StartButton() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorder(null);
        setBorderPainted(false);
        setRolloverEnabled(true);
        setRolloverIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_OVER));
        setIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_UP));
        setPressedIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_DOWN));
        
        addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent arg0) {
                setIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_UP));
            }
            
            @Override
            public void focusGained(FocusEvent arg0) {
                setIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_OVER));
            }
        });
    }
}
