package forge.player;

import forge.interfaces.IGuiBase;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.util.gui.SOptionPane;

import org.apache.commons.lang3.StringUtils;

public final class GamePlayerUtil {
    private GamePlayerUtil() { };

    private final static ForgePreferences prefs = FModel.getPreferences();

    public static void setPlayerName(final IGuiBase gui) {
        String oldPlayerName = prefs.getPref(FPref.PLAYER_NAME);
        String newPlayerName = null;

        if (StringUtils.isBlank(oldPlayerName)) {
            newPlayerName = getVerifiedPlayerName(getPlayerNameUsingFirstTimePrompt(gui), oldPlayerName);
        } else {
            newPlayerName = getVerifiedPlayerName(getPlayerNameUsingStandardPrompt(gui, oldPlayerName), oldPlayerName);
        }

        prefs.setPref(FPref.PLAYER_NAME, newPlayerName);
        prefs.save();

        if (StringUtils.isBlank(oldPlayerName) && newPlayerName != "Human") {
            showThankYouPrompt(gui, newPlayerName);
        }

    }

    private static void showThankYouPrompt(final IGuiBase gui, final String playerName) {
        SOptionPane.showMessageDialog(gui, "Thank you, " + playerName + ". "
                + "You will not be prompted again but you can change\n"
                + "your name at any time using the \"Player Name\" setting in Preferences\n"
                + "or via the constructed match setup screen\n");
    }

    private static String getPlayerNameUsingFirstTimePrompt(final IGuiBase gui) {
        return SOptionPane.showInputDialog(gui,
                "By default, Forge will refer to you as the \"Human\" during gameplay.\n" +
                        "If you would prefer a different name please enter it now.",
                        "Personalize Forge Gameplay",
                        SOptionPane.QUESTION_ICON);
    }

    private static String getPlayerNameUsingStandardPrompt(final IGuiBase gui, final String playerName) {
        return SOptionPane.showInputDialog(gui,
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
