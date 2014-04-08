package forge.screens.workshop.menus;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gets the menus associated with the Game screen.
 *
 */
public class CWorkshopUIMenus {

    private final boolean SHOW_ICONS = true;

    public List<JMenu> getMenus() {
        List<JMenu> menus = new ArrayList<JMenu>();
        menus.add(WorkshopFileMenu.getMenu(SHOW_ICONS));
        return menus;
    }
}
