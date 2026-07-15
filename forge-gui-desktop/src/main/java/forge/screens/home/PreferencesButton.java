package forge.screens.home;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedButton;
import forge.util.Localizer;

/**
 * Skinned lobby control matching {@link StartButton} styling for Preferences.
 */
@SuppressWarnings("serial")
public class PreferencesButton extends SkinnedButton {
    public PreferencesButton() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorder((Border) null);
        setBorderPainted(false);
        setRolloverEnabled(true);
        setRolloverIcon(FSkin.getIcon(FSkinProp.IMG_BTN_PREFERENCES_OVER));
        setIcon(FSkin.getIcon(FSkinProp.IMG_BTN_PREFERENCES_UP));
        setPressedIcon(FSkin.getIcon(FSkinProp.IMG_BTN_PREFERENCES_DOWN));
        this.getAccessibleContext().setAccessibleName("Preferences");
        setToolTipText(Localizer.getInstance().getMessage("Preferences"));
        addFocusListener(new FocusListener() {
            @Override
            public void focusLost(final FocusEvent arg0) {
                setIcon(FSkin.getIcon(FSkinProp.IMG_BTN_PREFERENCES_UP));
            }

            @Override
            public void focusGained(final FocusEvent arg0) {
                setIcon(FSkin.getIcon(FSkinProp.IMG_BTN_PREFERENCES_OVER));
            }
        });

        addActionListener(e -> {
            setEnabled(false);
            SwingUtilities.invokeLater(() -> setEnabled(true));
        });
    }
}
