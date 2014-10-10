package forge.player;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.match.MatchUtil;
import forge.util.GuiDisplayUtil;

public class LobbyPlayerHuman extends LobbyPlayer implements IGameEntitiesFactory {
    public LobbyPlayerHuman(final String name) {
        super(name);
    }

    @Override
    public PlayerController createControllerFor(Player human) {
        return new PlayerControllerHuman(human.getGame(), human, this);
    }

    @Override
    public Player createIngamePlayer(Game game, final int id) {
        Player player = new Player(GuiDisplayUtil.personalizeHuman(getName()), game, id);
        PlayerControllerHuman controller = new PlayerControllerHuman(game, player, this);
        player.setFirstController(controller);
        return player;
    }

    public void hear(LobbyPlayer player, String message) {
        MatchUtil.getController().hear(player, message);
    }
}