package forge.screens.home;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedButton;

@SuppressWarnings("serial")
public class StopButton extends SkinnedButton {
    public StopButton() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorder((Border)null);
        setBorderPainted(false);
        setRolloverEnabled(true);
        setRolloverIcon(FSkin.getIcon(FSkinProp.ICO_DELETE_OVER));
        setIcon(FSkin.getIcon(FSkinProp.ICO_DELETE));
        setPressedIcon(FSkin.getIcon(FSkinProp.ICO_DELETE_OVER));

        addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent arg0) {
                setIcon(FSkin.getIcon(FSkinProp.ICO_DELETE));
            }

            @Override
            public void focusGained(FocusEvent arg0) {
                setIcon(FSkin.getIcon(FSkinProp.ICO_DELETE));
            }
        });

        addActionListener(e -> {
            setEnabled(false);

            // ensure the click action can resolve before we allow the button to be clicked again
            SwingUtilities.invokeLater(() -> setEnabled(true));
        });
    }
}
