package forge.gui.home;

import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;

/**
 * Dictates methods required for a submenu view.
 * 
 * <br><br><i>(I at beginning of class name denotes an interface.)</i>
 * <br><i>(V at beginning of class name denotes a view class.)</i>
 */
public interface IVSubmenu extends IVDoc {
    /** Returns parent menu grouping of this submenu, useful for
     * functions such as expanding and collapsing in the menu area.
     * 
     * @return {@link javax.swing.JPanel} */
    EMenuGroup getGroupEnum();

    /** Display title string for this menu item.
     * @return {@link java.lang.String} */
    String getMenuTitle();

    /** Enum registration for this menu item, in EMenuItem.
     * @return {@link forge.gui.framework.EDocID} */
    EDocID getItemEnum();

    /** Removes all children and (re)populates panel components, independent of constructor.*/
    void populate();
}
