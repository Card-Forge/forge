package forge.control.home;

import forge.Command;

/** Dictates methods required for a submenu controller. */
public interface IControlSubmenu {
    /** @return {@link forge.Command} */
    Command getMenuCommand();

    /** Call this method after the view singleton has been fully realized
     * for the first time. This method should ideally only be called once. */
    void initialize();

    /** Update whatever content is in the panel. */
    void update();
}
