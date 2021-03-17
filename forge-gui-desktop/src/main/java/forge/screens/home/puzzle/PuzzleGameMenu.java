package forge.screens.home.puzzle;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;

import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.Localizer;

public class PuzzleGameMenu {
    private PuzzleGameMenu() { }

    private static ForgePreferences prefs = FModel.getPreferences();

    public static JMenu getMenu() {
        final Localizer localizer = Localizer.getInstance();
        JMenu menu = new JMenu(localizer.getMessage("lblPuzzle"));
        menu.setMnemonic(KeyEvent.VK_G);
        return menu;
    }
}
