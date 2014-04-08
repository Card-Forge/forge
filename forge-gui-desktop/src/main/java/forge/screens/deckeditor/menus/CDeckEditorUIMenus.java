package forge.screens.deckeditor.menus;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gets the menus associated with the Game screen.
 *
 */
public class CDeckEditorUIMenus {

    private final boolean SHOW_ICONS = true;

    public List<JMenu> getMenus() {
        List<JMenu> menus = new ArrayList<JMenu>();
        menus.add(DeckFileMenu.getMenu(SHOW_ICONS));
        return menus;
    }
}
