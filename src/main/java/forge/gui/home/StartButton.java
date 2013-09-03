package forge.gui.home;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import forge.gui.toolbox.FSkin;

@SuppressWarnings("serial")
public class StartButton extends JButton {
    public StartButton() {
        final FSkin.AbstractButtonSkin<StartButton> skin = FSkin.get(this);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorder(null);
        setBorderPainted(false);
        setRolloverEnabled(true);
        skin.setRolloverIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_OVER));
        skin.setIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_UP));
        skin.setPressedIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_DOWN));
        
        addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent arg0) {
                skin.setIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_UP));
            }
            
            @Override
            public void focusGained(FocusEvent arg0) {
                skin.setIcon(FSkin.getIcon(FSkin.ButtonImages.IMG_BTN_START_OVER));
            }
        });
        
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEnabled(false);
                
                // ensure the click action can resolve before we allow the button to be clicked again
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { setEnabled(true); }
                });
            }
        });
    }
}
