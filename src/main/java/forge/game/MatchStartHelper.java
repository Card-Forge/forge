package forge.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Supplier;

import forge.Card;
import forge.card.CardRules;
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

        start.setCardsInCommand(new Supplier<Iterable<Card>>() {

            @Override
            public Iterable<Card> get() {
                List<Card> res = new ArrayList<Card>();
                res.add(avatar.toForgeCard());
                return res;
            }

        });

        players.put(player, start);
    }

    public Map<LobbyPlayer, PlayerStartConditions> getPlayerMap() {

        return players;
    }

}
