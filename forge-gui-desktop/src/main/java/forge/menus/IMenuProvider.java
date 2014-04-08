package forge.menus;

import javax.swing.*;
import java.util.List;

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
