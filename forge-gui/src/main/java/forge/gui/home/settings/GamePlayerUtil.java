package forge.gui.home.settings;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;

import forge.Singletons;
import forge.gui.toolbox.FOptionPane;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;

public final class GamePlayerUtil {
    private GamePlayerUtil() { };

    private final static ForgePreferences prefs = Singletons.getModel().getPreferences();

    public static void setPlayerName() {

        String oldPlayerName = prefs.getPref(FPref.PLAYER_NAME);
        String newPlayerName = null;

        if (StringUtils.isBlank(oldPlayerName)) {
            newPlayerName = getVerifiedPlayerName(getPlayerNameUsingFirstTimePrompt(), oldPlayerName);
        } else {
            newPlayerName = getVerifiedPlayerName(getPlayerNameUsingStandardPrompt(oldPlayerName), oldPlayerName);
        }

        prefs.setPref(FPref.PLAYER_NAME, newPlayerName);
        prefs.save();

        if (StringUtils.isBlank(oldPlayerName) && newPlayerName != "Human") {
            showThankYouPrompt(newPlayerName);
        }

    }

    private static void showThankYouPrompt(String playerName) {
        FOptionPane.showMessageDialog("Thank you, " + playerName + ". " +
                "You will not be prompted again but you can change\nyour name at any time using the \"Player Name\" setting in Preferences.\n\n");
    }

    private static String getPlayerNameUsingFirstTimePrompt() {
        return (String)JOptionPane.showInputDialog(
                JOptionPane.getRootFrame(),
                "By default, Forge will refer to you as the \"Human\" during gameplay.\n" +
                        "If you would prefer a different name please enter it now.\n",
                        "Personalize Forge Gameplay",
                        JOptionPane.QUESTION_MESSAGE,
                        null, null, null);
    }

    private static String getPlayerNameUsingStandardPrompt(String playerName) {
        return (String)JOptionPane.showInputDialog(
                JOptionPane.getRootFrame(),
                "Please enter a new name (alpha-numeric only)\n",
                "Personalize Forge Gameplay",
                JOptionPane.PLAIN_MESSAGE,
                null, null, playerName);
    }

    private static String getVerifiedPlayerName(String newName, String oldName) {
        if (newName == null || !StringUtils.isAlphanumericSpace(newName)) {
            newName = (StringUtils.isBlank(oldName) ? "Human" : oldName);
        } else if (StringUtils.isWhitespace(newName)) {
            newName = "Human";
        } else {
            newName = newName.trim();
        }
        return newName;
    }

}
