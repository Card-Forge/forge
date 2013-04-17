package forge.game.player;

import org.apache.commons.lang.NotImplementedException;

import forge.game.GameState;

public class LobbyPlayerRemote extends LobbyPlayer {
    public LobbyPlayerRemote(String name) {
        super(name);
    }

    @Override
    public PlayerType getType() {
        return PlayerType.REMOTE;
    }

    /* (non-Javadoc)
     * @see forge.game.player.LobbyPlayer#getPlayer(forge.game.GameState)
     */
    @Override
    public Player getPlayer(GameState gameState) {
        // Cannot create remote players yet 
        throw new NotImplementedException();
    }
}