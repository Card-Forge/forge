package forge.menus;

import java.util.List;

import javax.swing.JMenu;

/** 
 * By implementing this interface a class can add menus to the menu bar
 * by calling the {@code MenuBarManager.SetupMenuBar()} method.
 *
 */
public interface IMenuProvider {

    /**
     * Returns a list of JMenu objects for display in MenuBar.
     */
    List<JMenu> getMenus();

}
