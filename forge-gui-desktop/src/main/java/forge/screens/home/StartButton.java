package forge.screens.home;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedButton;

@SuppressWarnings("serial")
public class StartButton extends SkinnedButton {
    public StartButton() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorder((Border)null);
        setBorderPainted(false);
        setRolloverEnabled(true);
        setRolloverIcon(FSkin.getIcon(FSkinProp.IMG_BTN_START_OVER));
        setIcon(FSkin.getIcon(FSkinProp.IMG_BTN_START_UP));
        setPressedIcon(FSkin.getIcon(FSkinProp.IMG_BTN_START_DOWN));
        // Accessible name.
        this.getAccessibleContext().setAccessibleName("Start game");
        addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent arg0) {
                setIcon(FSkin.getIcon(FSkinProp.IMG_BTN_START_UP));
            }
            
            @Override
            public void focusGained(FocusEvent arg0) {
                setIcon(FSkin.getIcon(FSkinProp.IMG_BTN_START_OVER));
            }
        });
        
        addActionListener(e -> {
            setEnabled(false);

            // ensure the click action can resolve before we allow the button to be clicked again
            SwingUtilities.invokeLater(() -> setEnabled(true));
        });
    }
}
