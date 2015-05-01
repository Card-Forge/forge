package forge.player;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.util.GuiDisplayUtil;

public class LobbyPlayerHuman extends LobbyPlayer implements IGameEntitiesFactory {
    public LobbyPlayerHuman(final String name) {
        this(name, -1);
    }
    public LobbyPlayerHuman(final String name, final int avatarIndex) {
        super(name);
        setAvatarIndex(avatarIndex);
    }

    @Override
    public PlayerController createMindSlaveController(final Player master, final Player slave) {
        return new PlayerControllerHuman(slave, this, (PlayerControllerHuman)master.getController());
    }

    @Override
    public Player createIngamePlayer(final Game game, final int id) {
        final Player player = new Player(GuiDisplayUtil.personalizeHuman(getName()), game, id);
        final PlayerControllerHuman controller = new PlayerControllerHuman(game, player, this);
        player.setFirstController(controller);
        return player;
    }

    @Override
    public void hear(final LobbyPlayer player, final String message) {
        //hostedMatch.getController().hear(player, message);
    }
}