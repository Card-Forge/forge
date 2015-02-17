package forge.player;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.util.GuiDisplayUtil;

public class LobbyPlayerHuman extends LobbyPlayer implements IGameEntitiesFactory {
    public LobbyPlayerHuman(final String name) {
        super(name);
    }

    @Override
    public PlayerController createMindSlaveController(Player master, Player slave) {
        return new PlayerControllerHuman(slave, this, (PlayerControllerHuman)master.getController());
    }

    @Override
    public Player createIngamePlayer(final Game game, final int id) {
        final Player player = new Player(GuiDisplayUtil.personalizeHuman(getName()), game, id);
        final PlayerControllerHuman controller = new PlayerControllerHuman(game, player, this);
        player.setFirstController(controller);
        return player;
    }

    public void hear(LobbyPlayer player, String message) {
        //ostedMatch.getController().hear(player, message);
    }
}