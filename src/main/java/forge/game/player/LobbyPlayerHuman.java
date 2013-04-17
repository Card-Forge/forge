package forge.game.player;

import forge.game.GameState;

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
        return new HumanPlayer(this, game);
    }
}