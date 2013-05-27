package forge.game.player;

import forge.game.GameState;
import forge.gui.FNetOverlay;

public class LobbyPlayerHuman extends LobbyPlayer {
    public LobbyPlayerHuman(String name) {
        super(name);
    }

    @Override
    public PlayerType getType() {
        return PlayerType.HUMAN;
    }

    @Override
    public Player getPlayer(GameState game) {
        Player player = new Player(this, game);
        player.setController(new PlayerControllerHuman(game, player, this));
        return player;
    }

    public void hear(LobbyPlayer player, String message) {
        FNetOverlay.SINGLETON_INSTANCE.addMessage(player.getName(), message);
    }
}