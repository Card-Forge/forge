package forge.control.home;

import forge.Command;
import forge.view.home.IViewSubmenu;

/** Dictates methods required for a submenu controller. */
public interface IControlSubmenu {
    /** @return {@link forge.command} */
    Command getCommand();

    /** @return {@link forge.view.home.IViewSubmenu} */
    IViewSubmenu getView();

    /** Update whatever content is in the panel. */
    void update();
}
