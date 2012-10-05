package forge.game;

import java.util.List;

import forge.Card;
import forge.deck.Deck;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 * @param <Card>
 *
 */
public class PlayerStartsGame {

    private final Player player;
    private final Deck deck;
    public int initialLives = 20;
    public List<Card> cardsOnBattlefield = null;
            
    public PlayerStartsGame(Player who, Deck cards) {
        player = who;
        deck = cards;
    }

    public Player getPlayer() {
        return player;
    }

    public Deck getDeck() {
        return deck;
    }
    
    
}
