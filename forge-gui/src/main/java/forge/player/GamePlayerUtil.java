package forge.player;

import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.util.gui.SOptionPane;

import org.apache.commons.lang3.StringUtils;

public final class GamePlayerUtil {
    private GamePlayerUtil() { };

    private final static ForgePreferences prefs = FModel.getPreferences();

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
        SOptionPane.showMessageDialog("Thank you, " + playerName + ". "
                + "You will not be prompted again but you can change\n"
                + "your name at any time using the \"Player Name\" setting in Preferences\n"
                + "or via the constructed match setup screen\n");
    }

    private static String getPlayerNameUsingFirstTimePrompt() {
        return SOptionPane.showInputDialog(
                "By default, Forge will refer to you as the \"Human\" during gameplay.\n" +
                        "If you would prefer a different name please enter it now.",
                        "Personalize Forge Gameplay",
                        SOptionPane.QUESTION_ICON);
    }

    private static String getPlayerNameUsingStandardPrompt(String playerName) {
        return SOptionPane.showInputDialog(
                "Please enter a new name. (alpha-numeric only)",
                "Personalize Forge Gameplay",
                null,
                playerName);
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
