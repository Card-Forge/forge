package forge.game;

import java.util.HashMap;
import java.util.Map;

import forge.deck.Deck;
import forge.game.player.LobbyPlayer;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class MatchStartHelper {
    private final Map<LobbyPlayer, PlayerStartConditions> players = new HashMap<LobbyPlayer, PlayerStartConditions>();
    
    public void addPlayer(LobbyPlayer player, PlayerStartConditions c) {
        players.put(player,c);
    }
    
    public void addPlayer(LobbyPlayer player, Deck deck) {
        players.put(player, new PlayerStartConditions(deck));
    }
    
    public Map<LobbyPlayer, PlayerStartConditions> getPlayerMap() 
    {
        return players;
    }

    
}
