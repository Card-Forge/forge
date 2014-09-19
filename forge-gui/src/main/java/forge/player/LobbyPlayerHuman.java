package forge.player;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.interfaces.IGuiBase;
import forge.util.GuiDisplayUtil;

public class LobbyPlayerHuman extends LobbyPlayer implements IGameEntitiesFactory {
    final IGuiBase gui;

    public LobbyPlayerHuman(final String name, final IGuiBase gui) {
        super(name);
        this.gui = gui;
    }

    @Override
    public PlayerController createControllerFor(Player human) {
        return new PlayerControllerHuman(human.getGame(), human, this, gui);
    }

    @Override
    public Player createIngamePlayer(Game game, final int id) {
        Player player = new Player(GuiDisplayUtil.personalizeHuman(getName()), game, id);
        player.setFirstController(new PlayerControllerHuman(game, player, this, gui));
        return player;
    }

    public void hear(LobbyPlayer player, String message) {
        gui.hear(player, message);
    }

    public IGuiBase getGui() {
        return this.gui;
    }
}