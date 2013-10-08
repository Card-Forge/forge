package forge.gui.home.settings;

import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;

import forge.Singletons;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

public final class GamePlayerUtil {
    private GamePlayerUtil() { };

    private final static ForgePreferences prefs = Singletons.getModel().getPreferences();

    public static void setPlayerName() {

        String playerName = prefs.getPref(FPref.PLAYER_NAME);
        String newName = null;

        if (StringUtils.isBlank(playerName)) {
            newName = (String)JOptionPane.showInputDialog(
                    JOptionPane.getRootFrame(),
                    "By default, Forge will refer to you as the \"Human\" during gameplay.\n" +
                            "If you would prefer a different name please enter it now.\n",
                            "Personalize Forge Gameplay",
                            JOptionPane.QUESTION_MESSAGE,
                            null, null, null);
        } else {
            newName = getNewPlayerNameFromInputDialog(playerName);
        }

        if (newName == null || !StringUtils.isAlphanumericSpace(newName)) {
            newName = (StringUtils.isBlank(playerName) ? "Human" : playerName);
        } else if (StringUtils.isWhitespace(newName)) {
            newName = "Human";
        } else {
            newName = newName.trim();
        }

        prefs.setPref(FPref.PLAYER_NAME, newName);
        prefs.save();

        if (StringUtils.isBlank(playerName) && newName != "Human") {
            JOptionPane.showMessageDialog(
                    JOptionPane.getRootFrame(),
                    "Thank you, " + newName + ". " +
                    "You will not be prompted again but you can change\nyour name at any time using the \"Player Name\" setting in Preferences.\n\n");
        }

    }

    private static String getNewPlayerNameFromInputDialog(String playerName) {
        String newName =
                (String)JOptionPane.showInputDialog(
                        JOptionPane.getRootFrame(),
                        "Please enter a new name (alpha-numeric only)\n",
                        "Personalize Forge Gameplay",
                        JOptionPane.PLAIN_MESSAGE,
                        null, null, playerName);
        if (newName == null || !StringUtils.isAlphanumericSpace(newName)) {
            return playerName;
        } else if (StringUtils.isWhitespace(newName)) {
            return "Human";
        } else {
            return newName.trim();
        }
    }

}
