package forge.player;

import forge.game.Game;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.model.FModel;
import forge.utils.ForgePreferences.FPref;

public class LobbyPlayerHuman extends LobbyPlayer {
    public LobbyPlayerHuman(String name) {
        super(name);
    }

    @Override
    protected PlayerType getType() {
        return PlayerType.HUMAN;
    }

    @Override
    public PlayerController createControllerFor(Player human) {
        return new PlayerControllerHuman(human.getGame(), human, this);
    }

    @Override
    public Player createIngamePlayer(Game game) { //TODO
        Player player = new Player(/*GuiDisplayUtil.personalizeHuman(*/getName()/*)*/, game);
        player.setFirstController(new PlayerControllerHuman(game, player, this));

        if (FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED) &&
                FModel.getPreferences().getPrefBoolean(FPref.DEV_UNLIMITED_LAND)) {
            player.canCheatPlayUnlimitedLands = true;
        }

        return player;
    }

    public void hear(LobbyPlayer player, String message) {
        //FNetOverlay.SINGLETON_INSTANCE.addMessage(player.getName(), message);
    }
}