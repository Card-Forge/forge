package forge.gui.home.settings;

import javax.swing.SwingUtilities;

import forge.Command;
import forge.gui.framework.ICDoc;

/** 
 * Controls the avatars submenu in the home UI.
 */
public enum CSubmenuAvatars implements ICDoc {
    SINGLETON_INSTANCE;

    @Override
    public void initialize() {
    }

    @Override
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { VSubmenuAvatars.SINGLETON_INSTANCE.focusHuman(); }
        });
    }

    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
