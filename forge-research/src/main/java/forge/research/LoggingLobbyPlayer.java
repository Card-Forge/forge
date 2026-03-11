package forge.research;

import java.util.List;
import java.util.Map;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.player.GamePlayerUtil;
import forge.player.LobbyPlayerHuman;
import forge.util.GuiDisplayUtil;

/**
 * LobbyPlayer that creates LoggingPlayerControllerHuman instances.
 * Extends LobbyPlayerHuman so all GUI integration works normally.
 * Passes the gui player singleton as the controller's lobbyPlayer reference
 * so the framework recognizes this player as the primary GUI player.
 */
public class LoggingLobbyPlayer extends LobbyPlayerHuman {

    private final List<Map<String, Object>> log;
    private final int playerIndex;

    public LoggingLobbyPlayer(String name, List<Map<String, Object>> log, int playerIndex) {
        super(name);
        this.log = log;
        this.playerIndex = playerIndex;
    }

    @Override
    public Player createIngamePlayer(Game game, int id) {
        Player p = new Player(GuiDisplayUtil.personalizeHuman(getName()), game, id);
        // Pass the gui player singleton so PlayerControllerHuman.isGuiPlayer() works
        LobbyPlayer guiLp = GamePlayerUtil.getGuiPlayer();
        LoggingPlayerControllerHuman controller =
                new LoggingPlayerControllerHuman(game, p, guiLp, log, playerIndex);
        p.setFirstController(controller);
        return p;
    }

    @Override
    public PlayerController createMindSlaveController(Player master, Player slave) {
        LobbyPlayer guiLp = GamePlayerUtil.getGuiPlayer();
        return new LoggingPlayerControllerHuman(slave.getGame(), slave, guiLp, log, playerIndex);
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        // No-op
    }
}
