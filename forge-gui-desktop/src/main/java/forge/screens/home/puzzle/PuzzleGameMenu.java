package forge.screens.home.puzzle;

import forge.model.FModel;
import forge.properties.ForgePreferences;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class PuzzleGameMenu {
    private PuzzleGameMenu() { }

    private static ForgePreferences prefs = FModel.getPreferences();

    public static JMenu getMenu() {
        JMenu menu = new JMenu("Puzzle");
        menu.setMnemonic(KeyEvent.VK_G);
        return menu;
    }
}
