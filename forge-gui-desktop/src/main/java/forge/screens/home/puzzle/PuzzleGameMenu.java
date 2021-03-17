package forge.screens.home.puzzle;

import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.Localizer;

import javax.swing.*;
import java.awt.event.KeyEvent;

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
