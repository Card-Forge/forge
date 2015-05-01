package forge.screens.home.settings;

import javax.swing.SwingUtilities;

import forge.gui.framework.ICDoc;

/**
 * Controls the avatars submenu in the home UI.
 */
public enum CSubmenuAvatars implements ICDoc {
    SINGLETON_INSTANCE;

    private final VSubmenuAvatars view = VSubmenuAvatars.SINGLETON_INSTANCE;

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
    }

    @Override
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public final void run() {
                view.refreshAvatarFromPrefs(0);
                view.refreshAvatarFromPrefs(1);
                view.focusHuman(); }
        });
    }

}
