package forge.player;

import forge.GuiBase;
import forge.LobbyPlayer;
import forge.ai.AiProfileUtil;
import forge.ai.LobbyPlayerAi;
import forge.interfaces.IGuiBase;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.util.GuiDisplayUtil;
import forge.util.MyRandom;
import forge.util.gui.SOptionPane;

import org.apache.commons.lang3.StringUtils;

public final class GamePlayerUtil {
    private GamePlayerUtil() { };

    private static final LobbyPlayer guiPlayer = new LobbyPlayerHuman("Human", GuiBase.getInterface());
    public static final LobbyPlayer getGuiPlayer() {
        return guiPlayer;
    }
    public static final LobbyPlayer getGuiPlayer(String name, int index) {
        if (index == 0) {
            if (!name.equals(guiPlayer.getName())) {
                guiPlayer.setName(name);
                FModel.getPreferences().setPref(FPref.PLAYER_NAME, name);
                FModel.getPreferences().save();
            }
            return guiPlayer;
        }
        //use separate LobbyPlayerHuman instance for human players beyond first
        return new LobbyPlayerHuman(name, GuiBase.getInterface());
    }

    public static final LobbyPlayer getQuestPlayer() {
        return guiPlayer; //TODO: Make this a separate player
    }

    public final static LobbyPlayer createAiPlayer() {
        return createAiPlayer(GuiDisplayUtil.getRandomAiName());
    }
    public final static LobbyPlayer createAiPlayer(String name) {
        int avatarCount = GuiBase.getInterface().getAvatarCount();
        return createAiPlayer(name, avatarCount == 0 ? 0 : MyRandom.getRandom().nextInt(avatarCount));
    }
    public final static LobbyPlayer createAiPlayer(String name, int avatarIndex) {
        LobbyPlayerAi player = new LobbyPlayerAi(name);

        // TODO: implement specific AI profiles for quest mode.
        String lastProfileChosen = FModel.getPreferences().getPref(FPref.UI_CURRENT_AI_PROFILE);
        player.setRotateProfileEachGame(lastProfileChosen.equals(AiProfileUtil.AI_PROFILE_RANDOM_DUEL));
        if (lastProfileChosen.equals(AiProfileUtil.AI_PROFILE_RANDOM_MATCH)) {
            lastProfileChosen = AiProfileUtil.getRandomProfile();
            System.out.println(String.format("AI profile %s was chosen for the lobby player %s.", lastProfileChosen, player.getName()));
        }
        player.setAiProfile(lastProfileChosen);
        player.setAvatarIndex(avatarIndex);
        return player;
    }

    public static void setPlayerName(final IGuiBase gui) {
        String oldPlayerName = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
        String newPlayerName = null;

        if (StringUtils.isBlank(oldPlayerName)) {
            newPlayerName = getVerifiedPlayerName(getPlayerNameUsingFirstTimePrompt(gui), oldPlayerName);
        } else {
            newPlayerName = getVerifiedPlayerName(getPlayerNameUsingStandardPrompt(gui, oldPlayerName), oldPlayerName);
        }

        FModel.getPreferences().setPref(FPref.PLAYER_NAME, newPlayerName);
        FModel.getPreferences().save();

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
