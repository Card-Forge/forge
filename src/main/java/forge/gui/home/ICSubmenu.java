package forge.gui.home;

import forge.Command;

/** 
 * Dictates methods required for a submenu controller.
 * 
 * <br><br><i>(I at beginning of class name denotes an interface.)</i>
 * <br><i>(C at beginning of class name denotes a controller class.)</i>
 */
public interface ICSubmenu {
    /** Fires when a menu is selected. Avoid any reference
     * referring to VHomeUI in this method, because
     * it is triggered when VHomeUI is initialized, which
     * will create an NPE.
     * 
     * @return {@link forge.Command} */
    Command getMenuCommand();

    /** Call this method after the view singleton has been fully realized
     * for the first time. This method should ideally only be called once. */
    void initialize();

    /** Update whatever content is in the panel. */
    void update();
}
