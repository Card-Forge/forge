package forge.player;

import forge.GuiBase;
import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.util.GuiDisplayUtil;

public class LobbyPlayerHuman extends LobbyPlayer implements IGameEntitiesFactory {
    public LobbyPlayerHuman(String name) {
        super(name);
    }

    @Override
    public PlayerController createControllerFor(Player human) {
        return new PlayerControllerHuman(human.getGame(), human, this);
    }
    
    @Override
    public Player createIngamePlayer(Game game) {
        Player player = new Player(GuiDisplayUtil.personalizeHuman(getName()), game);
        player.setFirstController(new PlayerControllerHuman(game, player, this));

        if (ForgePreferences.DEV_MODE && FModel.getPreferences().getPrefBoolean(FPref.DEV_UNLIMITED_LAND)) {
            player.canCheatPlayUnlimitedLands = true;
        }
        return player;
    }

    public void hear(LobbyPlayer player, String message) {
        GuiBase.getInterface().hear(player, message);
    }
}