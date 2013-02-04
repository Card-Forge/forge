package forge.game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import forge.deck.Deck;
import forge.game.player.LobbyPlayer;
import forge.item.CardPrinted;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class MatchStartHelper {
    private final Map<LobbyPlayer, PlayerStartConditions> players = new HashMap<LobbyPlayer, PlayerStartConditions>();

    public void addPlayer(final LobbyPlayer player, final PlayerStartConditions c) {
        players.put(player, c);
    }

    public void addPlayer(final LobbyPlayer player, final Deck deck) {
        PlayerStartConditions start = new PlayerStartConditions(deck);
        players.put(player, start);
    }

    public void addVanguardPlayer(final LobbyPlayer player, final Deck deck, final CardPrinted avatar) {

        PlayerStartConditions start = new PlayerStartConditions(deck);

        start.setStartingLife(start.getStartingLife() + avatar.getCard().getLife());
        start.setStartingHand(start.getStartingHand() + avatar.getCard().getHand());
        start.setCardsInCommand(Arrays.asList(avatar));

        players.put(player, start);
    }

    public void addArchenemy(final LobbyPlayer player, final Deck deck, final Iterable<CardPrinted> schemes) {
        PlayerStartConditions start = new PlayerStartConditions(deck);
        start.setSchemes(schemes);
        players.put(player, start);
    }
    
    public void addPlanechasePlayer(final LobbyPlayer player, final Deck deck, final Iterable<CardPrinted> planes) {
        PlayerStartConditions start = new PlayerStartConditions(deck);
        start.setPlanes(planes);
        players.put(player, start);
    }

    public Map<LobbyPlayer, PlayerStartConditions> getPlayerMap() {

        return players;
    }

}
