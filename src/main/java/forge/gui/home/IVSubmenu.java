package forge.gui.home;

import javax.swing.JPanel;

/**
 * Dictates methods required for a submenu view.
 * 
 * <br><br><i>(I at beginning of class name denotes an interface.)</i>
 * <br><i>(V at beginning of class name denotes a view class.)</i>
 */
public interface IVSubmenu {
    /** Allows static factory creation by decoupling UI components.
     * @return {@link javax.swing.JPanel} */
    JPanel getPanel();

    /** Retrives control object associated with this instance.
     * @return {@link forge.gui.home.ICSubmenu}
     */
    ICSubmenu getControl();

    /** Returns parent menu grouping of this submenu, useful for
     * functions such as expanding and collapsing in the menu area.
     * 
     * @return {@link javax.swing.JPanel} */
    EMenuGroup getGroupEnum();

    /** Display title string for this menu item.
     * @return {@link java.lang.String} */
    String getMenuTitle();

    /** Enum registration for this menu item, in EMenuItem.
     * @return {@link java.lang.String} */
    String getItemEnum();

    /** Removes all children and (re)populates panel components, independent of constructor.*/
    void populate();
}
